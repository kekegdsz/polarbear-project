package com.undersky.im.core.api

import org.json.JSONTokener
import org.json.JSONObject

/** 富媒体消息 JSON 的 k 字段 */
object ImPayloadKind {
    const val TEXT = "text"
    const val IMG = "img"
    const val VID = "vid"
    const val FILE = "file"
    const val VOICE = "voice"
}

/**
 * 去掉 BOM、解开外层 JSON 字符串（若 WebSocket/中间层把 body 又包了一层引号）。
 */
fun unwrapImMessageBody(raw: String): String {
    var t = raw.trim()
    if (t.startsWith("\uFEFF")) {
        t = t.substring(1).trim()
    }
    while (t.length >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
        val inner = try {
            JSONTokener(t).nextValue() as? String
        } catch (_: Exception) {
            null
        } ?: break
        val next = inner.trim()
        if (next === t) break
        t = next
    }
    return t
}

/** 从富媒体 JSON 取资源地址（兼容 u / url / src）。 */
fun mediaUrlFromImJson(o: JSONObject): String {
    val u = o.optString("u").trim()
    if (u.isNotEmpty()) return u
    val url = o.optString("url").trim()
    if (url.isNotEmpty()) return url
    return o.optString("src").trim()
}

/**
 * 从消息 body 推断内容类型（与 [unwrapImMessageBody]、[parseBubbleBody] 逻辑一致）。
 * 用于 [ChatMessage.payloadKind] 等展示分支；**不是** 传输层 P2P/GROUP 的 msgType。
 */
fun inferImPayloadKind(body: String): String {
    val t = unwrapImMessageBody(body)
    if (t.isEmpty()) return ImPayloadKind.TEXT
    if (!t.startsWith("{")) return ImPayloadKind.TEXT
    return try {
        val o = JSONObject(t)
        when (val k = o.optString("k").trim()) {
            ImPayloadKind.TEXT,
            ImPayloadKind.IMG,
            ImPayloadKind.VID,
            ImPayloadKind.FILE,
            ImPayloadKind.VOICE -> k
            "image" -> ImPayloadKind.IMG
            "video" -> ImPayloadKind.VID
            "" -> {
                val mime = o.optString("m").lowercase()
                when {
                    mime.startsWith("image/") -> ImPayloadKind.IMG
                    mime.startsWith("video/") -> ImPayloadKind.VID
                    mime.startsWith("audio/") && (o.has("d") || o.has("duration")) -> ImPayloadKind.VOICE
                    mediaUrlFromImJson(o).isNotEmpty() && (o.has("n") || o.has("name")) -> ImPayloadKind.FILE
                    else -> ImPayloadKind.TEXT
                }
            }
            else -> if (k.isNotEmpty()) k else ImPayloadKind.TEXT
        }
    } catch (_: Exception) {
        ImPayloadKind.TEXT
    }
}

/**
 * 将服务端返回的相对路径（如 `/api/im/files/uuid.jpg`）补成可请求的绝对 URL。
 * [apiBaseUrl] 一般为 `http://host:port/api`。
 */
fun resolveImAttachmentUrl(raw: String, apiBaseUrl: String): String {
    val u = raw.trim()
    if (u.isEmpty()) return u
    if (u.startsWith("http://", ignoreCase = true) || u.startsWith("https://", ignoreCase = true)) {
        return u
    }
    val path = if (u.startsWith("/")) u else "/$u"
    val base = apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/api/")) {
        val root = if (base.endsWith("/api")) base.removeSuffix("/api") else base
        root + path
    } else {
        base + path
    }
}

/**
 * 会话列表、通知摘要用；纯文本原样返回（截断由调用方决定）。
 */
fun chatMessagePreviewLabel(body: String): String {
    val t = unwrapImMessageBody(body)
    if (t.isEmpty()) return ""
    if (!t.startsWith("{")) return t
    return try {
        val o = JSONObject(t)
        val durMs = when {
            o.has("d") -> o.optLong("d", 0L)
            o.has("duration") -> o.optLong("duration", 0L)
            else -> 0L
        }
        when (inferImPayloadKind(body)) {
            ImPayloadKind.TEXT -> o.optString("t", "")
            ImPayloadKind.IMG -> "[图片]"
            ImPayloadKind.VID -> {
                if (durMs > 0) {
                    val sec = ((durMs + 500) / 1000).toInt()
                    val m = sec / 60
                    val s = sec % 60
                    if (m > 0) "[视频 ${m}:${"%02d".format(s)}]"
                    else "[视频 0:${"%02d".format(sec.coerceAtMost(59))}]"
                } else {
                    "[视频]"
                }
            }
            ImPayloadKind.FILE -> {
                val n = o.optString("n", "").trim().ifEmpty { o.optString("name", "").trim() }
                if (n.isNotEmpty()) "[文件] $n" else "[文件]"
            }
            ImPayloadKind.VOICE -> {
                if (durMs > 0) "[语音 ${durMs / 1000}s]" else "[语音]"
            }
            else -> "[消息]"
        }
    } catch (_: Exception) {
        t
    }
}

fun isRichMessageBody(body: String): Boolean {
    val t = unwrapImMessageBody(body)
    if (!t.startsWith("{")) return false
    return try {
        val o = JSONObject(t)
        o.has("k") || mediaUrlFromImJson(o).isNotEmpty()
    } catch (_: Exception) {
        false
    }
}
