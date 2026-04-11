package com.undersky.im.core.api

data class ChatMessage(
    val msgId: Long,
    val msgType: String,
    val fromUserId: Long,
    val toUserId: Long?,
    val groupId: Long?,
    val body: String,
    val createdAt: String?
)

data class ConversationItem(
    val convType: String,
    val peerUserId: Long?,
    val groupId: Long?,
    val lastMessage: ChatMessage,
    val unreadCount: Int = 0,
    val groupName: String? = null
)

/** 群成员角色：OWNER / ADMIN / MEMBER */
data class GroupMemberRow(val userId: Long, val role: String)
