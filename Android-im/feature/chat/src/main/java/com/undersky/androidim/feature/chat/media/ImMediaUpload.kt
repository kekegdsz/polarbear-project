package com.undersky.androidim.feature.chat.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class ImUploadResult(
    /** 绝对 URL，可直接给 Coil / MediaPlayer */
    val url: String,
    val path: String,
    val contentType: String?,
    val size: Long
)

/**
 * POST /api/im/upload（与 Retrofit 同源 baseUrl + /api）
 */
suspend fun uploadImFile(
    client: OkHttpClient,
    apiBaseUrl: String,
    file: File,
    filename: String = file.name
): ImUploadResult = withContext(Dispatchers.IO) {
    val base = apiBaseUrl.trimEnd('/')
    val url = "$base/api/im/upload"
    val mime = guessMime(filename)
    val body = file.asRequestBody(mime.toMediaTypeOrNull())
    val part = MultipartBody.Part.createFormData("file", filename, body)
    val multipart = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addPart(part)
        .build()
    val req = Request.Builder().url(url).post(multipart).build()
    client.newCall(req).execute().use { resp ->
        val str = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
            throw IOException("上传失败 HTTP ${resp.code}: $str")
        }
        val json = JSONObject(str)
        if (json.optInt("code") != 0) {
            throw IOException(json.optString("message", "上传失败"))
        }
        val data = json.getJSONObject("data")
        val path = data.getString("path")
        val abs = data.optString("url").takeIf { it.isNotBlank() }
            ?: (base + path)
        ImUploadResult(
            url = abs,
            path = path,
            contentType = data.optString("contentType").takeIf { it.isNotBlank() },
            size = data.optLong("size", file.length())
        )
    }
}

private fun guessMime(filename: String): String {
    val lower = filename.lowercase()
    return when {
        lower.endsWith(".png") -> "image/png"
        lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
        lower.endsWith(".webp") -> "image/webp"
        lower.endsWith(".gif") -> "image/gif"
        lower.endsWith(".mp4") -> "video/mp4"
        lower.endsWith(".mov") -> "video/quicktime"
        lower.endsWith(".m4a") -> "audio/mp4"
        lower.endsWith(".aac") -> "audio/aac"
        lower.endsWith(".mp3") -> "audio/mpeg"
        lower.endsWith(".wav") -> "audio/wav"
        lower.endsWith(".pdf") -> "application/pdf"
        else -> "application/octet-stream"
    }
}
