package com.undersky.androidim.ui.main

interface MainChatNavigator {
    fun openChatP2P(peerUserId: Long)
    fun openChatGroup(groupId: Long)
}
