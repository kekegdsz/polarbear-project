package com.undersky.androidim.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@JsonClass(generateAdapter = true)
data class ContactEntry(
    val userId: Long,
    val remark: String? = null
)

private val Context.contactDataStore: DataStore<Preferences> by preferencesDataStore(name = "im_contacts")

class ContactStore(context: Context) {

    private val appContext = context.applicationContext
    private val keyJson = stringPreferencesKey("contacts_json")

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, ContactEntry::class.java)
    private val listAdapter = moshi.adapter<List<ContactEntry>>(listType)

    val contactsFlow: Flow<List<ContactEntry>> = appContext.contactDataStore.data.map { prefs ->
        val raw = prefs[keyJson] ?: return@map emptyList()
        listAdapter.fromJson(raw).orEmpty()
    }

    suspend fun add(entry: ContactEntry) {
        appContext.contactDataStore.edit { prefs ->
            val current = listAdapter.fromJson(prefs[keyJson] ?: "[]").orEmpty()
                .filter { it.userId != entry.userId } + entry
            prefs[keyJson] = listAdapter.toJson(current.sortedBy { it.userId })
        }
    }

    suspend fun remove(userId: Long) {
        appContext.contactDataStore.edit { prefs ->
            val current = listAdapter.fromJson(prefs[keyJson] ?: "[]").orEmpty()
                .filter { it.userId != userId }
            prefs[keyJson] = listAdapter.toJson(current)
        }
    }
}
