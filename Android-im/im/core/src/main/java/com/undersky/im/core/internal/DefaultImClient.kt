package com.undersky.im.core.internal

import com.undersky.im.core.api.ChatMessage
import com.undersky.im.core.api.ConversationItem
import com.undersky.im.core.api.GroupMemberRow
import com.undersky.im.core.api.ImClient
import com.undersky.im.core.api.ImConnectionState
import com.undersky.im.core.api.ImEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import kotlin.jvm.Volatile

internal class DefaultImClient(
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
    private val wsUrl: String
) : ImClient {

    private val _events = MutableSharedFlow<ImEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val events: Flow<ImEvent> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow(ImConnectionState.Disconnected)
    override val connectionState: StateFlow<ImConnectionState> = _connectionState.asStateFlow()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var authUserId: Long? = null

    private var reconnectJob: Job? = null

    /** 与 [createGroupAndAwait] 配对，避免仅靠 SharedFlow 订阅导致漏收 GROUP_CREATED。 */
    private var groupCreateAwait: CompletableDeferred<ImEvent.GroupCreated>? = null

    override fun connect(userId: Long) {
        authUserId = userId
        reconnectJob?.cancel()
        _connectionState.value = ImConnectionState.Connecting
        scope.launch(Dispatchers.IO) {
            synchronized(this@DefaultImClient) {
                webSocket?.cancel()
                val request = Request.Builder().url(wsUrl).build()
                webSocket = client.newWebSocket(request, listener(userId))
            }
        }
    }

    override fun disconnect(clearUser: Boolean) {
        reconnectJob?.cancel()
        reconnectJob = null
        if (clearUser) {
            authUserId = null
        }
        synchronized(this) {
            val pending = groupCreateAwait
            groupCreateAwait = null
            pending?.cancel(CancellationException("disconnected"))
            webSocket?.close(1000, "logout")
            webSocket = null
        }
        _connectionState.value = ImConnectionState.Disconnected
    }

    override fun sendRaw(json: String) {
        webSocket?.send(json)
    }

    override fun sendAuth(userId: Long) {
        sendRaw("""{"type":"AUTH","userId":$userId}""")
    }

    override fun requestConversations() {
        sendRaw("""{"type":"CONVERSATIONS"}""")
    }

    override fun requestUserInfo(userId: Long) {
        sendRaw("""{"type":"USER_INFO","userId":$userId}""")
    }

    override fun requestHistoryP2P(peerUserId: Long, beforeId: Long?, afterId: Long?, limit: Int) {
        require(beforeId == null || afterId == null) { "beforeId 与 afterId 不能同时指定" }
        val extra = buildString {
            beforeId?.let { append(",\"beforeId\":$it") }
            afterId?.let { append(",\"afterId\":$it") }
        }
        sendRaw("""{"type":"HISTORY","mode":"P2P","peerUserId":$peerUserId,"limit":$limit$extra}""")
    }

    override fun requestHistoryGroup(groupId: Long, beforeId: Long?, afterId: Long?, limit: Int) {
        require(beforeId == null || afterId == null) { "beforeId 与 afterId 不能同时指定" }
        val extra = buildString {
            beforeId?.let { append(",\"beforeId\":$it") }
            afterId?.let { append(",\"afterId\":$it") }
        }
        sendRaw("""{"type":"HISTORY","mode":"GROUP","groupId":$groupId,"limit":$limit$extra}""")
    }

    override fun sendPrivate(toUserId: Long, body: String) {
        val o = JSONObject()
        o.put("type", "PRIVATE_SEND")
        o.put("toUserId", toUserId)
        o.put("body", body)
        sendRaw(o.toString())
    }

    override fun sendGroup(groupId: Long, body: String) {
        val o = JSONObject()
        o.put("type", "GROUP_SEND")
        o.put("groupId", groupId)
        o.put("body", body)
        sendRaw(o.toString())
    }

    override fun createGroup(name: String?, memberUserIds: List<Long>) {
        val o = JSONObject()
        o.put("type", "GROUP_CREATE")
        if (name != null) o.put("name", name)
        val arr = JSONArray()
        memberUserIds.forEach { arr.put(it) }
        o.put("memberIds", arr)
        sendRaw(o.toString())
    }

    override suspend fun createGroupAndAwait(name: String?, memberUserIds: List<Long>): ImEvent.GroupCreated {
        val deferred = CompletableDeferred<ImEvent.GroupCreated>()
        synchronized(this) {
            check(groupCreateAwait == null) { "已有建群请求进行中" }
            groupCreateAwait = deferred
        }
        try {
            createGroup(name, memberUserIds)
            return withTimeout(15_000L) { deferred.await() }
        } catch (e: TimeoutCancellationException) {
            synchronized(this) {
                if (groupCreateAwait === deferred) groupCreateAwait = null
            }
            throw e
        } catch (e: CancellationException) {
            synchronized(this) {
                if (groupCreateAwait === deferred) groupCreateAwait = null
            }
            throw e
        } finally {
            synchronized(this) {
                if (groupCreateAwait === deferred) groupCreateAwait = null
            }
        }
    }

    override fun requestGroupInfo(groupId: Long) {
        sendRaw("""{"type":"GROUP_INFO","groupId":$groupId}""")
    }

    override fun renameGroup(groupId: Long, name: String) {
        val o = JSONObject()
        o.put("type", "GROUP_RENAME")
        o.put("groupId", groupId)
        o.put("name", name)
        sendRaw(o.toString())
    }

    override fun setGroupAdmin(groupId: Long, targetUserId: Long) {
        val o = JSONObject()
        o.put("type", "GROUP_SET_ADMIN")
        o.put("groupId", groupId)
        o.put("targetUserId", targetUserId)
        sendRaw(o.toString())
    }

    override fun removeGroupAdmin(groupId: Long, targetUserId: Long) {
        val o = JSONObject()
        o.put("type", "GROUP_REMOVE_ADMIN")
        o.put("groupId", groupId)
        o.put("targetUserId", targetUserId)
        sendRaw(o.toString())
    }

    private fun listener(expectedUserId: Long) = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            scope.launch {
                _connectionState.value = ImConnectionState.Connected
                _events.emit(ImEvent.SocketOpen)
            }
            sendAuth(expectedUserId)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch { dispatchMessage(text) }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            scope.launch {
                _connectionState.value = ImConnectionState.Disconnected
                _events.emit(ImEvent.SocketClosed)
                scheduleReconnectIfNeeded()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            scope.launch {
                _events.emit(ImEvent.Error(t.message ?: "连接失败"))
                _connectionState.value = ImConnectionState.Disconnected
                _events.emit(ImEvent.SocketClosed)
                scheduleReconnectIfNeeded()
            }
        }
    }

    private suspend fun scheduleReconnectIfNeeded() {
        val uid = authUserId ?: return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(2500)
            if (authUserId == uid) {
                _connectionState.value = ImConnectionState.Connecting
                withContext(Dispatchers.IO) {
                    synchronized(this@DefaultImClient) {
                        webSocket?.cancel()
                        val request = Request.Builder().url(wsUrl).build()
                        webSocket = client.newWebSocket(request, listener(uid))
                    }
                }
            }
        }
    }

    private suspend fun dispatchMessage(text: String) {
        val obj = try {
            JSONObject(text)
        } catch (_: Exception) {
            _events.emit(ImEvent.Error("无效 JSON"))
            return
        }
        when (obj.optString("type")) {
            "AUTH_OK" -> {
                val uid = obj.optLong("userId", -1L)
                if (uid > 0) _events.emit(ImEvent.AuthOk(uid))
            }
            "PRESENCE" -> {
                val uid = obj.optLong("userId", -1L)
                if (uid > 0) {
                    _events.emit(ImEvent.Presence(uid, obj.optBoolean("online", false)))
                }
            }
            "ERROR" -> {
                val msg = obj.optString("message", "错误")
                synchronized(this@DefaultImClient) {
                    val w = groupCreateAwait
                    if (w != null) {
                        groupCreateAwait = null
                        w.completeExceptionally(IllegalStateException(msg))
                    }
                }
                _events.emit(ImEvent.Error(msg))
            }
            "CONVERSATIONS_RESULT" -> parseConversations(obj)
            "PRIVATE_MESSAGE" -> parsePrivate(obj)?.let { _events.emit(ImEvent.PrivateMessage(it)) }
            "GROUP_MESSAGE" -> parseGroup(obj)?.let { _events.emit(ImEvent.GroupMessage(it)) }
            "HISTORY_RESULT" -> parseHistory(obj)
            "USER_INFO_RESULT" -> {
                val uid = obj.optLong("userId", -1L)
                if (uid > 0) {
                    val online = when {
                        obj.isNull("online") -> null
                        else -> obj.optBoolean("online", false)
                    }
                    _events.emit(
                        ImEvent.UserInfoResult(
                            userId = uid,
                            username = obj.optString("username").takeIf { it.isNotEmpty() },
                            nickname = obj.optString("nickname").takeIf { it.isNotEmpty() },
                            mobile = obj.optString("mobile").takeIf { it.isNotEmpty() },
                            online = online
                        )
                    )
                }
            }
            "GROUP_CREATED" -> {
                val gid = obj.optLong("groupId", -1L)
                if (gid > 0) {
                    val ev = ImEvent.GroupCreated(gid, obj.optString("name"))
                    synchronized(this@DefaultImClient) {
                        val w = groupCreateAwait
                        if (w != null) {
                            groupCreateAwait = null
                            w.complete(ev)
                        }
                    }
                    _events.emit(ev)
                }
            }
            "GROUP_INFO_RESULT" -> parseGroupInfo(obj)?.let { _events.emit(it) }
        }
    }

    private fun parseGroupInfo(obj: JSONObject): ImEvent.GroupInfoResult? {
        val gid = obj.optLong("groupId", -1L)
        if (gid <= 0) return null
        val arr = obj.optJSONArray("members") ?: JSONArray()
        val members = buildList {
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                val uid = row.optLong("userId", -1L)
                if (uid <= 0) continue
                add(GroupMemberRow(uid, row.optString("role", "MEMBER")))
            }
        }
        return ImEvent.GroupInfoResult(
            groupId = gid,
            name = obj.optString("name"),
            ownerUserId = obj.optLong("ownerUserId", -1L),
            members = members
        )
    }

    private suspend fun parseConversations(obj: JSONObject) {
        val arr = obj.optJSONArray("items") ?: JSONArray()
        val list = buildList {
            for (i in 0 until arr.length()) {
                val it = arr.optJSONObject(i) ?: continue
                val convType = it.optString("convType")
                val peer = if (it.has("peerUserId")) it.optLong("peerUserId") else null
                val gid = if (it.has("groupId")) it.optLong("groupId") else null
                val last = it.optJSONObject("lastMessage") ?: continue
                val msg = parseMessageRow(last) ?: continue
                val unread = if (it.has("unreadCount")) it.optInt("unreadCount", 0) else 0
                val gname = when {
                    !it.has("groupName") || it.isNull("groupName") -> null
                    else -> it.optString("groupName").takeIf { s -> s.isNotEmpty() }
                }
                add(ConversationItem(convType, peer, gid, msg, unreadCount = unread, groupName = gname))
            }
        }
        _events.emit(ImEvent.Conversations(list))
    }

    private suspend fun parseHistory(obj: JSONObject) {
        val arr = obj.optJSONArray("messages") ?: JSONArray()
        val list = buildList {
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                parseMessageRow(row)?.let { add(it) }
            }
        }
        _events.emit(ImEvent.HistoryResult(list))
    }

    private fun parseMessageRow(o: JSONObject): ChatMessage? {
        val id = o.optLong("msgId", -1L)
        if (id < 0) return null
        return ChatMessage(
            msgId = id,
            msgType = o.optString("msgType", "P2P"),
            fromUserId = o.optLong("fromUserId"),
            toUserId = if (o.has("toUserId") && !o.isNull("toUserId")) o.optLong("toUserId") else null,
            groupId = if (o.has("groupId") && !o.isNull("groupId")) o.optLong("groupId") else null,
            body = o.optString("body"),
            createdAt = o.optString("createdAt").takeIf { it.isNotEmpty() }
        )
    }

    private fun parsePrivate(o: JSONObject): ChatMessage? {
        val id = o.optLong("msgId", -1L)
        if (id < 0) return null
        return ChatMessage(
            msgId = id,
            msgType = "P2P",
            fromUserId = o.optLong("fromUserId"),
            toUserId = o.optLong("toUserId"),
            groupId = null,
            body = o.optString("body"),
            createdAt = o.optString("createdAt").takeIf { it.isNotEmpty() }
        )
    }

    private fun parseGroup(o: JSONObject): ChatMessage? {
        val id = o.optLong("msgId", -1L)
        if (id < 0) return null
        return ChatMessage(
            msgId = id,
            msgType = "GROUP",
            fromUserId = o.optLong("fromUserId"),
            toUserId = null,
            groupId = o.optLong("groupId"),
            body = o.optString("body"),
            createdAt = o.optString("createdAt").takeIf { it.isNotEmpty() }
        )
    }
}
