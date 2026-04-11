package com.undersky.im.core.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConversationListDao {

    @Query("DELETE FROM conversation_list WHERE ownerUserId = :ownerUserId")
    suspend fun deleteByOwner(ownerUserId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<ConversationListEntity>)

    @Query("SELECT * FROM conversation_list WHERE ownerUserId = :ownerUserId ORDER BY lastMsgId DESC")
    suspend fun loadForUser(ownerUserId: Long): List<ConversationListEntity>
}
