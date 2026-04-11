package com.undersky.business.user

import java.util.concurrent.atomic.AtomicReference

object AuthTokenHolder {
    private val ref = AtomicReference<String?>(null)

    fun set(token: String?) {
        ref.set(token?.takeIf { it.isNotBlank() })
    }

    fun get(): String? = ref.get()
}
