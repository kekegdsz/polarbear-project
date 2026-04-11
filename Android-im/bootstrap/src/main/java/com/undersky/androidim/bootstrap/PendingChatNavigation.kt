package com.undersky.androidim.bootstrap

data class PendingChatNavigation(
    val peerUserId: Long,
    val groupId: Long,
    val titleFallback: String
)
