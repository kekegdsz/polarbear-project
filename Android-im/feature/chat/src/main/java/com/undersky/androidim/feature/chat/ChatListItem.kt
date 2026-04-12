package com.undersky.androidim.feature.chat

import com.undersky.im.core.api.ChatMessage
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val BATCH_MS = 5 * 60 * 1000L

sealed class ChatListItem {
    data class TimeHeader(val anchorMsgId: Long, val epochMillis: Long) : ChatListItem()
    /**
     * @param showSenderMeta 为 false 时与上一条为同一发送者且中间无时间条：隐藏头像与昵称（对标 Telegram 等折叠）。
     */
    data class MessageRow(
        val message: ChatMessage,
        val displayName: String,
        val showSenderMeta: Boolean = true
    ) : ChatListItem()
}

/**
 * 将消息转为列表项：顺序 **严格按 msgId 升序**（与服务器一致），避免用「当前时间」占位导致每次 submit 重排。
 * 时间分隔条仍按相邻消息的解析时间间隔插入；无法解析的时间用 0L，展示为 "--:--"。
 */
internal fun List<ChatMessage>.toChatListItems(displayNames: Map<Long, String>): List<ChatListItem> {
    if (isEmpty()) return emptyList()
    val sorted = sortedBy { it.msgId }
    val out = ArrayList<ChatListItem>(sorted.size * 2)
    var prevTime = Long.MIN_VALUE
    var lastSenderForCompact: Long? = null
    var timeHeaderJustInserted = false
    for (m in sorted) {
        val t = m.parsedTimeMillisForOrdering()
        val timeBackwards =
            prevTime != Long.MIN_VALUE && t > 0L && prevTime > 0L && t < prevTime
        if (prevTime == Long.MIN_VALUE || t - prevTime > BATCH_MS || timeBackwards) {
            out.add(ChatListItem.TimeHeader(m.msgId, t))
            timeHeaderJustInserted = true
        }
        val name = resolveDisplayName(m.fromUserId, displayNames)
        val showMeta =
            lastSenderForCompact == null || timeHeaderJustInserted || lastSenderForCompact != m.fromUserId
        out.add(ChatListItem.MessageRow(m, name, showMeta))
        lastSenderForCompact = m.fromUserId
        timeHeaderJustInserted = false
        prevTime = if (t > 0L) t else prevTime
    }
    return out
}

private fun resolveDisplayName(userId: Long, displayNames: Map<Long, String>): String =
    displayNames[userId]?.takeIf { it.isNotBlank() } ?: "用户 $userId"

internal fun avatarLetter(displayName: String): String {
    val t = displayName.trim()
    return if (t.isEmpty()) "?" else t.take(1)
}

/**
 * 用于排序与时间条间隔：绝不使用 [System.currentTimeMillis]（会导致同一批消息每次列表刷新顺序变化）。
 * 无法解析时返回 0L，与 [formatChatListTime] 的占位展示一致。
 */
private fun ChatMessage.parsedTimeMillisForOrdering(): Long {
    val s = createdAt?.trim().orEmpty()
    if (s.isEmpty()) return 0L
    return parseCreatedAtToEpochMillis(s) ?: 0L
}

private fun parseCreatedAtToEpochMillis(s: String): Long? {
    if (s.isEmpty()) return null
    return try {
        Instant.parse(s).toEpochMilli()
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }
}

internal fun formatChatListTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "--:--"
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
