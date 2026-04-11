package com.undersky.androidim.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.undersky.androidim.ImApp
import com.undersky.androidim.data.ConversationItem
import com.undersky.androidim.data.ImSocketManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainTabsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ImApp

    private val _conversations = MutableLiveData<List<ConversationItem>>(emptyList())
    val conversations: LiveData<List<ConversationItem>> = _conversations

    private var eventsJob: Job? = null

    init {
        startCollecting()
    }

    private fun startCollecting() {
        if (eventsJob != null) return
        eventsJob = viewModelScope.launch {
            app.imSocket.events.collect { ev ->
                when (ev) {
                    is ImSocketManager.Event.AuthOk -> app.imSocket.requestConversations()
                    is ImSocketManager.Event.Conversations -> _conversations.postValue(ev.items)
                    is ImSocketManager.Event.PrivateMessage,
                    is ImSocketManager.Event.GroupMessage -> app.imSocket.requestConversations()
                    else -> Unit
                }
            }
        }
    }

    fun refreshConversations() {
        app.imSocket.requestConversations()
    }
}
