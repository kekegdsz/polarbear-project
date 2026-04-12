package com.undersky.androidim.feature.chat.media

import com.undersky.androidim.feature.chat.BubbleContent
import com.undersky.androidim.feature.chat.parseBubbleBody
import com.undersky.im.core.api.ChatMessage
import com.undersky.im.core.api.resolveImAttachmentUrl
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import java.io.File

@Parcelize
data class ChatVisualMediaPage(
    val msgId: Long,
    val isVideo: Boolean,
    val remoteUrl: String,
    val localPath: String?
) : Parcelable {

    fun displayUriString(): String {
        val p = localPath?.trim().orEmpty()
        if (p.isNotEmpty()) {
            val f = File(p)
            if (f.exists() && f.canRead()) return f.absolutePath
        }
        return remoteUrl
    }
}

/**
 * 当前会话内可左右滑浏览的图片 / 视频（按消息时间升序，与列表一致）。
 */
fun buildChatVisualMediaPages(messages: List<ChatMessage>, apiBaseUrl: String): List<ChatVisualMediaPage> {
    val base = apiBaseUrl.trimEnd('/')
    return messages.mapNotNull { m ->
        when (val c = parseBubbleBody(m.body)) {
            is BubbleContent.ImageMsg -> {
                val remote = resolveImAttachmentUrl(c.url, base)
                ChatVisualMediaPage(
                    msgId = m.msgId,
                    isVideo = false,
                    remoteUrl = remote,
                    localPath = m.localMediaPath
                )
            }
            is BubbleContent.VideoMsg -> {
                val remote = resolveImAttachmentUrl(c.url, base)
                ChatVisualMediaPage(
                    msgId = m.msgId,
                    isVideo = true,
                    remoteUrl = remote,
                    localPath = m.localMediaPath
                )
            }
            else -> null
        }
    }
}
