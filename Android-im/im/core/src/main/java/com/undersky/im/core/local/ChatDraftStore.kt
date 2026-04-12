package com.undersky.im.core.local

import android.content.Context

/**
 * 按「当前登录用户 + 会话」保存输入框草稿（与客户端包名无关，固定 prefs 名以便多模块共用）。
 */
class ChatDraftStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(ownerUserId: Long, convKey: String): String =
        prefs.getString(key(ownerUserId, convKey), null).orEmpty()

    fun put(ownerUserId: Long, convKey: String, text: String) {
        val k = key(ownerUserId, convKey)
        val t = text.trimEnd()
        if (t.isEmpty()) {
            prefs.edit().remove(k).apply()
        } else {
            prefs.edit().putString(k, text).apply()
        }
    }

    private fun key(ownerUserId: Long, convKey: String) = "$ownerUserId|$convKey"

    companion object {
        private const val PREFS = "im_chat_drafts"
    }
}
