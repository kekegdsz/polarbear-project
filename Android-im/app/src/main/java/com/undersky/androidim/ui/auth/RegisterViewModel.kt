package com.undersky.androidim.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.undersky.androidim.ImApp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ImApp

    private val eventsChannel = Channel<RegisterEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    fun register(username: String, password: String, mobile: String) {
        if (username.isBlank() || password.isBlank()) {
            viewModelScope.launch { eventsChannel.send(RegisterEvent.ShowError("请输入用户名和密码")) }
            return
        }
        viewModelScope.launch {
            app.authRepository.register(username, password, mobile.ifBlank { null })
                .onSuccess { data ->
                    runCatching {
                        app.sessionStore.save(data, username.trim())
                    }.onFailure { e ->
                        eventsChannel.send(RegisterEvent.ShowError(e.message ?: "保存会话失败"))
                        return@launch
                    }
                    eventsChannel.send(RegisterEvent.NavigateMain)
                }
                .onFailure { e ->
                    eventsChannel.send(RegisterEvent.ShowError(e.message ?: "注册失败"))
                }
        }
    }

    sealed class RegisterEvent {
        data object NavigateMain : RegisterEvent()
        data class ShowError(val message: String) : RegisterEvent()
    }
}
