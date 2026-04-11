package com.undersky.im.core.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: Long,
    val username: String?,
    val nickname: String?,
    val mobile: String?,
    /** 写入/更新时间，用于判断是否需要后台刷新 */
    val updatedAtMillis: Long
)
