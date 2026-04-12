package com.undersky.im.core.api

data class ChatMessage(
    val msgId: Long,
    /** 传输类型：P2P / GROUP（与富媒体 JSON 里的 k 无关）。 */
    val msgType: String,
    val fromUserId: Long,
    val toUserId: Long?,
    val groupId: Long?,
    val body: String,
    val createdAt: String?,
    /** 内容类型：与 [ImPayloadKind] 对齐，由 [inferImPayloadKind] 从 body 推断。 */
    val payloadKind: String = inferImPayloadKind(body),
    /**
     * 仅本机：当前用户发送的媒体在磁盘上的缓存路径（发送前上传用的文件），气泡优先用其展示；
     * 不来自服务端，持久化在 Room 供重进会话后仍能读图/视频首帧。
     */
    val localMediaPath: String? = null
)

data class ConversationItem(
    val convType: String,
    val peerUserId: Long?,
    val groupId: Long?,
    val lastMessage: ChatMessage,
    val unreadCount: Int = 0,
    val groupName: String? = null
)

/** 群成员角色：OWNER / ADMIN / MEMBER */
data class GroupMemberRow(val userId: Long, val role: String)
