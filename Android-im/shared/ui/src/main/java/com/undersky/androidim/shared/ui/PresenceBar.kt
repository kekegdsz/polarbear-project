package com.undersky.androidim.shared.ui

import android.view.View
import android.widget.TextView

/** 叠在头像底部区域内的状态文案：在线 / 离线；不展示时 [show] 为 false */
fun TextView.bindPresenceLabel(online: Boolean?, show: Boolean) {
    if (!show) {
        visibility = View.GONE
        return
    }
    visibility = View.VISIBLE
    if (online == true) {
        text = "在线"
        setBackgroundResource(R.drawable.bg_presence_overlay_online)
        setTextColor(0xFFFFFFFF.toInt())
    } else {
        text = "离线"
        setBackgroundResource(R.drawable.bg_presence_overlay_offline)
        setTextColor(0xFFFFFFFF.toInt())
    }
}
