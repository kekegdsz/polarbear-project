package com.undersky.androidim.feature.chat.media

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

internal fun copyUriToCacheFile(context: Context, uri: Uri, destName: String): File {
    val dir = File(context.cacheDir, "im_upload").apply { mkdirs() }
    val out = File(dir, destName)
    context.contentResolver.openInputStream(uri)!!.use { input ->
        out.outputStream().use { input.copyTo(it) }
    }
    return out
}

internal fun queryDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) {
                val s = c.getString(idx)
                if (!s.isNullOrBlank()) return s
            }
        }
    }
    return "file"
}

internal fun decodeImageSize(context: Context, uri: Uri): Pair<Int, Int>? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    }
    return if (opts.outWidth > 0 && opts.outHeight > 0) opts.outWidth to opts.outHeight else null
}
