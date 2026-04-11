package com.undersky.androidim.feature.home

interface MainChatNavigator {
    fun openChatP2P(peerUserId: Long, titleFallback: String)
    fun openChatGroup(groupId: Long)
}
