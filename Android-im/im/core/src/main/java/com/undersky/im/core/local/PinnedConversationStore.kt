package com.undersky.im.core.local

import android.content.Context

/**
 * 本地置顶会话顺序（仅本机排序，与服务器列表无关）。
 */
class PinnedConversationStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 置顶顺序：靠前优先展示（与 [ChatConvKeys] 字符串一致）。 */
    fun getOrderedPins(ownerUserId: Long): List<String> {
        val raw = prefs.getString(key(ownerUserId), null).orEmpty()
        return raw.split(DELIM).map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun isPinned(ownerUserId: Long, convKey: String): Boolean =
        convKey in getOrderedPins(ownerUserId).toSet()

    /** @return 置顶后 true，取消置顶后 false */
    fun togglePin(ownerUserId: Long, convKey: String): Boolean {
        val cur = getOrderedPins(ownerUserId).toMutableList()
        return if (convKey in cur) {
            cur.remove(convKey)
            save(ownerUserId, cur)
            false
        } else {
            cur.remove(convKey)
            cur.add(0, convKey)
            save(ownerUserId, cur)
            true
        }
    }

    private fun save(ownerUserId: Long, keys: List<String>) {
        prefs.edit().putString(key(ownerUserId), keys.joinToString(DELIM)).apply()
    }

    private fun key(ownerUserId: Long) = "pins_$ownerUserId"

    companion object {
        private const val PREFS = "im_conv_pins"
        private const val DELIM = "\u001f"
    }
}
