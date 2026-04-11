package com.undersky.im.core.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatMessageEntity::class, UserProfileEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ImDatabase : RoomDatabase() {

    abstract fun chatMessageDao(): ChatMessageDao

    abstract fun userProfileDao(): UserProfileDao

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

        @Volatile
        private var instance: ImDatabase? = null

        fun getInstance(context: Context): ImDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ImDatabase::class.java,
                    "undersky_im.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
