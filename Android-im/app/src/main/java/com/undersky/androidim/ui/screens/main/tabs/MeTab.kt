package com.undersky.androidim.ui.screens.main.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.undersky.androidim.ImApp
import com.undersky.androidim.data.UserSession
import com.undersky.androidim.ui.theme.WxLine
import com.undersky.androidim.ui.theme.WxNav
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeTab(
    modifier: Modifier = Modifier,
    session: UserSession,
    app: ImApp
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("我", fontSize = 22.sp) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = WxNav)
        )
        HorizontalDivider(color = WxLine, thickness = 0.5.dp)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(session.username ?: "用户", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "ID：${session.userId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
        HorizontalDivider(color = WxLine.copy(alpha = 0.5f), thickness = 0.5.dp)
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable {
                    scope.launch {
                        app.sessionStore.clear()
                        app.imSocket.disconnect(clearUser = true)
                    }
                }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("退出登录", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
        }
    }
}
