package com.undersky.business.user

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
    @GET("im/directory/users")
    suspend fun listUsers(
        @Query("limit") limit: Int = 2000
    ): DirectoryListEnvelope
}
