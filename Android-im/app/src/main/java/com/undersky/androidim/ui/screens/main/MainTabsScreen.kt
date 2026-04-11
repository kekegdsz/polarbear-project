package com.undersky.androidim.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.undersky.androidim.ImApp
import com.undersky.androidim.data.ConversationItem
import com.undersky.androidim.data.ImSocketManager
import com.undersky.androidim.data.UserSession
import com.undersky.androidim.ui.screens.main.tabs.ContactsTab
import com.undersky.androidim.ui.screens.main.tabs.MessagesTab
import com.undersky.androidim.ui.screens.main.tabs.MeTab
import com.undersky.androidim.ui.theme.WxGreen
import com.undersky.androidim.ui.theme.WxNav

@Composable
fun MainTabsScreen(
    app: ImApp,
    session: UserSession,
    onOpenChatP2P: (Long) -> Unit,
    onOpenChatGroup: (Long) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val conversations = remember { mutableStateListOf<ConversationItem>() }

    LaunchedEffect(Unit) {
        app.imSocket.events.collect { ev ->
            when (ev) {
                is ImSocketManager.Event.AuthOk -> app.imSocket.requestConversations()
                is ImSocketManager.Event.Conversations -> {
                    conversations.clear()
                    conversations.addAll(ev.items)
                }
                is ImSocketManager.Event.PrivateMessage,
                is ImSocketManager.Event.GroupMessage -> app.imSocket.requestConversations()
                else -> Unit
            }
        }
    }

    LaunchedEffect(session.userId) {
        app.imSocket.requestConversations()
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = WxNav,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "消息") },
                    label = { Text("消息") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WxGreen,
                        selectedTextColor = WxGreen,
                        indicatorColor = WxNav.copy(alpha = 0.01f)
                    )
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Contacts, contentDescription = "通讯录") },
                    label = { Text("通讯录") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WxGreen,
                        selectedTextColor = WxGreen,
                        indicatorColor = WxNav.copy(alpha = 0.01f)
                    )
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "我") },
                    label = { Text("我") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WxGreen,
                        selectedTextColor = WxGreen,
                        indicatorColor = WxNav.copy(alpha = 0.01f)
                    )
                )
            }
        }
    ) { padding ->
        when (tab) {
            0 -> MessagesTab(
                modifier = Modifier.padding(padding),
                session = session,
                conversations = conversations.toList(),
                onOpenP2P = onOpenChatP2P,
                onOpenGroup = onOpenChatGroup,
                onRefresh = { app.imSocket.requestConversations() }
            )
            1 -> ContactsTab(
                modifier = Modifier.padding(padding),
                app = app,
                onChat = onOpenChatP2P
            )
            2 -> MeTab(
                modifier = Modifier.padding(padding),
                session = session,
                app = app
            )
        }
    }
}
