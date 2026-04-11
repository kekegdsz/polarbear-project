package com.undersky.androidim.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.undersky.androidim.ImApp
import com.undersky.business.user.UserSession
import com.undersky.im.core.api.ChatMessage
import com.undersky.im.core.api.ImEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ImApp

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private var eventsJob: Job? = null
    private var session: UserSession? = null
    private var peerUserId: Long = -1L
    private var groupId: Long = -1L
    private var titleFallback: String = ""

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
        _title.value = titleFallbackArg
        _messages.postValue(emptyList())

        val isP2P = peerUserIdArg != -1L
        if (isP2P) {
            app.imClient.requestHistoryP2P(peerUserIdArg)
            app.imClient.requestUserInfo(peerUserIdArg)
        } else if (groupIdArg != -1L) {
            app.imClient.requestHistoryGroup(groupIdArg)
        }

        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            app.imClient.events.collect { ev ->
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
                                _messages.postValue(cur + m)
                            }
                        }
                    }
                    is ImEvent.GroupMessage -> {
                        if (isP2P) return@collect
                        val m = ev.message
                        if (m.groupId == groupIdArg) {
                            val cur = _messages.value.orEmpty()
                            if (cur.none { it.msgId == m.msgId }) {
                                _messages.postValue(cur + m)
                            }
                        }
                    }
                    is ImEvent.UserInfoResult -> {
                        if (isP2P && ev.userId == peerUserIdArg) {
                            val t = ev.username?.takeIf { it.isNotBlank() } ?: titleFallbackArg
                            _title.postValue(t)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun send(text: String) {
        session ?: return
        val t = text.trim()
        if (t.isEmpty()) return
        if (peerUserId != -1L) {
            app.imClient.sendPrivate(peerUserId, t)
        } else if (groupId != -1L) {
            app.imClient.sendGroup(groupId, t)
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventsJob?.cancel()
    }
}
