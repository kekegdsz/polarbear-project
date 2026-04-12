package com.undersky.androidim.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.androidim.feature.home.R
import com.undersky.androidim.notify.ImMessageNotifier
import com.undersky.im.core.api.ChatMessage
import com.undersky.im.core.api.ConversationItem
import com.undersky.im.core.api.ImEvent
import com.undersky.im.core.api.chatMessagePreviewLabel
import com.undersky.im.core.local.ChatConvKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlin.jvm.Volatile

/** 会话列表「管理隐藏」弹窗用一行数据。 */
data class HiddenConversationRow(
    val convKey: String,
    val title: String,
)

class MainTabsViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as BootstrapApplication).services
    private val unreadStore = services.unreadCountStore

    private val _conversations = MutableLiveData<List<ConversationItem>>(emptyList())
    val conversations: LiveData<List<ConversationItem>> = _conversations

    private val _totalUnread = MutableLiveData(0)
    val totalUnread: LiveData<Int> = _totalUnread

    /** IM 实时在线（含不在通讯录缓存中的会话对方） */
    private val _userPresence = MutableLiveData<Map<Long, Boolean>>(emptyMap())
    val userPresence: LiveData<Map<Long, Boolean>> = _userPresence

    @Volatile
    private var selfUserId: Long? = null

    @Volatile
    private var openP2PPeer: Long? = null

    @Volatile
    private var openGroupId: Long? = null

    /** 本次登录周期内是否已收到过服务端 CONVERSATIONS（避免慢磁盘读覆盖新列表） */
    @Volatile
    private var conversationsHydratedFromServer: Boolean = false

    /** 最近一次完整会话列表（服务端或本地缓存），用于在未重新拉取前正确合并未读 / 隐藏 / 置顶 */
    @Volatile
    private var lastServerConversationSource: List<ConversationItem> = emptyList()

    private var eventsJob: Job? = null
    private var sessionJob: Job? = null

    init {
        sessionJob = viewModelScope.launch {
            services.sessionStore.sessionFlow
                .map { it?.userId }
                .distinctUntilChanged()
                .collect { uid ->
                    selfUserId = uid
                    _userPresence.postValue(emptyMap())
                    if (uid == null) {
                        unreadStore.clearAll()
                        openP2PPeer = null
                        openGroupId = null
                        conversationsHydratedFromServer = false
                        lastServerConversationSource = emptyList()
                        publishMerged(emptyList())
                    } else {
                        conversationsHydratedFromServer = false
                        viewModelScope.launch(Dispatchers.IO) {
                            val cached = services.conversationLocalStore.load(uid)
                            if (!conversationsHydratedFromServer) {
                                publishMerged(cached)
                            }
                        }
                    }
                }
        }
        startCollecting()
    }

    fun setOpenChat(peerUserId: Long, groupId: Long) {
        openP2PPeer = peerUserId.takeIf { it > 0 }
        openGroupId = groupId.takeIf { it > 0 }
    }

    fun clearOpenChat() {
        openP2PPeer = null
        openGroupId = null
    }

    fun clearUnreadP2P(peerUserId: Long) {
        if (peerUserId <= 0) return
        unreadStore.clearP2p(peerUserId)
        refreshDisplayedCounts()
    }

    fun clearUnreadGroup(groupId: Long) {
        if (groupId <= 0) return
        unreadStore.clearGroup(groupId)
        refreshDisplayedCounts()
    }

    private fun startCollecting() {
        if (eventsJob != null) return
        eventsJob = viewModelScope.launch {
            services.imClient.events.collect { ev ->
                when (ev) {
                    is ImEvent.AuthOk -> {
                        _userPresence.postValue(emptyMap())
                        services.imClient.requestConversations()
                    }
                    is ImEvent.Conversations -> persistAndPublishConversations(ev.items)
                    is ImEvent.PrivateMessage -> {
                        maybeIncrementPrivate(ev.message)
                        maybeNotifyIncomingPrivate(ev.message)
                        services.imClient.requestConversations()
                    }
                    is ImEvent.GroupMessage -> {
                        maybeIncrementGroup(ev.message)
                        maybeNotifyIncomingGroup(ev.message)
                        services.imClient.requestConversations()
                    }
                    is ImEvent.Presence -> {
                        val me = selfUserId ?: return@collect
                        val cur = _userPresence.value.orEmpty()
                        _userPresence.postValue(cur + (ev.userId to ev.online))
                        viewModelScope.launch(Dispatchers.IO) {
                            services.userDirectoryCacheStore.patchUserOnline(me, ev.userId, ev.online)
                        }
                    }
                    is ImEvent.GroupCreated -> services.imClient.requestConversations()
                    else -> Unit
                }
            }
        }
    }

    private fun maybeIncrementPrivate(m: ChatMessage) {
        val me = selfUserId ?: return
        if (m.fromUserId == me) return
        val to = m.toUserId ?: return
        if (to != me) return
        val peer = m.fromUserId
        if (openP2PPeer != null && openP2PPeer == peer) return
        unreadStore.incrementP2p(peer)
    }

    private fun maybeIncrementGroup(m: ChatMessage) {
        val me = selfUserId ?: return
        if (m.fromUserId == me) return
        val gid = m.groupId ?: return
        if (openGroupId != null && openGroupId == gid) return
        unreadStore.incrementGroup(gid)
    }

    private fun appInForeground(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun maybeNotifyIncomingPrivate(m: ChatMessage) {
        val me = selfUserId ?: return
        if (m.fromUserId == me) return
        val to = m.toUserId ?: return
        if (to != me) return
        if (m.body.isBlank()) return
        val peer = m.fromUserId
        if (openP2PPeer != null && openP2PPeer == peer) return
        if (appInForeground()) return
        val ctx = getApplication<Application>().applicationContext
        val title = ctx.getString(R.string.notification_title_p2p, peer)
        ImMessageNotifier.showIncomingMessage(ctx, title, chatMessagePreviewLabel(m.body), peer, -1L)
    }

    private fun maybeNotifyIncomingGroup(m: ChatMessage) {
        val me = selfUserId ?: return
        if (m.fromUserId == me) return
        val gid = m.groupId ?: return
        if (m.body.isBlank()) return
        if (openGroupId != null && openGroupId == gid) return
        if (appInForeground()) return
        val ctx = getApplication<Application>().applicationContext
        val title = ctx.getString(R.string.notification_title_group, gid)
        ImMessageNotifier.showIncomingMessage(ctx, title, chatMessagePreviewLabel(m.body), -1L, gid)
    }

    private fun persistAndPublishConversations(serverList: List<ConversationItem>) {
        conversationsHydratedFromServer = true
        val uid = selfUserId
        if (uid != null) {
            viewModelScope.launch(Dispatchers.IO) {
                services.conversationLocalStore.replaceFromServer(uid, serverList)
            }
        }
        publishMerged(serverList)
    }

    private fun publishMerged(serverList: List<ConversationItem>) {
        if (serverList.isNotEmpty()) {
            lastServerConversationSource = serverList.toList()
        } else if (selfUserId == null) {
            lastServerConversationSource = emptyList()
        }
        applyConversationTransforms()
    }

    private fun applyConversationTransforms() {
        val uid = selfUserId
        val source = lastServerConversationSource
        val merged = source.map { item ->
            val u = when (item.convType) {
                "P2P" -> unreadStore.getP2p(item.peerUserId ?: 0L)
                "GROUP" -> unreadStore.getGroup(item.groupId ?: 0L)
                else -> 0
            }
            item.copy(unreadCount = u)
        }
        val hidden = if (uid != null) services.hiddenConversationStore.getHidden(uid) else emptySet()
        val visible = if (uid != null && hidden.isNotEmpty()) {
            merged.filter {
                val k = conversationConvKey(uid, it)
                k == null || k !in hidden
            }
        } else {
            merged
        }
        val sorted = if (uid != null) sortConversationsByPins(uid, visible) else visible
        _conversations.postValue(sorted)
        _totalUnread.postValue(sorted.sumOf { it.unreadCount })
    }

    private fun conversationConvKey(ownerUserId: Long, item: ConversationItem): String? = when (item.convType) {
        "P2P" -> item.peerUserId?.let { p -> ChatConvKeys.p2p(ownerUserId, p) }
        "GROUP" -> item.groupId?.let { g -> ChatConvKeys.group(g) }
        else -> null
    }

    private fun sortConversationsByPins(ownerUserId: Long, items: List<ConversationItem>): List<ConversationItem> {
        val pinOrder = services.pinnedConversationStore.getOrderedPins(ownerUserId)
        if (pinOrder.isEmpty()) return items
        val pinnedItems = pinOrder.mapNotNull { pk ->
            items.find { conversationConvKey(ownerUserId, it) == pk }
        }
        val pinnedSet = pinnedItems.mapNotNull { conversationConvKey(ownerUserId, it) }.toSet()
        val rest = items.filter {
            val k = conversationConvKey(ownerUserId, it)
            k == null || k !in pinnedSet
        }
        return pinnedItems + rest
    }

    fun togglePinForConversation(convKey: String) {
        val uid = selfUserId ?: return
        services.pinnedConversationStore.togglePin(uid, convKey)
        refreshDisplayedCounts()
    }

    fun hideConversationLocally(convKey: String) {
        val uid = selfUserId ?: return
        services.hiddenConversationStore.hide(uid, convKey)
        refreshDisplayedCounts()
    }

    fun unhideConversationLocally(convKey: String) {
        val uid = selfUserId ?: return
        services.hiddenConversationStore.unhide(uid, convKey)
        refreshDisplayedCounts()
    }

    /** 清除所有会话未读角标（本机），并取消系统通知。 */
    fun markAllConversationsRead() {
        unreadStore.clearAll()
        ImMessageNotifier.cancelAllForApp(getApplication())
        refreshDisplayedCounts()
    }

    private fun refreshDisplayedCounts() {
        applyConversationTransforms()
    }

    fun refreshConversations() {
        services.imClient.requestConversations()
    }

    /** 当前账号下已隐藏会话，标题尽量来自最近一次服务端会话快照。 */
    fun listHiddenConversationsForManage(): List<HiddenConversationRow> {
        val uid = selfUserId ?: return emptyList()
        val hidden = services.hiddenConversationStore.getHidden(uid)
        if (hidden.isEmpty()) return emptyList()
        val source = lastServerConversationSource
        return hidden.map { h ->
            val item = source.find { conversationConvKey(uid, it) == h }
            HiddenConversationRow(h, hiddenConversationTitle(uid, item, h))
        }.sortedBy { it.title }
    }

    private fun hiddenConversationTitle(selfId: Long, item: ConversationItem?, fallback: String): String {
        if (item == null) return fallback
        return when (item.convType) {
            "P2P" -> {
                val peer = item.peerUserId ?: 0L
                if (peer == selfId) "与自己" else "单聊 · 用户 $peer"
            }
            "GROUP" -> item.groupName?.takeIf { it.isNotBlank() } ?: "群聊 ${item.groupId ?: ""}"
            else -> fallback
        }
    }
}
