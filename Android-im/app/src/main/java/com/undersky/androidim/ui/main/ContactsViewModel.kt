package com.undersky.androidim.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.undersky.androidim.ImApp
import com.undersky.androidim.data.AuthTokenHolder
import com.undersky.androidim.data.DirectoryUserDto
import com.undersky.androidim.data.UserSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ImApp

    private val _users = MutableLiveData<List<DirectoryUserDto>>(emptyList())
    val users: LiveData<List<DirectoryUserDto>> = _users

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var cacheJob: Job? = null
    private var fetchJob: Job? = null
    private var refreshTick = 0

    fun start(session: UserSession) {
        stop()
        refreshTick = 0
        cacheJob = viewModelScope.launch {
            AuthTokenHolder.set(session.token)
            app.userDirectoryCacheStore.directoryFlow(session.userId).collect { raw ->
                _users.postValue(sortedDirectoryUsers(raw, session.userId))
            }
        }
        scheduleFetch(session, initial = true)
    }

    fun refresh(session: UserSession) {
        refreshTick++
        fetchJob?.cancel()
        scheduleFetch(session, initial = false)
    }

    private fun scheduleFetch(session: UserSession, initial: Boolean) {
        fetchJob = viewModelScope.launch {
            AuthTokenHolder.set(session.token)
            if (initial && refreshTick == 0) {
                if (app.userDirectoryCacheStore.read(session.userId).isNotEmpty()) {
                    return@launch
                }
            }
            _loading.postValue(true)
            _error.postValue(null)
            try {
                app.userDirectoryRepository.listAll()
                    .onSuccess { list ->
                        app.userDirectoryCacheStore.save(session.userId, list)
                        _error.postValue(null)
                    }
                    .onFailure { e ->
                        val cachedEmpty = app.userDirectoryCacheStore.read(session.userId).isEmpty()
                        if (cachedEmpty) {
                            _error.postValue(e.message ?: "加载失败")
                        } else {
                            _error.postValue(null)
                        }
                    }
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun stop() {
        cacheJob?.cancel()
        cacheJob = null
        fetchJob?.cancel()
        fetchJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}

private fun sortedDirectoryUsers(raw: List<DirectoryUserDto>, selfId: Long): List<DirectoryUserDto> =
    raw
        .filter { it.id != selfId }
        .sortedWith(
            compareBy<DirectoryUserDto> { (it.username ?: "").isBlank() }
                .thenBy { it.username?.lowercase() ?: "" }
                .thenBy { it.id }
        )
