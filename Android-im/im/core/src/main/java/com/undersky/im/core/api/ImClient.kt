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

    /** [memberUserIds] 不含当前用户；服务端会将当前用户作为群主并入群。 */
    fun createGroup(name: String?, memberUserIds: List<Long>)

    /**
     * 发送建群并等待 [ImEvent.GroupCreated]（或服务端 [ImEvent.Error] 对应的异常），不依赖 [events] 的订阅时序。
     * 超时默认 15s，抛出 [kotlinx.coroutines.TimeoutCancellationException]。
     */
    suspend fun createGroupAndAwait(name: String?, memberUserIds: List<Long>): ImEvent.GroupCreated

    fun requestGroupInfo(groupId: Long)

    fun renameGroup(groupId: Long, name: String)

    fun setGroupAdmin(groupId: Long, targetUserId: Long)

    fun removeGroupAdmin(groupId: Long, targetUserId: Long)
}
