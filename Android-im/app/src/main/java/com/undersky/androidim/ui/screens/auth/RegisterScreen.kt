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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.undersky.androidim.ui.theme.WxNav
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    app: ImApp,
    onBack: () -> Unit,
    onRegistered: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("注册") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WxNav)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = mobile,
                onValueChange = { mobile = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("手机号（可选）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                        app.authRepository.register(username, password, mobile.ifBlank { null })
                            .onSuccess { data ->
                                runCatching {
                                    app.sessionStore.save(data, username.trim())
                                }.onFailure { e ->
                                    error = e.message
                                }
                                onRegistered()
                            }
                            .onFailure { e ->
                                error = e.message ?: "注册失败"
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
                    Text("注册并登录")
                }
            }
        }
    }
}
