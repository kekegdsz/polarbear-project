package com.undersky.androidim.feature.chat.media

import android.media.MediaMetadataRetriever
import java.io.File

/** 本地视频文件时长（毫秒），失败返回 null */
fun extractVideoDurationMs(file: File): Long? {
    if (!file.exists() || file.length() == 0L) return null
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(file.absolutePath)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    } catch (_: Exception) {
        null
    } finally {
        try {
            r.release()
        } catch (_: Exception) {
        }
    }
}
