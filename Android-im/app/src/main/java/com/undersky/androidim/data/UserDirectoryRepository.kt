package com.undersky.androidim.data

import java.io.IOException
import retrofit2.HttpException

class UserDirectoryRepository(private val api: UserDirectoryApi) {

    suspend fun listAll(limit: Int = 2000): Result<List<DirectoryUserDto>> {
        if (AuthTokenHolder.get().isNullOrBlank()) {
            return Result.failure(IllegalStateException("未登录或缺少 token，请重新登录"))
        }
        return try {
            val env = api.listUsers(limit)
            if (env.code == 0 && env.data != null) {
                Result.success(env.data)
            } else {
                Result.failure(IllegalStateException(env.message.ifBlank { "加载失败" }))
            }
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.use { it.string() }?.take(300) ?: ""
            val hint = when (e.code()) {
                401 -> "未授权，请重新登录"
                else -> "HTTP ${e.code()}"
            }
            Result.failure(IllegalStateException("$hint ${body.takeIf { it.isNotBlank() } ?: ""}".trim()))
        } catch (e: IOException) {
            Result.failure(IOException("无法连接服务器，请检查网络", e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.message ?: "解析失败", e))
        }
    }
}
