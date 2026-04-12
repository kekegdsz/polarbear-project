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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlin.jvm.Volatile

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
        val merged = serverList.map { item ->
            val u = when (item.convType) {
                "P2P" -> unreadStore.getP2p(item.peerUserId ?: 0L)
                "GROUP" -> unreadStore.getGroup(item.groupId ?: 0L)
                else -> 0
            }
            item.copy(unreadCount = u)
        }
        _conversations.postValue(merged)
        _totalUnread.postValue(merged.sumOf { it.unreadCount })
    }

    private fun refreshDisplayedCounts() {
        publishMerged(_conversations.value.orEmpty())
    }

    fun refreshConversations() {
        services.imClient.requestConversations()
    }
}
