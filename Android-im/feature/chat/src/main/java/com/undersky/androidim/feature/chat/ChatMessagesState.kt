package com.undersky.androidim.feature.chat

import com.undersky.im.core.api.ChatMessage

data class ChatMessagesState(
    val messages: List<ChatMessage>,
    val scroll: ChatScroll
)

sealed class ChatScroll {
    /** 滚到列表底部（新消息、首屏、历史首包） */
    data object ToBottom : ChatScroll()

    /** 在列表顶部插入更早消息时保持视口不跳 */
    data object KeepScroll : ChatScroll()

    /** 仅刷新展示（如昵称），不改变滚动位置 */
    data object NoScroll : ChatScroll()
}
