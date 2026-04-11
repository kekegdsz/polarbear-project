package com.undersky.androidim.data

import java.io.IOException

class AuthRepository(private val api: AuthApi) {

    suspend fun login(username: String, password: String): Result<LoginDataDto> {
        return try {
            val env = api.login(LoginRequestDto(username.trim(), password))
            if (env.code == 0 && env.data != null) {
                Result.success(env.data)
            } else {
                Result.failure(IllegalStateException(env.message.ifBlank { "登录失败" }))
            }
        } catch (e: IOException) {
            Result.failure(IOException("网络连接失败，请检查网络与服务器地址、端口是否可达", e))
        }
    }

    suspend fun register(username: String, password: String, mobile: String?): Result<LoginDataDto> {
        return try {
            val env = api.register(
                RegisterRequestDto(
                    username = username.trim(),
                    password = password,
                    mobile = mobile?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
            if (env.code == 0 && env.data != null) {
                Result.success(env.data)
            } else {
                Result.failure(IllegalStateException(env.message.ifBlank { "注册失败" }))
            }
        } catch (e: IOException) {
            Result.failure(IOException("网络连接失败，请检查网络与服务器地址、端口是否可达", e))
        }
    }
}
