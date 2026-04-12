package com.undersky.androidim.feature.chat

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.androidim.feature.chat.media.copyUriToCacheFile
import com.undersky.androidim.feature.chat.media.decodeImageSize
import com.undersky.androidim.feature.chat.media.extractVideoDurationMs
import com.undersky.androidim.feature.chat.media.queryDisplayName
import com.undersky.androidim.feature.chat.media.uploadImFile
import com.undersky.business.user.UserSession
import com.undersky.im.core.CHAT_PAGE_SIZE
import com.undersky.im.core.CHAT_SYNC_NEWER_LIMIT
import com.undersky.im.core.api.ChatMessage
import com.undersky.im.core.api.GroupMemberRow
import com.undersky.im.core.api.ImEvent
import com.undersky.im.core.api.mediaUrlFromImJson
import com.undersky.im.core.api.resolveImAttachmentUrl
import com.undersky.im.core.api.unwrapImMessageBody
import com.undersky.im.core.local.ChatConvKeys
import com.undersky.im.core.local.ChatMessageLocalStore
import com.undersky.im.core.local.UserProfileLocalStore
import com.undersky.im.core.local.isStale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class GroupDetailUi(
    val groupId: Long,
    val name: String,
    val ownerUserId: Long,
    val members: List<GroupMemberRow>,
    val myRole: String
)

