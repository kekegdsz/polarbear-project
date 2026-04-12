package com.undersky.im.core.local

import android.content.Context

/**
 * 从消息列表中隐藏的会话（仅本机 UI，不删服务端数据；新消息到达后仍会出现在列表除非再次隐藏）。
 */
class HiddenConversationStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getHidden(ownerUserId: Long): Set<String> =
        HashSet(prefs.getStringSet(key(ownerUserId), emptySet()) ?: emptySet())

    fun hide(ownerUserId: Long, convKey: String) {
        val next = HashSet(getHidden(ownerUserId))
        next.add(convKey)
        prefs.edit().putStringSet(key(ownerUserId), next).apply()
    }

    fun unhide(ownerUserId: Long, convKey: String) {
        val next = HashSet(getHidden(ownerUserId))
        next.remove(convKey)
        if (next.isEmpty()) {
            prefs.edit().remove(key(ownerUserId)).apply()
        } else {
            prefs.edit().putStringSet(key(ownerUserId), next).apply()
        }
    }

    private fun key(ownerUserId: Long) = "hidden_$ownerUserId"

    companion object {
        private const val PREFS = "im_hidden_conv"
    }
}
