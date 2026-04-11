package com.undersky.androidim.data

import java.util.concurrent.atomic.AtomicReference

/**
 * 为 OkHttp 拦截器提供当前登录 token（与 DataStore 会话同步）。
 */
object AuthTokenHolder {
    private val ref = AtomicReference<String?>(null)

    fun set(token: String?) {
        ref.set(token?.takeIf { it.isNotBlank() })
    }

    fun get(): String? = ref.get()
}
