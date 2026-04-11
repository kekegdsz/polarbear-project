package com.undersky.core.common

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

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
