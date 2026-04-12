package com.undersky.im.core.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["convKey", "msgId"])]
)
data class ChatMessageEntity(
    @PrimaryKey val msgId: Long,
    val convKey: String,
    val msgType: String,
    val fromUserId: Long,
    val toUserId: Long?,
    val groupId: Long?,
    val body: String,
    val createdAt: String?,
    val localMediaPath: String? = null
)
