package com.undersky.androidim.feature.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.business.user.UserSession
import com.undersky.im.core.api.ChatMessage
import com.undersky.im.core.api.ImEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as BootstrapApplication).services

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _displayNames = MutableLiveData<Map<Long, String>>(emptyMap())
    val displayNames: LiveData<Map<Long, String>> = _displayNames

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private var eventsJob: Job? = null
    private var session: UserSession? = null
    private var peerUserId: Long = -1L
    private var groupId: Long = -1L
    private var titleFallback: String = ""
    private val prefetchedUserIds = mutableSetOf<Long>()

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
        _title.value = titleFallbackArg
        _messages.postValue(emptyList())

        val selfLabel = session.nickname?.takeIf { it.isNotBlank() }
            ?: session.username?.takeIf { it.isNotBlank() }
            ?: "我"
        _displayNames.value = mapOf(session.userId to selfLabel)
        prefetchedUserIds.add(session.userId)

        val isP2P = peerUserIdArg != -1L
        if (isP2P) {
            services.imClient.requestHistoryP2P(peerUserIdArg)
            services.imClient.requestUserInfo(peerUserIdArg)
            prefetchedUserIds.add(peerUserIdArg)
        } else if (groupIdArg != -1L) {
            services.imClient.requestHistoryGroup(groupIdArg)
        }

        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            services.imClient.events.collect { ev ->
                when (ev) {
                    is ImEvent.HistoryResult -> {
                        val filtered = ev.messages.filter { m ->
                            if (isP2P) {
                                val p = peerUserIdArg
                                m.msgType == "P2P" && (
                                    (m.fromUserId == p && m.toUserId == session.userId) ||
                                        (m.fromUserId == session.userId && m.toUserId == p)
                                    )
                            } else {
                                m.msgType == "GROUP" && m.groupId == groupIdArg
                            }
                        }
                        _messages.postValue(filtered)
                        prefetchSenderProfiles(filtered)
                    }
                    is ImEvent.PrivateMessage -> {
                        if (!isP2P) return@collect
                        val m = ev.message
                        val peer = peerUserIdArg
                        val involved = m.fromUserId == peer || m.toUserId == peer
                        val me = session.userId
                        if (involved && (m.fromUserId == me || m.toUserId == me)) {
                            val cur = _messages.value.orEmpty()
                            if (cur.none { it.msgId == m.msgId }) {
                                val next = cur + m
                                _messages.postValue(next)
                                prefetchSenderProfiles(next)
                            }
                        }
                    }
                    is ImEvent.GroupMessage -> {
                        if (isP2P) return@collect
                        val m = ev.message
                        if (m.groupId == groupIdArg) {
                            val cur = _messages.value.orEmpty()
                            if (cur.none { it.msgId == m.msgId }) {
                                val next = cur + m
                                _messages.postValue(next)
                                prefetchSenderProfiles(next)
                            }
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
