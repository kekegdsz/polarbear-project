package com.undersky.androidim.data

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
    val lastMessage: ChatMessage
)
