package com.undersky.androidim.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import kotlin.jvm.Volatile

class ImSocketManager(
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
    private val wsUrl: String
) {

    companion object {
        fun buildWsUrl(httpBase: String, wsPath: String): String {
            val base = httpBase.trimEnd('/')
            val path = if (wsPath.startsWith("/")) wsPath else "/$wsPath"
            return when {
                base.startsWith("https://") -> "wss://" + base.removePrefix("https://") + path
                base.startsWith("http://") -> "ws://" + base.removePrefix("http://") + path
                else -> "ws://$base$path"
            }
        }
    }

    sealed class Event {
        data object SocketOpen : Event()
        data object SocketClosed : Event()
        data class AuthOk(val userId: Long) : Event()
        data class Error(val message: String) : Event()
        data class Conversations(val items: List<ConversationItem>) : Event()
        data class PrivateMessage(val message: ChatMessage) : Event()
        data class GroupMessage(val message: ChatMessage) : Event()
        data class HistoryResult(val messages: List<ChatMessage>) : Event()
        data class UserInfoResult(val userId: Long, val username: String?, val mobile: String?) : Event()
    }

    private val _events = MutableSharedFlow<Event>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var authUserId: Long? = null

    private var reconnectJob: Job? = null

    fun connect(userId: Long) {
        authUserId = userId
        reconnectJob?.cancel()
        scope.launch(Dispatchers.IO) {
            synchronized(this@ImSocketManager) {
                webSocket?.cancel()
                val request = Request.Builder().url(wsUrl).build()
                webSocket = client.newWebSocket(request, listener(userId))
            }
        }
    }

    fun disconnect(clearUser: Boolean = true) {
        reconnectJob?.cancel()
        reconnectJob = null
        if (clearUser) {
            authUserId = null
        }
        synchronized(this) {
            webSocket?.close(1000, "logout")
            webSocket = null
        }
    }

    fun sendRaw(json: String) {
        webSocket?.send(json)
    }

    fun sendAuth(userId: Long) {
        sendRaw("""{"type":"AUTH","userId":$userId}""")
    }

    fun requestConversations() {
        sendRaw("""{"type":"CONVERSATIONS"}""")
    }

    fun requestUserInfo(userId: Long) {
        sendRaw("""{"type":"USER_INFO","userId":$userId}""")
    }

    fun requestHistoryP2P(peerUserId: Long, beforeId: Long? = null, limit: Int = 50) {
        val before = beforeId?.let { ",\"beforeId\":$it" } ?: ""
        sendRaw("""{"type":"HISTORY","mode":"P2P","peerUserId":$peerUserId,"limit":$limit$before}""")
    }

    fun requestHistoryGroup(groupId: Long, beforeId: Long? = null, limit: Int = 50) {
        val before = beforeId?.let { ",\"beforeId\":$it" } ?: ""
        sendRaw("""{"type":"HISTORY","mode":"GROUP","groupId":$groupId,"limit":$limit$before}""")
    }

    fun sendPrivate(toUserId: Long, body: String) {
        val o = JSONObject()
        o.put("type", "PRIVATE_SEND")
        o.put("toUserId", toUserId)
        o.put("body", body)
        sendRaw(o.toString())
    }

    fun sendGroup(groupId: Long, body: String) {
        val o = JSONObject()
        o.put("type", "GROUP_SEND")
        o.put("groupId", groupId)
        o.put("body", body)
        sendRaw(o.toString())
    }

    private fun listener(expectedUserId: Long) = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            scope.launch { _events.emit(Event.SocketOpen) }
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
                _events.emit(Event.SocketClosed)
                scheduleReconnectIfNeeded()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            scope.launch {
                _events.emit(Event.Error(t.message ?: "连接失败"))
                _events.emit(Event.SocketClosed)
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
                withContext(Dispatchers.IO) {
                    synchronized(this@ImSocketManager) {
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
            _events.emit(Event.Error("无效 JSON"))
            return
        }
        when (obj.optString("type")) {
            "AUTH_OK" -> {
                val uid = obj.optLong("userId", -1L)
                if (uid > 0) _events.emit(Event.AuthOk(uid))
            }
            "ERROR" -> _events.emit(Event.Error(obj.optString("message", "错误")))
            "CONVERSATIONS_RESULT" -> parseConversations(obj)
            "PRIVATE_MESSAGE" -> parsePrivate(obj)?.let { _events.emit(Event.PrivateMessage(it)) }
            "GROUP_MESSAGE" -> parseGroup(obj)?.let { _events.emit(Event.GroupMessage(it)) }
            "HISTORY_RESULT" -> parseHistory(obj)
            "USER_INFO_RESULT" -> {
                val uid = obj.optLong("userId", -1L)
                if (uid > 0) {
                    _events.emit(
                        Event.UserInfoResult(
                            userId = uid,
                            username = obj.optString("username").takeIf { it.isNotEmpty() },
                            mobile = obj.optString("mobile").takeIf { it.isNotEmpty() }
                        )
                    )
                }
            }
        }
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
                add(ConversationItem(convType, peer, gid, msg, unreadCount = unread))
            }
        }
        _events.emit(Event.Conversations(list))
    }

    private suspend fun parseHistory(obj: JSONObject) {
        val arr = obj.optJSONArray("messages") ?: JSONArray()
        val list = buildList {
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                parseMessageRow(row)?.let { add(it) }
            }
        }
        _events.emit(Event.HistoryResult(list))
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
