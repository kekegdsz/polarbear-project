package com.undersky.im.core.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ChatMessageEntity>)

    @Query(
        """
        SELECT * FROM (
            SELECT * FROM chat_messages WHERE convKey = :convKey ORDER BY msgId DESC LIMIT :limit
        ) ORDER BY msgId ASC
        """
    )
    suspend fun loadLatestAscending(convKey: String, limit: Int): List<ChatMessageEntity>

    @Query(
        """
        SELECT * FROM (
            SELECT * FROM chat_messages
            WHERE convKey = :convKey AND msgId < :beforeMsgId
            ORDER BY msgId DESC LIMIT :limit
        ) ORDER BY msgId ASC
        """
    )
    suspend fun loadOlderAscending(convKey: String, beforeMsgId: Long, limit: Int): List<ChatMessageEntity>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE convKey = :convKey")
    suspend fun countForConversation(convKey: String): Int

    @Query("SELECT MAX(msgId) FROM chat_messages WHERE convKey = :convKey")
    suspend fun maxMsgId(convKey: String): Long?
}
