package com.undersky.androidim.data

class AuthRepository(private val api: AuthApi) {

    suspend fun login(username: String, password: String): Result<LoginDataDto> {
        val env = api.login(LoginRequestDto(username.trim(), password))
        return if (env.code == 0 && env.data != null) {
            Result.success(env.data)
        } else {
            Result.failure(IllegalStateException(env.message.ifBlank { "登录失败" }))
        }
    }

    suspend fun register(username: String, password: String, mobile: String?): Result<LoginDataDto> {
        val env = api.register(
            RegisterRequestDto(
                username = username.trim(),
                password = password,
                mobile = mobile?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
        return if (env.code == 0 && env.data != null) {
            Result.success(env.data)
        } else {
            Result.failure(IllegalStateException(env.message.ifBlank { "注册失败" }))
        }
    }
}
