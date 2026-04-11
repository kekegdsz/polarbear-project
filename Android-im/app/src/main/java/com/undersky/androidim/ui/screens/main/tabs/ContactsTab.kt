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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.undersky.androidim.ImApp
import com.undersky.androidim.data.ContactEntry
import com.undersky.androidim.ui.theme.WxGreen
import com.undersky.androidim.ui.theme.WxLine
import com.undersky.androidim.ui.theme.WxNav
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsTab(
    modifier: Modifier = Modifier,
    app: ImApp,
    onChat: (Long) -> Unit
) {
    var contacts by remember { mutableStateOf<List<ContactEntry>>(emptyList()) }
    var showAdd by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        app.contactStore.contactsFlow.collectLatest { contacts = it }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("通讯录", fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WxNav)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = WxGreen,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalDivider(color = WxLine, thickness = 0.5.dp)
            LazyColumn(Modifier.fillMaxSize()) {
                items(contacts, key = { it.userId }) { c ->
                    val label = c.remark?.takeIf { it.isNotBlank() } ?: "用户 ${c.userId}"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChat(c.userId) }
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
                                label.take(1),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HorizontalDivider(color = WxLine.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }

    if (showAdd) {
        var uidText by remember { mutableStateOf("") }
        var remarkText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加联系人") },
            text = {
                Column {
                    OutlinedTextField(
                        value = uidText,
                        onValueChange = { uidText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("对方用户 ID（数字）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value = remarkText,
                        onValueChange = { remarkText = it },
                        label = { Text("备注（可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = uidText.toLongOrNull()
                        if (id != null && id > 0) {
                            scope.launch {
                                app.contactStore.add(ContactEntry(id, remarkText.ifBlank { null }))
                                showAdd = false
                            }
                        }
                    }
                ) { Text("保存", color = WxGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("取消") }
            }
        )
    }
}
