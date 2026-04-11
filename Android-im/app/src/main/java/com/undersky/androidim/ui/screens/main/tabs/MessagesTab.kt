package com.undersky.androidim.ui.screens.main.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.undersky.androidim.data.ConversationItem
import com.undersky.androidim.data.UserSession
import com.undersky.androidim.ui.theme.WxGreen
import com.undersky.androidim.ui.theme.WxLine
import com.undersky.androidim.ui.theme.WxNav
import com.undersky.androidim.ui.theme.WxSub

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesTab(
    modifier: Modifier = Modifier,
    session: UserSession,
    conversations: List<ConversationItem>,
    onOpenP2P: (Long) -> Unit,
    onOpenGroup: (Long) -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("消息", fontSize = 22.sp) },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = WxNav)
        )
        HorizontalDivider(color = WxLine, thickness = 0.5.dp)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            items(conversations, key = {
                when (it.convType) {
                    "P2P" -> "p2p-${it.peerUserId}"
                    "GROUP" -> "g-${it.groupId}"
                    else -> it.lastMessage.msgId.toString()
                }
            }) { item ->
                ConversationRow(
                    session = session,
                    item = item,
                    onClick = {
                        when (item.convType) {
                            "P2P" -> item.peerUserId?.let(onOpenP2P)
                            "GROUP" -> item.groupId?.let(onOpenGroup)
                        }
                    }
                )
                HorizontalDivider(color = WxLine.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun ConversationRow(
    session: UserSession,
    item: ConversationItem,
    onClick: () -> Unit
) {
    val title = when (item.convType) {
        "P2P" -> {
            val peer = item.peerUserId ?: 0L
            if (peer == session.userId) "我" else "用户 $peer"
        }
        "GROUP" -> "群聊 ${item.groupId ?: ""}"
        else -> "会话"
    }
    val preview = item.lastMessage.body
    val time = item.lastMessage.createdAt?.takeLast(8) ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(WxGreen.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.take(1),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(time, style = MaterialTheme.typography.labelSmall, color = WxSub)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                preview,
                style = MaterialTheme.typography.bodyMedium,
                color = WxSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
