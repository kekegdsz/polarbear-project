package com.undersky.core.common

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * 为根布局应用系统栏 padding。
 * 底部同时考虑导航栏与输入法（IME），避免聊天/表单底部被键盘遮挡。
 * （与 [android:windowSoftInputMode] adjustResize 配合：若系统已压缩窗口，IME bottom 常为 0，不会重复顶起。）
 */
fun View.applyWindowInsetsPadding(
    padTop: Boolean = false,
    padBottom: Boolean = false,
    onWindowInsetsApplied: ((WindowInsetsCompat) -> Unit)? = null
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val top = if (padTop) {
            windowInsets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
        } else {
            v.paddingTop
        }
        val bottom = if (padBottom) {
            val nav = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            maxOf(nav, ime)
        } else {
            v.paddingBottom
        }
        v.updatePadding(top = top, bottom = bottom)
        onWindowInsetsApplied?.invoke(windowInsets)
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.applyStatusBarTopInset() = applyWindowInsetsPadding(padTop = true, padBottom = false)

fun View.applyNavigationBarBottomInset() = applyWindowInsetsPadding(padTop = false, padBottom = true)
