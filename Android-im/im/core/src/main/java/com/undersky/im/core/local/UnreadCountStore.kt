package com.undersky.im.core.local

import android.content.Context

/**
 * 本地未读计数（按会话）。可由 UI 层组合服务端会话列表展示。
 */
class UnreadCountStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("im_unread_v1", Context.MODE_PRIVATE)

    private fun keyP2p(peerUserId: Long) = "p2p_$peerUserId"
    private fun keyGroup(groupId: Long) = "g_$groupId"

    fun getP2p(peerUserId: Long): Int =
        if (peerUserId <= 0) 0 else prefs.getInt(keyP2p(peerUserId), 0)

    fun getGroup(groupId: Long): Int =
        if (groupId <= 0) 0 else prefs.getInt(keyGroup(groupId), 0)

    fun incrementP2p(peerUserId: Long) {
        if (peerUserId <= 0) return
        val k = keyP2p(peerUserId)
        prefs.edit().putInt(k, (prefs.getInt(k, 0) + 1).coerceAtMost(9999)).apply()
    }

    fun incrementGroup(groupId: Long) {
        if (groupId <= 0) return
        val k = keyGroup(groupId)
        prefs.edit().putInt(k, (prefs.getInt(k, 0) + 1).coerceAtMost(9999)).apply()
    }

    fun clearP2p(peerUserId: Long) {
        if (peerUserId <= 0) return
        prefs.edit().remove(keyP2p(peerUserId)).apply()
    }

    fun clearGroup(groupId: Long) {
        if (groupId <= 0) return
        prefs.edit().remove(keyGroup(groupId)).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
