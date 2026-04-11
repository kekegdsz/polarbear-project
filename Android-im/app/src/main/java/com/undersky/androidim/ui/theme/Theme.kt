package com.undersky.androidim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val WxGreen = Color(0xFF07C160)
val WxBg = Color(0xFFEDEDED)
val WxNav = Color(0xFFEDEDED)
val WxBubbleOut = Color(0xFF95EC69)
val WxBubbleIn = Color(0xFFFFFFFF)
val WxLine = Color(0xFFD9D9D9)
val WxSub = Color(0xFF888888)

private val WxLight = lightColorScheme(
    primary = WxGreen,
    onPrimary = Color.White,
    surface = WxBg,
    background = WxBg,
    onSurface = Color(0xFF111111),
    outline = WxLine
)

@Composable
fun WxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WxLight,
        content = content
    )
}
