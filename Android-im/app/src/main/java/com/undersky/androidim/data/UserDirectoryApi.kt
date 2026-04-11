package com.undersky.androidim.data

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class DirectoryUserDto(
    val id: Long,
    val username: String? = null,
    val mobile: String? = null
)

@JsonClass(generateAdapter = true)
data class DirectoryListEnvelope(
    val code: Int,
    val message: String,
    val data: List<DirectoryUserDto>?
)

interface UserDirectoryApi {
    /** Token 由 OkHttp 拦截器统一附加 `X-Auth-Token`，避免 @Header 空串导致未授权 */
    @GET("im/directory/users")
    suspend fun listUsers(
        @Query("limit") limit: Int = 2000
    ): DirectoryListEnvelope
}
