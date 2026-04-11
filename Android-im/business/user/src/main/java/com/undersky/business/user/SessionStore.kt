package com.undersky.business.user

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class UserSession(
    val userId: Long,
    val token: String?,
    val username: String?,
    val nickname: String?
)

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "im_session")

class SessionStore(private val context: Context) {

    private val keyUserId = longPreferencesKey("user_id")
    private val keyToken = stringPreferencesKey("token")
    private val keyUsername = stringPreferencesKey("username")
    private val keyNickname = stringPreferencesKey("nickname")

    val sessionFlow: Flow<UserSession?> = context.sessionDataStore.data.map { prefs ->
        val id = prefs[keyUserId] ?: return@map null
        UserSession(
            userId = id,
            token = prefs[keyToken],
            username = prefs[keyUsername],
            nickname = prefs[keyNickname]
        )
    }

    suspend fun save(data: LoginDataDto, loginUsername: String) {
        val uid = data.userId?.toLongOrNull()
            ?: throw IllegalArgumentException("无效的 userId")
        val nick = data.nickname?.takeIf { it.isNotBlank() } ?: loginUsername
        context.sessionDataStore.edit { prefs ->
            prefs[keyUserId] = uid
            prefs[keyToken] = data.token.orEmpty()
            prefs[keyUsername] = loginUsername
            prefs[keyNickname] = nick
        }
    }

    suspend fun applyProfileUpdate(data: LoginDataDto) {
        context.sessionDataStore.edit { prefs ->
            data.token?.takeIf { it.isNotBlank() }?.let { prefs[keyToken] = it }
            data.nickname?.takeIf { it.isNotBlank() }?.let { prefs[keyNickname] = it }
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