private sealed class PendingHistory {
    object None : PendingHistory()
    object InitialFromServer : PendingHistory()
    object NewerSinceLocal : PendingHistory()
    data class Older(val beforeMsgId: Long) : PendingHistory()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as BootstrapApplication).services
    private val localStore = ChatMessageLocalStore(application)
    private val profileStore: UserProfileLocalStore get() = services.userProfileLocalStore

    private val _messagesState = MutableLiveData(
        ChatMessagesState(emptyList(), ChatScroll.NoScroll)
    )
    val messagesState: LiveData<ChatMessagesState> = _messagesState

    private val _displayNames = MutableLiveData<Map<Long, String>>(emptyMap())
    val displayNames: LiveData<Map<Long, String>> = _displayNames

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private val _userOnline = MutableLiveData<Map<Long, Boolean>>(emptyMap())
    val userOnline: LiveData<Map<Long, Boolean>> = _userOnline

    private val _groupDetail = MutableLiveData<GroupDetailUi?>(null)
    val groupDetail: LiveData<GroupDetailUi?> = _groupDetail

    /** 当前正在播放的语音 URL（用于气泡高亮；同一条再点即停止） */
    private val _playingVoiceUrl = MutableLiveData<String?>(null)
    val playingVoiceUrl: LiveData<String?> = _playingVoiceUrl

    private var eventsJob: Job? = null
    private var session: UserSession? = null
    private var peerUserId: Long = -1L
    private var groupId: Long = -1L
    private var titleFallback: String = ""
    private var convKey: String = ""
    private val prefetchedUserIds = mutableSetOf<Long>()

    private var pendingHistory: PendingHistory = PendingHistory.None

    @Volatile
    private var loadingOlder: Boolean = false

    @Volatile
    private var noMoreOlderHistory: Boolean = false

    private var voicePlayer: MediaPlayer? = null

    /** 刚发出的富媒体：服务端回显 URL → 本机上传前缓存文件路径，用于气泡优先读本地。 */
    private val pendingSelfMediaLocalByRemoteKey = LinkedHashMap<String, String>()

    private fun registerPendingSelfMedia(remoteUrlOrPath: String, file: File) {
        val s = remoteUrlOrPath.trim()
        if (s.isBlank() || !file.exists()) return
        val keys = buildSet {
            add(s)
            add(resolveImAttachmentUrl(s, services.apiBaseUrl))
        }
        synchronized(pendingSelfMediaLocalByRemoteKey) {
            for (k in keys) {
                if (k.isNotBlank()) pendingSelfMediaLocalByRemoteKey[k] = file.absolutePath
            }
            while (pendingSelfMediaLocalByRemoteKey.size > 48) {
                pendingSelfMediaLocalByRemoteKey.remove(pendingSelfMediaLocalByRemoteKey.keys.first())
            }
        }
    }

    private fun takePendingSelfMedia(remoteUrlFromBody: String): String? {
        val s0 = remoteUrlFromBody.trim()
        if (s0.isEmpty()) return null
        val s1 = resolveImAttachmentUrl(s0, services.apiBaseUrl)
        return synchronized(pendingSelfMediaLocalByRemoteKey) {
            pendingSelfMediaLocalByRemoteKey.remove(s0) ?: pendingSelfMediaLocalByRemoteKey.remove(s1)
        }
    }

    private fun attachLocalPathIfSelfSent(m: ChatMessage): ChatMessage {
        val me = session?.userId ?: return m
        if (m.fromUserId != me) return m
        val remote = try {
            mediaUrlFromImJson(JSONObject(unwrapImMessageBody(m.body)))
        } catch (_: Exception) {
            return m
        }
        if (remote.isBlank()) return m
        val local = takePendingSelfMedia(remote) ?: m.localMediaPath ?: return m
        return m.copy(localMediaPath = local)
    }

    fun bind(
        session: UserSession,
        peerUserIdArg: Long,
        groupIdArg: Long,
        titleFallbackArg: String
    ) {
        stopVoicePlayback()
        this.session = session
        this.peerUserId = peerUserIdArg
        this.groupId = groupIdArg
        this.titleFallback = titleFallbackArg
        prefetchedUserIds.clear()
        pendingHistory = PendingHistory.None
        loadingOlder = false
        noMoreOlderHistory = false

        convKey = if (peerUserIdArg != -1L) {
            ChatConvKeys.p2p(session.userId, peerUserIdArg)
        } else {
            ChatConvKeys.group(groupIdArg)
        }

        synchronized(pendingSelfMediaLocalByRemoteKey) { pendingSelfMediaLocalByRemoteKey.clear() }

        _title.value = titleFallbackArg
        _userOnline.value = emptyMap()
        _groupDetail.postValue(null)
        postMessages(emptyList(), ChatScroll.ToBottom)

        val selfLabel = session.nickname?.takeIf { it.isNotBlank() }
            ?: session.username?.takeIf { it.isNotBlank() }
            ?: "我"
        _displayNames.value = mapOf(session.userId to selfLabel)
        prefetchedUserIds.add(session.userId)

        val isP2P = peerUserIdArg != -1L

        viewModelScope.launch(Dispatchers.IO) {
            profileStore.upsert(session.userId, session.username, session.nickname, null)
            if (isP2P) {
                val peerRow = profileStore.getProfile(peerUserIdArg)
                withContext(Dispatchers.Main) {
                    if (peerRow != null) {
                        mergeDisplayName(peerUserIdArg, peerRow.nickname, peerRow.username)
                        val t = peerRow.nickname?.takeIf { it.isNotBlank() }
                            ?: peerRow.username?.takeIf { it.isNotBlank() }
                            ?: titleFallbackArg
                        _title.postValue(t)
                    }
                }
                if (peerRow == null || peerRow.isStale()) {
                    services.imClient.requestUserInfo(peerUserIdArg)
                } else {
                    synchronized(prefetchedUserIds) { prefetchedUserIds.add(peerUserIdArg) }
                }
            }
            if (!isP2P && groupIdArg > 0) {
                services.imClient.requestGroupInfo(groupIdArg)
            }
        }

        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            services.imClient.events.collect { ev ->
                when (ev) {
                    is ImEvent.HistoryResult -> handleHistory(ev, isP2P, peerUserIdArg, groupIdArg, session.userId)
                    is ImEvent.PrivateMessage -> {
                        if (!isP2P) return@collect
                        val m = ev.message
                        val peer = peerUserIdArg
                        val me = session.userId
                        val involved = m.fromUserId == peer || m.toUserId == peer
                        if (involved && (m.fromUserId == me || m.toUserId == me)) {
                            ingestLiveMessage(m)
                        }
                    }
                    is ImEvent.GroupMessage -> {
                        if (isP2P) return@collect
                        val m = ev.message
                        if (m.groupId == groupIdArg) {
                            ingestLiveMessage(m)
                        }
                    }
                    is ImEvent.UserInfoResult -> {
                        viewModelScope.launch(Dispatchers.IO) {
                            profileStore.upsert(ev.userId, ev.username, ev.nickname, ev.mobile)
                        }
                        mergeDisplayName(ev.userId, ev.nickname, ev.username)
                        ev.online?.let { mergeOnline(ev.userId, it) }
                        if (isP2P && ev.userId == peerUserIdArg) {
                            val t = ev.nickname?.takeIf { it.isNotBlank() }
                                ?: ev.username?.takeIf { it.isNotBlank() }
                                ?: titleFallbackArg
                            _title.postValue(t)
                        }
                    }
                    is ImEvent.Presence -> mergeOnline(ev.userId, ev.online)
                    is ImEvent.GroupInfoResult -> {
                        if (!isP2P && ev.groupId == groupIdArg) {
                            applyGroupInfo(ev, session.userId)
                        }
                    }
                    else -> Unit
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val tail = localStore.loadLatestPage(convKey, CHAT_PAGE_SIZE)
            val hasLocal = localStore.hasLocalMessages(convKey)
            val localMaxMsgId = if (hasLocal) localStore.maxMsgId(convKey) else null
            withContext(Dispatchers.Main) {
                if (tail.isNotEmpty()) {
                    postMessages(tail, ChatScroll.ToBottom)
                    prefetchSenderProfiles(tail)
                }
            }
            withContext(Dispatchers.Main) {
                when {
                    !hasLocal -> {
                        pendingHistory = PendingHistory.InitialFromServer
                        if (isP2P) {
                            services.imClient.requestHistoryP2P(
                                peerUserIdArg,
                                beforeId = null,
                                afterId = null,
                                limit = CHAT_PAGE_SIZE
                            )
                        } else if (groupIdArg != -1L) {
                            services.imClient.requestHistoryGroup(
                                groupIdArg,
                                beforeId = null,
                                afterId = null,
                                limit = CHAT_PAGE_SIZE
                            )
                        }
                    }
                    localMaxMsgId != null -> {
                        // 轻量增量：对比本地最大 msgId，拉取服务端更新的消息，避免离线/漏推送丢消息
                        pendingHistory = PendingHistory.NewerSinceLocal
                        if (isP2P) {
                            services.imClient.requestHistoryP2P(
                                peerUserIdArg,
                                beforeId = null,
                                afterId = localMaxMsgId,
                                limit = CHAT_SYNC_NEWER_LIMIT
                            )
                        } else if (groupIdArg != -1L) {
                            services.imClient.requestHistoryGroup(
                                groupIdArg,
                                beforeId = null,
                                afterId = localMaxMsgId,
                                limit = CHAT_SYNC_NEWER_LIMIT
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun handleHistory(
        ev: ImEvent.HistoryResult,
        isP2P: Boolean,
        peerUserIdArg: Long,
        groupIdArg: Long,
        selfId: Long
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val filtered = ev.messages.filter { m ->
                if (isP2P) {
                    val p = peerUserIdArg
                    m.msgType == "P2P" && (
                        (m.fromUserId == p && m.toUserId == selfId) ||
                            (m.fromUserId == selfId && m.toUserId == p)
                        )
                } else {
                    m.msgType == "GROUP" && m.groupId == groupIdArg
                }
            }.map { attachLocalPathIfSelfSent(it) }
            val kind = pendingHistory
            pendingHistory = PendingHistory.None

            withContext(Dispatchers.IO) {
                localStore.upsert(convKey, filtered)
            }

            when (kind) {
                PendingHistory.None -> Unit
                PendingHistory.NewerSinceLocal -> {
                    withContext(Dispatchers.Main) {
                        if (filtered.isNotEmpty()) {
                            val cur = currentMessages()
                            val combined = (cur + filtered).distinctBy { it.msgId }.sortedBy { it.msgId }
                            postMessages(combined, ChatScroll.ToBottom)
                            prefetchSenderProfiles(filtered)
                        }
                    }
                }
                PendingHistory.InitialFromServer -> {
                    val page = withContext(Dispatchers.IO) {
                        localStore.loadLatestPage(convKey, CHAT_PAGE_SIZE)
                    }
                    withContext(Dispatchers.Main) {
                        postMessages(page, ChatScroll.ToBottom)
                        prefetchSenderProfiles(page)
                    }
                }
                is PendingHistory.Older -> {
                    when {
                        filtered.isEmpty() -> {
                            noMoreOlderHistory = true
                            loadingOlder = false
                        }
                        else -> {
                            if (filtered.size < CHAT_PAGE_SIZE) {
                                noMoreOlderHistory = true
                            }
                            withContext(Dispatchers.Main) {
                                val cur = currentMessages()
                                val older = filtered.sortedBy { it.msgId }
                                val combined = (older + cur).distinctBy { it.msgId }.sortedBy { it.msgId }
                                postMessages(combined, ChatScroll.KeepScroll)
                                prefetchSenderProfiles(older)
                                loadingOlder = false
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ingestLiveMessage(m: ChatMessage) {
        val enriched = attachLocalPathIfSelfSent(m)
        viewModelScope.launch(Dispatchers.IO) {
            localStore.upsert(convKey, listOf(enriched))
            withContext(Dispatchers.Main) {
                val cur = currentMessages()
                if (cur.none { it.msgId == enriched.msgId }) {
                    val next = (cur + enriched).sortedBy { it.msgId }
                    postMessages(next, ChatScroll.ToBottom)
                    prefetchSenderProfiles(next)
                }
            }
        }
    }

    fun loadOlderMessages() {
        if (loadingOlder || noMoreOlderHistory) return
        val cur = currentMessages()
        if (cur.isEmpty()) return
        val oldestId = cur.first().msgId
        loadingOlder = true
        viewModelScope.launch(Dispatchers.IO) {
            val localOlder = localStore.loadOlderPage(convKey, oldestId, CHAT_PAGE_SIZE)
            if (localOlder.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    val combined = (localOlder + cur).distinctBy { it.msgId }.sortedBy { it.msgId }
                    postMessages(combined, ChatScroll.KeepScroll)
                    loadingOlder = false
                }
                return@launch
            }
            pendingHistory = PendingHistory.Older(oldestId)
            withContext(Dispatchers.Main) {
                if (peerUserId != -1L) {
                    services.imClient.requestHistoryP2P(
                        peerUserId,
                        beforeId = oldestId,
                        afterId = null,
                        limit = CHAT_PAGE_SIZE
                    )
                } else if (groupId != -1L) {
                    services.imClient.requestHistoryGroup(
                        groupId,
                        beforeId = oldestId,
                        afterId = null,
                        limit = CHAT_PAGE_SIZE
                    )
                } else {
                    loadingOlder = false
                }
            }
        }
    }

    private fun currentMessages(): List<ChatMessage> =
        _messagesState.value?.messages.orEmpty()

    private fun postMessages(list: List<ChatMessage>, scroll: ChatScroll) {
        _messagesState.value = ChatMessagesState(list, scroll)
    }

    private fun mergeDisplayName(userId: Long, nickname: String?, username: String?) {
        val label = nickname?.takeIf { it.isNotBlank() }
            ?: username?.takeIf { it.isNotBlank() }
            ?: "用户 $userId"
        val cur = _displayNames.value.orEmpty()
        if (cur[userId] == label) return
        _displayNames.postValue(cur + (userId to label))
    }

    private fun mergeOnline(userId: Long, online: Boolean) {
        val cur = _userOnline.value.orEmpty()
        if (cur[userId] == online) return
        _userOnline.postValue(cur + (userId to online))
    }

    private fun prefetchSenderProfiles(messages: List<ChatMessage>) {
        val me = session?.userId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            for (uid in messages.map { it.fromUserId }.distinct()) {
                if (uid == me) continue
                val row = profileStore.getProfile(uid)
                if (row != null && !row.isStale()) {
                    withContext(Dispatchers.Main) {
                        mergeDisplayName(uid, row.nickname, row.username)
                    }
                    continue
                }
                val shouldRequest = synchronized(prefetchedUserIds) {
                    if (uid in prefetchedUserIds) false
                    else {
                        prefetchedUserIds.add(uid)
                        true
                    }
                }
                if (shouldRequest) {
                    services.imClient.requestUserInfo(uid)
                }
            }
        }
    }

    fun send(text: String) {
        session ?: return
        val t = text.trim()
        if (t.isEmpty()) return
        if (peerUserId != -1L) {
            services.imClient.sendPrivate(peerUserId, t)
        } else if (groupId != -1L) {
            services.imClient.sendGroup(groupId, t)
        }
    }

    private fun sendRichJson(
        json: String,
        localMediaFile: File? = null,
        extrasRegisterUrls: List<String> = emptyList()
    ) {
        session ?: return
        val t = json.trim()
        if (t.isEmpty()) return
        if (localMediaFile != null) {
            try {
                val remote = mediaUrlFromImJson(JSONObject(unwrapImMessageBody(t)))
                if (remote.isNotBlank()) registerPendingSelfMedia(remote, localMediaFile)
            } catch (_: Exception) {
            }
            for (e in extrasRegisterUrls) {
                if (e.isNotBlank()) registerPendingSelfMedia(e, localMediaFile)
            }
        }
        if (peerUserId != -1L) {
            services.imClient.sendPrivate(peerUserId, t)
        } else if (groupId != -1L) {
            services.imClient.sendGroup(groupId, t)
        }
    }

    private fun toast(msg: String) {
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopVoicePlayback() {
        try {
            voicePlayer?.release()
        } catch (_: Exception) {
        }
        voicePlayer = null
        _playingVoiceUrl.postValue(null)
    }

    /**
     * 点击播放语音；若正在播放同一条则停止（再点即停）。
     */
    fun playVoiceFromUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch(Dispatchers.Main) {
            if (_playingVoiceUrl.value == url && voicePlayer != null) {
                stopVoicePlayback()
                return@launch
            }
            stopVoicePlayback()
            launch(Dispatchers.IO) {
                try {
                    val p = MediaPlayer()
                    p.setDataSource(url)
                    p.prepare()
                    withContext(Dispatchers.Main) {
                        voicePlayer = p
                        _playingVoiceUrl.value = url
                        p.setOnCompletionListener {
                            stopVoicePlayback()
                        }
                        p.setOnErrorListener { mp, _, _ ->
                            try {
                                mp.release()
                            } catch (_: Exception) {
                            }
                            if (voicePlayer === mp) {
                                voicePlayer = null
                                _playingVoiceUrl.postValue(null)
                            }
                            toast("无法播放语音")
                            true
                        }
                        p.start()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        toast(e.message ?: "播放失败")
                        stopVoicePlayback()
                    }
                }
            }
        }
    }

    fun uploadImageFromUri(uri: Uri) {
        session ?: return
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val name = "up_${System.currentTimeMillis()}.jpg"
                val f = copyUriToCacheFile(ctx, uri, name)
                val dim = decodeImageSize(ctx, uri)
                val up = uploadImFile(services.httpClient, services.apiBaseUrl, f)
                val mime = up.contentType ?: "image/jpeg"
                val json = buildImageJson(up.url, mime, dim?.first, dim?.second)
                withContext(Dispatchers.Main) {
                    sendRichJson(json, localMediaFile = f, extrasRegisterUrls = listOf(up.path))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast(e.message ?: "发送图片失败") }
            }
        }
    }

    fun uploadVideoFromUri(uri: Uri) {
        session ?: return
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ext = when (ctx.contentResolver.getType(uri)) {
                    "video/quicktime" -> "mov"
                    else -> "mp4"
                }
                val name = "vid_${System.currentTimeMillis()}.$ext"
                val f = copyUriToCacheFile(ctx, uri, name)
                val dur = extractVideoDurationMs(f)
                val up = uploadImFile(services.httpClient, services.apiBaseUrl, f)
                val mime = up.contentType ?: "video/mp4"
                val json = buildVideoJson(up.url, mime, dur)
                withContext(Dispatchers.Main) {
                    sendRichJson(json, localMediaFile = f, extrasRegisterUrls = listOf(up.path))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast(e.message ?: "发送视频失败") }
            }
        }
    }

    fun uploadFileFromUri(uri: Uri) {
        session ?: return
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val disp = queryDisplayName(ctx, uri)
                val safe = disp.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(80)
                val name = "${System.currentTimeMillis()}_$safe"
                val f = copyUriToCacheFile(ctx, uri, name)
                val up = uploadImFile(services.httpClient, services.apiBaseUrl, f, filename = safe)
                val mime = up.contentType ?: "application/octet-stream"
                val json = buildFileJson(up.url, disp, mime, up.size)
                withContext(Dispatchers.Main) {
                    sendRichJson(json, localMediaFile = f, extrasRegisterUrls = listOf(up.path))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast(e.message ?: "发送文件失败") }
            }
        }
    }

    fun uploadVoiceFile(file: File, durationMs: Long) {
        session ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val up = uploadImFile(services.httpClient, services.apiBaseUrl, file, filename = file.name)
                val json = buildVoiceJson(up.url, durationMs.coerceAtLeast(0L))
                withContext(Dispatchers.Main) {
                    sendRichJson(json, localMediaFile = file, extrasRegisterUrls = listOf(up.path))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast(e.message ?: "发送语音失败") }
            }
        }
    }

    fun requestGroupInfoRefresh() {
        if (groupId > 0) {
            services.imClient.requestGroupInfo(groupId)
        }
    }

    fun renameGroupTo(newName: String) {
        if (groupId <= 0) return
        val t = newName.trim()
        if (t.isEmpty()) return
        services.imClient.renameGroup(groupId, t)
    }

    fun setGroupAdminFor(targetUserId: Long) {
        if (groupId <= 0) return
        services.imClient.setGroupAdmin(groupId, targetUserId)
    }

    fun removeGroupAdminFor(targetUserId: Long) {
        if (groupId <= 0) return
        services.imClient.removeGroupAdmin(groupId, targetUserId)
    }

    private fun applyGroupInfo(ev: ImEvent.GroupInfoResult, selfId: Long) {
        _title.postValue(ev.name)
        val myR = ev.members.find { it.userId == selfId }?.role ?: "MEMBER"
        _groupDetail.postValue(
            GroupDetailUi(ev.groupId, ev.name, ev.ownerUserId, ev.members, myR)
        )
        prefetchMemberProfiles(ev.members, selfId)
    }

    private fun prefetchMemberProfiles(members: List<GroupMemberRow>, selfId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            for (uid in members.map { it.userId }.distinct()) {
                if (uid == selfId) continue
                val row = profileStore.getProfile(uid)
                if (row != null && !row.isStale()) {
                    withContext(Dispatchers.Main) {
                        mergeDisplayName(uid, row.nickname, row.username)
                    }
                    continue
                }
                val shouldRequest = synchronized(prefetchedUserIds) {
                    if (uid in prefetchedUserIds) false
                    else {
                        prefetchedUserIds.add(uid)
                        true
                    }
                }
                if (shouldRequest) {
                    services.imClient.requestUserInfo(uid)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventsJob?.cancel()
        stopVoicePlayback()
    }
}
