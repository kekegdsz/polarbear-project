package com.undersky.androidim.feature.chat

import com.undersky.im.core.api.ImPayloadKind
import com.undersky.im.core.api.mediaUrlFromImJson
import com.undersky.im.core.api.unwrapImMessageBody
import org.json.JSONObject

/** 聊天气泡展示用（与 JSON k 字段对应） */
sealed class BubbleContent {
    data class PlainText(val text: String) : BubbleContent()
    data class ImageMsg(val url: String, val mime: String) : BubbleContent()
    data class VideoMsg(val url: String, val mime: String, val durationMs: Long) : BubbleContent()
    data class FileMsg(val url: String, val name: String, val mime: String) : BubbleContent()
    data class VoiceMsg(val url: String, val durationMs: Long) : BubbleContent()
}

fun parseBubbleBody(body: String): BubbleContent {
    val t = unwrapImMessageBody(body)
    if (!t.startsWith("{")) return BubbleContent.PlainText(t)
    return try {
        val o = JSONObject(t)
        val mediaUrl = mediaUrlFromImJson(o)
        val k = o.optString("k").trim()
        val effectiveKind = when {
            k.isNotEmpty() -> k
            mediaUrl.isNotEmpty() && (o.has("n") || o.has("name")) -> ImPayloadKind.FILE
            mediaUrl.isNotEmpty() && (o.has("d") || o.has("duration")) &&
                o.optString("m").lowercase().startsWith("audio/") -> ImPayloadKind.VOICE
            mediaUrl.isNotEmpty() && o.optString("m").lowercase().startsWith("video/") -> ImPayloadKind.VID
            mediaUrl.isNotEmpty() && o.optString("m").lowercase().startsWith("image/") -> ImPayloadKind.IMG
            else -> ""
        }
        when (effectiveKind) {
            ImPayloadKind.TEXT -> BubbleContent.PlainText(o.optString("t", ""))
            ImPayloadKind.IMG, "image" -> BubbleContent.ImageMsg(
                mediaUrl,
                o.optString("m", "image/jpeg").ifBlank { "image/jpeg" }
            )
            ImPayloadKind.VID, "video" -> BubbleContent.VideoMsg(
                mediaUrl,
                o.optString("m", "video/mp4").ifBlank { "video/mp4" },
                when {
                    o.has("d") -> o.optLong("d", 0L)
                    o.has("duration") -> o.optLong("duration", 0L)
                    else -> 0L
                }
            )
            ImPayloadKind.FILE -> BubbleContent.FileMsg(
                mediaUrl,
                o.optString("n").ifBlank { o.optString("name", "文件") },
                o.optString("m", "application/octet-stream").ifBlank { "application/octet-stream" }
            )
            ImPayloadKind.VOICE -> BubbleContent.VoiceMsg(
                mediaUrl,
                when {
                    o.has("d") -> o.optLong("d", 0L)
                    o.has("duration") -> o.optLong("duration", 0L)
                    else -> 0L
                }
            )
            else -> BubbleContent.PlainText(t)
        }
    } catch (_: Exception) {
        BubbleContent.PlainText(t)
    }
}

internal fun buildTextJson(text: String): String =
    JSONObject().put("k", ImPayloadKind.TEXT).put("t", text).toString()

internal fun buildImageJson(url: String, mime: String, w: Int?, h: Int?): String {
    val o = JSONObject().put("k", ImPayloadKind.IMG).put("u", url).put("m", mime)
    if (w != null && w > 0) o.put("w", w)
    if (h != null && h > 0) o.put("h", h)
    return o.toString()
}

internal fun buildVideoJson(url: String, mime: String, durationMs: Long?): String {
    val o = JSONObject().put("k", ImPayloadKind.VID).put("u", url).put("m", mime)
    if (durationMs != null && durationMs > 0) o.put("d", durationMs)
    return o.toString()
}

internal fun buildFileJson(url: String, name: String, mime: String, size: Long): String =
    JSONObject()
        .put("k", ImPayloadKind.FILE)
        .put("u", url)
        .put("n", name)
        .put("m", mime)
        .put("s", size)
        .toString()

internal fun buildVoiceJson(url: String, durationMs: Long): String =
    JSONObject()
        .put("k", ImPayloadKind.VOICE)
        .put("u", url)
        .put("d", durationMs)
        .toString()
