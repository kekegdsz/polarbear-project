package com.undersky.im.core.api

/** WebSocket 与鉴权链路的大致状态，供 UI 展示「连接中 / 已断开」等。 */
enum class ImConnectionState {
    Disconnected,
    Connecting,
    Connected,
}
