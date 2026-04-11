package com.undersky.androidim.ui.screens.main.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.undersky.androidim.ImApp
import com.undersky.androidim.data.AuthTokenHolder
import com.undersky.androidim.data.DirectoryUserDto
import com.undersky.androidim.data.UserSession
import com.undersky.androidim.ui.theme.WxGreen
import com.undersky.androidim.ui.theme.WxLine
import com.undersky.androidim.ui.theme.WxNav
import com.undersky.androidim.ui.theme.WxSub

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsTab(
    modifier: Modifier = Modifier,
    app: ImApp,
    session: UserSession,
    onChat: (Long) -> Unit
) {
    var users by remember { mutableStateOf<List<DirectoryUserDto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember(session.userId) { mutableIntStateOf(0) }

    LaunchedEffect(session.userId) {
        AuthTokenHolder.set(session.token)
        app.userDirectoryCacheStore.directoryFlow(session.userId).collect { raw ->
            users = sortedDirectoryUsers(raw, session.userId)
        }
    }

    LaunchedEffect(session.userId, refreshTick) {
        AuthTokenHolder.set(session.token)
        if (refreshTick == 0) {
            if (app.userDirectoryCacheStore.read(session.userId).isNotEmpty()) {
                return@LaunchedEffect
            }
        }
        loading = true
        error = null
        try {
            app.userDirectoryRepository.listAll()
                .onSuccess { list ->
                    app.userDirectoryCacheStore.save(session.userId, list)
                    error = null
                }
                .onFailure { e ->
                    val cachedEmpty = app.userDirectoryCacheStore.read(session.userId).isEmpty()
                    if (cachedEmpty) {
                        error = e.message ?: "加载失败"
                    } else {
                        error = null
                    }
                }
        } finally {
            loading = false
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("通讯录", fontSize = 22.sp) },
                actions = {
                    IconButton(
                        onClick = { refreshTick++ },
                        enabled = !loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WxNav)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalDivider(color = WxLine, thickness = 0.5.dp)
            when {
                loading && users.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WxGreen)
                    }
                }
                error != null && users.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.size(12.dp))
                            Text(
                                "点击右上角刷新重试",
                                style = MaterialTheme.typography.bodySmall,
                                color = WxSub
                            )
                        }
                    }
                }
                users.isEmpty() && !loading && error == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("暂无其他用户", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "服务器上只有您一个账号时，列表会为空（已自动隐藏自己）",
                                style = MaterialTheme.typography.bodySmall,
                                color = WxSub,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(users, key = { it.id }) { u ->
                            val title = u.username?.takeIf { it.isNotBlank() } ?: "用户 ${u.id}"
                            val sub = buildString {
                                append("ID ${u.id}")
                                u.mobile?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onChat(u.id) }
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(WxGreen.copy(alpha = 0.85f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        title.take(1),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(Modifier.size(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.size(2.dp))
                                    Text(
                                        sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WxSub,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(color = WxLine.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
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
