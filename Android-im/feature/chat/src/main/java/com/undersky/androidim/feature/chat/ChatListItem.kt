package com.undersky.androidim.feature.chat

import com.undersky.im.core.api.ChatMessage
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val BATCH_MS = 5 * 60 * 1000L

sealed class ChatListItem {
    data class TimeHeader(val anchorMsgId: Long, val epochMillis: Long) : ChatListItem()
    data class MessageRow(val message: ChatMessage, val displayName: String) : ChatListItem()
}

internal fun List<ChatMessage>.toChatListItems(displayNames: Map<Long, String>): List<ChatListItem> {
    if (isEmpty()) return emptyList()
    val sorted = sortedWith(compareBy({ it.parsedTimeMillis() }, { it.msgId }))
    val out = ArrayList<ChatListItem>(sorted.size * 2)
    var prevTime = Long.MIN_VALUE
    for (m in sorted) {
        val t = m.parsedTimeMillis()
        if (prevTime == Long.MIN_VALUE || t - prevTime > BATCH_MS) {
            out.add(ChatListItem.TimeHeader(m.msgId, t))
        }
        val name = resolveDisplayName(m.fromUserId, displayNames)
        out.add(ChatListItem.MessageRow(m, name))
        prevTime = t
    }
    return out
}

private fun resolveDisplayName(userId: Long, displayNames: Map<Long, String>): String =
    displayNames[userId]?.takeIf { it.isNotBlank() } ?: "用户 $userId"

internal fun avatarLetter(displayName: String): String {
    val t = displayName.trim()
    return if (t.isEmpty()) "?" else t.take(1)
}

private fun ChatMessage.parsedTimeMillis(): Long {
    val s = createdAt ?: return System.currentTimeMillis()
    return try {
        Instant.parse(s).toEpochMilli()
    } catch (_: Exception) {
        try {
            LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}

internal fun formatChatListTime(epochMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone)
    val today = LocalDate.now(zone)
    val msgDate = zdt.toLocalDate()
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    return when {
        msgDate == today -> timeFmt.format(zdt)
        msgDate == today.minusDays(1) -> "昨天 ${timeFmt.format(zdt)}"
        else -> DateTimeFormatter.ofPattern("M月d日 HH:mm").format(zdt)
    }
}
