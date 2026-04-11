package com.undersky.androidim.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * @param padTop 状态栏 + 刘海顶部留白，避免标题与系统状态栏文字重叠
 * @param padBottom 导航栏底部留白（手势条区域）
 */
fun View.applyWindowInsetsPadding(padTop: Boolean = false, padBottom: Boolean = false) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val top = if (padTop) {
            windowInsets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
        } else {
            v.paddingTop
        }
        val bottom = if (padBottom) {
            windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        } else {
            v.paddingBottom
        }
        v.updatePadding(top = top, bottom = bottom)
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.applyStatusBarTopInset() = applyWindowInsetsPadding(padTop = true, padBottom = false)

fun View.applyNavigationBarBottomInset() = applyWindowInsetsPadding(padTop = false, padBottom = true)
