package com.undersky.androidim.feature.home

interface MainChatNavigator {
    fun openChatP2P(peerUserId: Long)
    fun openChatGroup(groupId: Long)
}
