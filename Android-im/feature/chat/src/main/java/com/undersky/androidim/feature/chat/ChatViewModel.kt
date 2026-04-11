package com.undersky.androidim.feature.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.business.user.UserSession
import com.undersky.im.core.CHAT_PAGE_SIZE
import com.undersky.im.core.CHAT_SYNC_NEWER_LIMIT
import com.undersky.im.core.api.ChatMessage
import com.undersky.im.core.api.ImEvent
import com.undersky.im.core.local.ChatConvKeys
import com.undersky.im.core.local.ChatMessageLocalStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed class PendingHistory {
    object None : PendingHistory()
    object InitialFromServer : PendingHistory()
    object NewerSinceLocal : PendingHistory()
    data class Older(val beforeMsgId: Long) : PendingHistory()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as BootstrapApplication).services
    private val localStore = ChatMessageLocalStore(application)

    private val _messagesState = MutableLiveData(
        ChatMessagesState(emptyList(), ChatScroll.NoScroll)
    )
    val messagesState: LiveData<ChatMessagesState> = _messagesState

    private val _displayNames = MutableLiveData<Map<Long, String>>(emptyMap())
    val displayNames: LiveData<Map<Long, String>> = _displayNames

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

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

    fun bind(
        session: UserSession,
        peerUserIdArg: Long,
        groupIdArg: Long,
        titleFallbackArg: String
    ) {
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

        _title.value = titleFallbackArg
        postMessages(emptyList(), ChatScroll.ToBottom)

        val selfLabel = session.nickname?.takeIf { it.isNotBlank() }
            ?: session.username?.takeIf { it.isNotBlank() }
            ?: "我"
        _displayNames.value = mapOf(session.userId to selfLabel)
        prefetchedUserIds.add(session.userId)

        val isP2P = peerUserIdArg != -1L
        if (isP2P) {
            prefetchedUserIds.add(peerUserIdArg)
            services.imClient.requestUserInfo(peerUserIdArg)
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
                        mergeDisplayName(ev.userId, ev.nickname, ev.username)
                        if (isP2P && ev.userId == peerUserIdArg) {
                            val t = ev.nickname?.takeIf { it.isNotBlank() }
                                ?: ev.username?.takeIf { it.isNotBlank() }
                                ?: titleFallbackArg
                            _title.postValue(t)
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
            }
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
        viewModelScope.launch(Dispatchers.IO) {
            localStore.upsert(convKey, listOf(m))
            withContext(Dispatchers.Main) {
                val cur = currentMessages()
                if (cur.none { it.msgId == m.msgId }) {
                    val next = (cur + m).sortedBy { it.msgId }
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

    private fun prefetchSenderProfiles(messages: List<ChatMessage>) {
        val me = session?.userId ?: return
        for (uid in messages.map { it.fromUserId }.distinct()) {
            if (uid == me) continue
            if (prefetchedUserIds.add(uid)) {
                services.imClient.requestUserInfo(uid)
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

    override fun onCleared() {
        super.onCleared()
        eventsJob?.cancel()
    }
}
