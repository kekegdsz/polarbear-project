package com.undersky.business.user

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userDirectoryCacheDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "im_user_directory_cache"
)

class UserDirectoryCacheStore(context: Context) {

    private val appContext = context.applicationContext
    private val keyUserId = longPreferencesKey("directory_cache_owner_id")
    private val keyJson = stringPreferencesKey("directory_cache_json")

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, DirectoryUserDto::class.java)
    private val listAdapter = moshi.adapter<List<DirectoryUserDto>>(listType)

    fun directoryFlow(ownerUserId: Long): Flow<List<DirectoryUserDto>> =
        appContext.userDirectoryCacheDataStore.data.map { prefs ->
            val storedOwner = prefs[keyUserId] ?: return@map emptyList()
            if (storedOwner != ownerUserId) return@map emptyList()
            val raw = prefs[keyJson] ?: return@map emptyList()
            listAdapter.fromJson(raw).orEmpty()
        }

    suspend fun read(ownerUserId: Long): List<DirectoryUserDto> =
        directoryFlow(ownerUserId).first()

    suspend fun save(ownerUserId: Long, users: List<DirectoryUserDto>) {
        appContext.userDirectoryCacheDataStore.edit { prefs ->
            prefs[keyUserId] = ownerUserId
            prefs[keyJson] = listAdapter.toJson(users)
        }
    }

    suspend fun clear() {
        appContext.userDirectoryCacheDataStore.edit { it.clear() }
    }
}
