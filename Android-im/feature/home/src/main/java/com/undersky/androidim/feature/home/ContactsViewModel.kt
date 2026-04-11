package com.undersky.androidim.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.business.user.AuthTokenHolder
import com.undersky.business.user.DirectoryUserDto
import com.undersky.business.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as BootstrapApplication).services

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
            services.userDirectoryCacheStore.directoryFlow(session.userId).collect { raw ->
                withContext(Dispatchers.IO) {
                    for (u in raw) {
                        services.userProfileLocalStore.upsert(u.id, u.username, u.nickname, u.mobile)
                    }
                }
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
                if (services.userDirectoryCacheStore.read(session.userId).isNotEmpty()) {
                    return@launch
                }
            }
            _loading.postValue(true)
            _error.postValue(null)
            try {
                services.userDirectoryRepository.listAll()
                    .onSuccess { list ->
                        services.userDirectoryCacheStore.save(session.userId, list)
                        _error.postValue(null)
                    }
                    .onFailure { e ->
                        val cachedEmpty = services.userDirectoryCacheStore.read(session.userId).isEmpty()
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

private fun DirectoryUserDto.sortKey(): String =
    nickname?.takeIf { it.isNotBlank() } ?: username?.takeIf { it.isNotBlank() } ?: ""

private fun sortedDirectoryUsers(raw: List<DirectoryUserDto>, selfId: Long): List<DirectoryUserDto> =
    raw
        .filter { it.id != selfId }
        .sortedWith(
            compareBy<DirectoryUserDto> { it.sortKey().isBlank() }
                .thenBy { it.sortKey().lowercase() }
                .thenBy { it.id }
        )
