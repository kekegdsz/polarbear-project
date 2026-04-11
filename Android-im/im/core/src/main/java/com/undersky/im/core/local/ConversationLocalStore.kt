package com.undersky.im.core.local

import android.content.Context
import androidx.room.withTransaction
import com.undersky.im.core.api.ChatMessage
import com.undersky.im.core.api.ConversationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 最近会话列表持久化（按当前登录用户分库存储）。未读数仍由 [UnreadCountStore] 管理。 */
class ConversationLocalStore(context: Context) {

    private val db = ImDatabase.getInstance(context)

    suspend fun replaceFromServer(ownerUserId: Long, items: List<ConversationItem>) {
        withContext(Dispatchers.IO) {
            val rows = items.map { it.toEntity(ownerUserId) }
            db.withTransaction {
                val dao = db.conversationListDao()
                dao.deleteByOwner(ownerUserId)
                if (rows.isNotEmpty()) {
                    dao.insertAll(rows)
                }
            }
        }
    }

    suspend fun load(ownerUserId: Long): List<ConversationItem> =
        withContext(Dispatchers.IO) {
            db.conversationListDao().loadForUser(ownerUserId).map { it.toConversationItem() }
        }
}

private fun ConversationItem.toEntity(ownerUserId: Long): ConversationListEntity {
    val key = when (convType) {
        "P2P" -> {
            val p = peerUserId ?: error("P2P conversation missing peerUserId")
            ChatConvKeys.p2p(ownerUserId, p)
        }
        "GROUP" -> {
            val g = groupId ?: error("GROUP conversation missing groupId")
            ChatConvKeys.group(g)
        }
        else -> "OTHER_${lastMessage.msgId}"
    }
    val m = lastMessage
    return ConversationListEntity(
        ownerUserId = ownerUserId,
        convKey = key,
        convType = convType,
        peerUserId = peerUserId,
        groupId = groupId,
        groupName = groupName,
        lastMsgId = m.msgId,
        lastMsgType = m.msgType,
        lastFromUserId = m.fromUserId,
        lastToUserId = m.toUserId,
        lastGroupId = m.groupId,
        lastBody = m.body,
        lastCreatedAt = m.createdAt
    )
}

private fun ConversationListEntity.toConversationItem(): ConversationItem {
    val msg = ChatMessage(
        msgId = lastMsgId,
        msgType = lastMsgType,
        fromUserId = lastFromUserId,
        toUserId = lastToUserId,
        groupId = lastGroupId,
        body = lastBody,
        createdAt = lastCreatedAt
    )
    return ConversationItem(
        convType = convType,
        peerUserId = peerUserId,
        groupId = groupId,
        lastMessage = msg,
        unreadCount = 0,
        groupName = groupName
    )
}
