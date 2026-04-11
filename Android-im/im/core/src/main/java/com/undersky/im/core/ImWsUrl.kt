package com.undersky.im.core

object ImWsUrl {
    fun build(httpBase: String, wsPath: String): String {
        val base = httpBase.trimEnd('/')
        val path = if (wsPath.startsWith("/")) wsPath else "/$wsPath"
        return when {
            base.startsWith("https://") -> "wss://" + base.removePrefix("https://") + path
            base.startsWith("http://") -> "ws://" + base.removePrefix("http://") + path
            else -> "ws://$base$path"
        }
    }
}
