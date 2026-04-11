package com.undersky.im.core.local

import com.undersky.im.core.api.ChatMessage

internal fun ChatMessageEntity.toApi(): ChatMessage =
    ChatMessage(
        msgId = msgId,
        msgType = msgType,
        fromUserId = fromUserId,
        toUserId = toUserId,
        groupId = groupId,
        body = body,
        createdAt = createdAt
    )

internal fun ChatMessage.toEntity(convKey: String): ChatMessageEntity =
    ChatMessageEntity(
        msgId = msgId,
        convKey = convKey,
        msgType = msgType,
        fromUserId = fromUserId,
        toUserId = toUserId,
        groupId = groupId,
        body = body,
        createdAt = createdAt
    )

object ChatConvKeys {
    fun p2p(selfId: Long, peerId: Long): String =
        "P2P_${minOf(selfId, peerId)}_${maxOf(selfId, peerId)}"

    fun group(groupId: Long): String = "GROUP_$groupId"
}
