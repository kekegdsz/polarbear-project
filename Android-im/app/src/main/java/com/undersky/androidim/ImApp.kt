package com.undersky.androidim

import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.androidim.bootstrap.HostConfig

class ImApp : BootstrapApplication() {
    override fun hostConfig(): HostConfig = HostConfig(
        apiBaseUrl = BuildConfig.API_BASE_URL,
        imWebSocketPath = BuildConfig.IM_WS_PATH
    )
}
