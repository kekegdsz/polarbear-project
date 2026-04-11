package com.undersky.androidim.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.undersky.androidim.ImApp
import com.undersky.androidim.data.ChatMessage
import com.undersky.androidim.data.ImSocketManager
import com.undersky.androidim.data.UserSession
import com.undersky.androidim.ui.theme.WxBubbleIn
import com.undersky.androidim.ui.theme.WxBubbleOut
import com.undersky.androidim.ui.theme.WxGreen
import com.undersky.androidim.ui.theme.WxLine
import com.undersky.androidim.ui.theme.WxNav
import com.undersky.androidim.ui.theme.WxSub

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    app: ImApp,
    session: UserSession,
    peerUserId: Long?,
    groupId: Long?,
    titleFallback: String,
    onBack: () -> Unit
) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var title by remember { mutableStateOf(titleFallback) }
    val listState = rememberLazyListState()
    val isP2P = peerUserId != null

    LaunchedEffect(peerUserId, groupId) {
        messages.clear()
        if (isP2P) {
            app.imSocket.requestHistoryP2P(peerUserId!!)
            app.imSocket.requestUserInfo(peerUserId)
        } else {
            app.imSocket.requestHistoryGroup(groupId!!)
        }
    }

    LaunchedEffect(Unit) {
        app.imSocket.events.collect { ev ->
            when (ev) {
                is ImSocketManager.Event.HistoryResult -> {
                    val filtered = ev.messages.filter { m ->
                        if (isP2P) {
                            val p = peerUserId!!
                            m.msgType == "P2P" && (
                                (m.fromUserId == p && m.toUserId == session.userId) ||
                                    (m.fromUserId == session.userId && m.toUserId == p)
                                )
                        } else {
                            m.msgType == "GROUP" && m.groupId == groupId
                        }
                    }
                    messages.clear()
                    messages.addAll(filtered)
                }
                is ImSocketManager.Event.PrivateMessage -> {
                    if (!isP2P) return@collect
                    val m = ev.message
                    val peer = peerUserId!!
                    val involved = m.fromUserId == peer || m.toUserId == peer
                    val me = session.userId
                    if (involved && (m.fromUserId == me || m.toUserId == me)) {
                        if (messages.none { it.msgId == m.msgId }) {
                            messages.add(m)
                        }
                    }
                }
                is ImSocketManager.Event.GroupMessage -> {
                    if (isP2P) return@collect
                    val m = ev.message
                    if (m.groupId == groupId && messages.none { it.msgId == m.msgId }) {
                        messages.add(m)
                    }
                }
                is ImSocketManager.Event.UserInfoResult -> {
                    if (isP2P && ev.userId == peerUserId) {
                        title = ev.username?.takeIf { it.isNotBlank() } ?: titleFallback
                    }
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WxNav)
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(WxNav)
                    .padding(8.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("发送消息…", color = WxSub) },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WxLine,
                        unfocusedBorderColor = WxLine,
                        cursorColor = WxGreen
                    )
                )
                IconButton(
                    onClick = {
                        val t = input.trim()
                        if (t.isEmpty()) return@IconButton
                        if (isP2P) {
                            app.imSocket.sendPrivate(peerUserId!!, t)
                        } else {
                            app.imSocket.sendGroup(groupId!!, t)
                        }
                        input = ""
                    },
                    enabled = input.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送", tint = WxGreen)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
        ) {
            items(messages, key = { it.msgId }) { msg ->
                val mine = msg.fromUserId == session.userId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .background(
                                if (mine) WxBubbleOut else WxBubbleIn,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(msg.body, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
