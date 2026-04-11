package com.undersky.im.core.api

sealed class ImEvent {
    data object SocketOpen : ImEvent()
    data object SocketClosed : ImEvent()
    data class AuthOk(val userId: Long) : ImEvent()
    data class Error(val message: String) : ImEvent()
    data class Conversations(val items: List<ConversationItem>) : ImEvent()
    data class PrivateMessage(val message: ChatMessage) : ImEvent()
    data class GroupMessage(val message: ChatMessage) : ImEvent()
    data class HistoryResult(val messages: List<ChatMessage>) : ImEvent()
    data class UserInfoResult(val userId: Long, val username: String?, val mobile: String?) : ImEvent()
}
