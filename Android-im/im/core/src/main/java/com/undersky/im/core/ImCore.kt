package com.undersky.im.core

import com.undersky.im.core.api.ImClient
import com.undersky.im.core.internal.DefaultImClient
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient

object ImCore {
    fun createClient(
        httpClient: OkHttpClient,
        coroutineScope: CoroutineScope,
        httpBaseUrl: String,
        webSocketPath: String
    ): ImClient {
        val wsUrl = ImWsUrl.build(httpBaseUrl, webSocketPath)
        return DefaultImClient(httpClient, coroutineScope, wsUrl)
    }
}
