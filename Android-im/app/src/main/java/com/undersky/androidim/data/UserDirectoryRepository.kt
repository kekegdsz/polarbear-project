package com.undersky.androidim.data

import java.io.IOException

class UserDirectoryRepository(private val api: UserDirectoryApi) {

    suspend fun listAll(token: String, limit: Int = 2000): Result<List<DirectoryUserDto>> {
        if (token.isBlank()) {
            return Result.failure(IllegalStateException("未登录"))
        }
        return try {
            val env = api.listUsers(token, limit)
            if (env.code == 0 && env.data != null) {
                Result.success(env.data)
            } else {
                Result.failure(IllegalStateException(env.message.ifBlank { "加载失败" }))
            }
        } catch (e: IOException) {
            Result.failure(IOException("无法连接服务器，请检查网络", e))
        }
    }
}
