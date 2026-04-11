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
    data class UserInfoResult(
        val userId: Long,
        val username: String?,
        val nickname: String?,
        val mobile: String?,
        val online: Boolean? = null
    ) : ImEvent()

    /** 用户上下线（服务端广播） */
    data class Presence(val userId: Long, val online: Boolean) : ImEvent()

    data class GroupCreated(val groupId: Long, val name: String) : ImEvent()

    data class GroupInfoResult(
        val groupId: Long,
        val name: String,
        val ownerUserId: Long,
        val members: List<GroupMemberRow>
    ) : ImEvent()
}
