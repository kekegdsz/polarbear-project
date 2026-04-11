package com.undersky.im.core.local

import android.content.Context

/** 用户昵称/账号等摘要的本地持久化，减少重复 IM USER_INFO 请求 */
class UserProfileLocalStore(context: Context) {

    private val dao = ImDatabase.getInstance(context).userProfileDao()

    suspend fun getProfile(userId: Long): UserProfileEntity? = dao.getById(userId)

    suspend fun upsert(
        userId: Long,
        username: String?,
        nickname: String?,
        mobile: String?,
        updatedAtMillis: Long = System.currentTimeMillis()
    ) {
        dao.upsert(
            UserProfileEntity(
                userId = userId,
                username = username,
                nickname = nickname,
                mobile = mobile,
                updatedAtMillis = updatedAtMillis
            )
        )
    }

    companion object {
        /** 超过该时间认为可后台刷新（仍先用本地展示） */
        const val STALE_AFTER_MS: Long = 7L * 24 * 60 * 60 * 1000
    }
}

fun UserProfileEntity.isStale(maxAgeMs: Long = UserProfileLocalStore.STALE_AFTER_MS): Boolean =
    System.currentTimeMillis() - updatedAtMillis > maxAgeMs
