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
    val username: String?
)

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "im_session")

class SessionStore(private val context: Context) {

    private val keyUserId = longPreferencesKey("user_id")
    private val keyToken = stringPreferencesKey("token")
    private val keyUsername = stringPreferencesKey("username")

    val sessionFlow: Flow<UserSession?> = context.sessionDataStore.data.map { prefs ->
        val id = prefs[keyUserId] ?: return@map null
        UserSession(
            userId = id,
            token = prefs[keyToken],
            username = prefs[keyUsername]
        )
    }

    suspend fun save(data: LoginDataDto, username: String) {
        val uid = data.userId?.toLongOrNull()
            ?: throw IllegalArgumentException("无效的 userId")
        context.sessionDataStore.edit { prefs ->
            prefs[keyUserId] = uid
            prefs[keyToken] = data.token.orEmpty()
            prefs[keyUsername] = username
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
