package com.undersky.business.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequestDto(
    val username: String,
    val password: String,
    val mobile: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginEnvelope(
    val code: Int,
    val message: String,
    val data: LoginDataDto?
)

@JsonClass(generateAdapter = true)
data class LoginDataDto(
    val userId: String?,
    val token: String?,
    val nickname: String? = null,
    val mobile: String?,
    @Json(name = "is_vip") val isVip: Boolean = false,
    val role: String?
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequestDto(
    val nickname: String
)

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginEnvelope

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): LoginEnvelope

    @PUT("user/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequestDto): LoginEnvelope
}
