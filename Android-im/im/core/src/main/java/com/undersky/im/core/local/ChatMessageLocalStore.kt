package com.undersky.im.core.local

import android.content.Context
import com.undersky.im.core.api.ChatMessage
import java.io.File

/** 本地持久化聊天消息（Room），按会话 convKey 分表查询。 */
class ChatMessageLocalStore(context: Context) {

    private val dao = ImDatabase.getInstance(context).chatMessageDao()

    suspend fun upsert(convKey: String, messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        val merged = messages.map { incoming ->
            val prev = dao.findByConvAndMsgId(convKey, incoming.msgId)
            val reuse = prev?.localMediaPath?.takeIf { p ->
                File(p).exists() && incoming.localMediaPath == null
            }
            incoming.copy(localMediaPath = incoming.localMediaPath ?: reuse)
        }
        dao.upsertAll(merged.map { it.toEntity(convKey) })
    }

    suspend fun loadLatestPage(convKey: String, limit: Int): List<ChatMessage> =
        dao.loadLatestAscending(convKey, limit).map { it.toApi() }

    suspend fun loadOlderPage(convKey: String, beforeMsgId: Long, limit: Int): List<ChatMessage> =
        dao.loadOlderAscending(convKey, beforeMsgId, limit).map { it.toApi() }

    /** 该会话在本地是否已有消息（用于跳过重复的首次历史拉取） */
    suspend fun hasLocalMessages(convKey: String): Boolean =
        dao.countForConversation(convKey) > 0

    /** 本地该会话已持久化的最大服务端 msgId，用于增量同步 */
    suspend fun maxMsgId(convKey: String): Long? = dao.maxMsgId(convKey)
}
