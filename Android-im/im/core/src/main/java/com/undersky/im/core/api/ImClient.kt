package com.undersky.im.core.api

import com.undersky.im.core.CHAT_PAGE_SIZE
import kotlinx.coroutines.flow.Flow

/**
 * IM 传输层对外接口（无 UI）。由宿主通过 [com.undersky.im.core.ImCore] 创建实现。
 */
interface ImClient {

    val events: Flow<ImEvent>

    fun connect(userId: Long)

    fun disconnect(clearUser: Boolean = true)

    fun sendRaw(json: String)

    fun sendAuth(userId: Long)

    fun requestConversations()

    fun requestUserInfo(userId: Long)

    fun requestHistoryP2P(
        peerUserId: Long,
        beforeId: Long? = null,
        afterId: Long? = null,
        limit: Int = CHAT_PAGE_SIZE
    )

    fun requestHistoryGroup(
        groupId: Long,
        beforeId: Long? = null,
        afterId: Long? = null,
        limit: Int = CHAT_PAGE_SIZE
    )

    fun sendPrivate(toUserId: Long, body: String)

    fun sendGroup(groupId: Long, body: String)
}
