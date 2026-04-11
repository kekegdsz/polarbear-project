package com.undersky.im.core.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "conversation_list",
    primaryKeys = ["ownerUserId", "convKey"],
    indices = [Index(value = ["ownerUserId", "lastMsgId"])]
)
data class ConversationListEntity(
    val ownerUserId: Long,
    val convKey: String,
    val convType: String,
    val peerUserId: Long?,
    val groupId: Long?,
    val groupName: String?,
    val lastMsgId: Long,
    val lastMsgType: String,
    val lastFromUserId: Long,
    val lastToUserId: Long?,
    val lastGroupId: Long?,
    val lastBody: String,
    val lastCreatedAt: String?
)
