package com.undersky.im.core.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatMessageEntity::class, UserProfileEntity::class, ConversationListEntity::class],
    version = 4,
    exportSchema = false
)
abstract class ImDatabase : RoomDatabase() {

    abstract fun chatMessageDao(): ChatMessageDao

    abstract fun userProfileDao(): UserProfileDao

    abstract fun conversationListDao(): ConversationListDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS user_profiles (" +
                        "userId INTEGER NOT NULL PRIMARY KEY, " +
                        "username TEXT, " +
                        "nickname TEXT, " +
                        "mobile TEXT, " +
                        "updatedAtMillis INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS conversation_list (" +
                        "ownerUserId INTEGER NOT NULL, " +
                        "convKey TEXT NOT NULL, " +
                        "convType TEXT NOT NULL, " +
                        "peerUserId INTEGER, " +
                        "groupId INTEGER, " +
                        "groupName TEXT, " +
                        "lastMsgId INTEGER NOT NULL, " +
                        "lastMsgType TEXT NOT NULL, " +
                        "lastFromUserId INTEGER NOT NULL, " +
                        "lastToUserId INTEGER, " +
                        "lastGroupId INTEGER, " +
                        "lastBody TEXT NOT NULL, " +
                        "lastCreatedAt TEXT, " +
                        "PRIMARY KEY(ownerUserId, convKey))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_conversation_list_owner_last " +
                        "ON conversation_list(ownerUserId, lastMsgId)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN localMediaPath TEXT")
            }
        }

        @Volatile
        private var instance: ImDatabase? = null

        fun getInstance(context: Context): ImDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ImDatabase::class.java,
                    "undersky_im.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
