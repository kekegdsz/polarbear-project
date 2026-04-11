package com.undersky.androidim.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.undersky.androidim.ImApp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ImApp

    private val eventsChannel = Channel<LoginEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            viewModelScope.launch { eventsChannel.send(LoginEvent.ShowError("请输入用户名和密码")) }
            return
        }
        viewModelScope.launch {
            app.authRepository.login(username, password)
                .onSuccess { data ->
                    runCatching {
                        app.sessionStore.save(data, username.trim())
                    }.onFailure { e ->
                        eventsChannel.send(LoginEvent.ShowError(e.message ?: "保存会话失败"))
                        return@launch
                    }
                    eventsChannel.send(LoginEvent.NavigateMain)
                }
                .onFailure { e ->
                    eventsChannel.send(LoginEvent.ShowError(e.message ?: "登录失败"))
                }
        }
    }

    sealed class LoginEvent {
        data object NavigateMain : LoginEvent()
        data class ShowError(val message: String) : LoginEvent()
    }
}
