package com.undersky.androidim.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.undersky.androidim.ImApp
import com.undersky.androidim.ui.theme.WxGreen
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    app: ImApp,
    onRegister: () -> Unit,
    onLoggedIn: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "登录",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("用户名") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WxGreen,
                focusedLabelColor = WxGreen,
                cursorColor = WxGreen
            )
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("密码") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WxGreen,
                focusedLabelColor = WxGreen,
                cursorColor = WxGreen
            )
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    error = "请输入用户名和密码"
                    return@Button
                }
                loading = true
                error = null
                scope.launch {
                    app.authRepository.login(username, password)
                        .onSuccess { data ->
                            runCatching {
                                app.sessionStore.save(data, username.trim())
                            }.onFailure { e ->
                                error = e.message
                            }
                            onLoggedIn()
                        }
                        .onFailure { e ->
                            error = e.message ?: "登录失败"
                        }
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = WxGreen)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("登录")
            }
        }
        TextButton(onClick = onRegister, modifier = Modifier.padding(top = 8.dp)) {
            Text("注册新账号", color = WxGreen)
        }
    }
}
