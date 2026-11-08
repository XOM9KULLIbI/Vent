package com.example.vent.data.network

import android.util.Log
import com.example.vent.data.network.models.*
import com.example.vent.data.repository.ChatRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class VentWebSocketManager(
    private val tokenManager: TokenManager,
    private val chatRepository: ChatRepository,
    private val networkMonitor: com.example.vent.util.NetworkMonitor? = null,
    private val onUnauthorized: (() -> Unit)? = null
) {
    companion object {
        private const val TAG = "VentWebSocket"
        private const val WS_URL = "wss://messenger-3yfw.onrender.com/ws/"
        private const val CLOSE_CODE_AUTH_ERROR = 1008
    }

    private var webSocket: WebSocket? = null
    private val isConnecting = AtomicBoolean(false)
    private var isManuallyDisconnected = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconnectJob: Job? = null
    private var pingJob: Job? = null
    private var reconnectAttempts = 0

    // Статусы онлайн пользователей: userId -> Boolean
    private val _userOnlineStatus = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val userOnlineStatus: StateFlow<Map<Long, Boolean>> = _userOnlineStatus.asStateFlow()

    // Статус подключения WebSocket
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val envelopeAdapter = moshi.adapter(WsEnvelope::class.java)
    private val chatMessageAdapter = moshi.adapter(WsChatMessageEvent::class.java)
    private val markedReadAdapter = moshi.adapter(WsChatMarkedReadEvent::class.java)
    private val messageEditedAdapter = moshi.adapter(WsMessageEditedEvent::class.java)
    private val messageDeletedAdapter = moshi.adapter(WsMessageDeletedEvent::class.java)
    private val userStatusAdapter = moshi.adapter(WsUserStatusEvent::class.java)
    private val errorAdapter = moshi.adapter(WsErrorEvent::class.java)
    private val ackAdapter = moshi.adapter(WsAckEvent::class.java)
    private val pingAdapter = moshi.adapter(WsPingEvent::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(25, TimeUnit.SECONDS) // Держит TCP-туннель активным, защищая от таймаута Render
        .build()

    init {
        scope.launch {
            networkMonitor?.isOnline?.collect { online ->
                if (online && !isManuallyDisconnected && webSocket == null) {
                    Log.i(TAG, "Network restored, auto-connecting WebSocket...")
                    connect()
                }
            }
        }
    }

    @Synchronized
    fun connect() {
        isManuallyDisconnected = false
        val token = tokenManager.getAccessToken()
        if (token.isNullOrEmpty()) {
            Log.w(TAG, "Cannot connect: access token is missing")
            return
        }

        if (webSocket != null || isConnecting.get()) {
            Log.d(TAG, "WS already connected or in connection process")
            return
        }

        isConnecting.set(true)
        reconnectJob?.cancel()

        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            Log.i(TAG, "WebSocket connected")
            isConnecting.set(false)
            _isConnected.value = true
            reconnectAttempts = 0
            startAppLevelPing()
        }

        override fun onMessage(ws: WebSocket, text: String) {
            handleIncomingMessage(text)
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: code=$code, reason=$reason")
            ws.close(code, reason)
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            Log.w(TAG, "WebSocket closed: code=$code, reason=$reason")
            handleDisconnection(code)
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            val code = response?.code ?: -1
            Log.e(TAG, "WebSocket failure: code=$code, error=${t.message}")
            handleDisconnection(code)
        }
    }

    private fun handleIncomingMessage(jsonText: String) {
        scope.launch {
            try {
                val envelope = envelopeAdapter.fromJson(jsonText) ?: return@launch
                when (envelope.type) {
                    "chat.message" -> {
                        val event = chatMessageAdapter.fromJson(jsonText) ?: return@launch
                        val payload = event.payload
                        val msg = payload.message
                        val chatId = msg.chatId ?: return@launch

                        Log.d(TAG, "Received message: id=${msg.messageId} in chat=$chatId")
                        // Сохраняем в Room (Room Flow уведомит ChatScreen и ChatListScreen)
                        chatRepository.saveIncomingMessage(chatId, msg)

                        // Подтверждаем получение серверу (ACK)
                        sendAck(listOf(payload.deliveryId))
                    }

                    "chat.marked_read" -> {
                        val event = markedReadAdapter.fromJson(jsonText) ?: return@launch
                        val payload = event.payload
                        Log.d(TAG, "Received chat.marked_read: chat=${payload.chatId}, lastMessageId=${payload.lastReadMessageId}")
                        chatRepository.markMessagesAsRead(payload.chatId, payload.lastReadMessageId)
                    }

                    "message.edited" -> {
                        val event = messageEditedAdapter.fromJson(jsonText) ?: return@launch
                        val msg = event.payload.editedMessage
                        val chatId = msg.chatId ?: return@launch
                        Log.d(TAG, "Received message.edited: id=${msg.messageId}")
                        chatRepository.saveIncomingMessage(chatId, msg)
                    }

                    "message.deleted" -> {
                        val event = messageDeletedAdapter.fromJson(jsonText) ?: return@launch
                        val payload = event.payload
                        Log.d(TAG, "Received message.deleted: chat=${payload.chatId}, count=${payload.deletedMessageIds.size}")
                        chatRepository.deleteMessages(payload.chatId, payload.deletedMessageIds)
                    }

                    "user.status" -> {
                        val event = userStatusAdapter.fromJson(jsonText) ?: return@launch
                        val payload = event.payload
                        Log.d(TAG, "Received user.status: user=${payload.userId}, online=${payload.online}")
                        _userOnlineStatus.update { current ->
                            current + (payload.userId to payload.online)
                        }
                    }

                    "pong" -> {
                        Log.v(TAG, "Pong received")
                    }

                    "chat.message.ack.result" -> {
                        Log.v(TAG, "ACK confirmed by server")
                    }

                    "error" -> {
                        val event = errorAdapter.fromJson(jsonText)
                        Log.e(TAG, "WS error: ${event?.payload?.code} - ${event?.payload?.detail}")
                    }

                    else -> {
                        Log.d(TAG, "Unhandled event: ${envelope.type}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse WS payload: ${e.message}", e)
            }
        }
    }

    private fun sendAck(deliveryIds: List<Long>) {
        if (deliveryIds.isEmpty()) return
        try {
            val ackJson = ackAdapter.toJson(WsAckEvent(payload = WsAckPayload(deliveryIds)))
            sendText(ackJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send ACK: ${e.message}")
        }
    }

    fun sendText(text: String): Boolean {
        return webSocket?.send(text) ?: false
    }

    private fun startAppLevelPing() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                delay(30_000)
                try {
                    val pingJson = pingAdapter.toJson(WsPingEvent())
                    sendText(pingJson)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send app ping: ${e.message}")
                }
            }
        }
    }

    private fun handleDisconnection(code: Int) {
        isConnecting.set(false)
        _isConnected.value = false
        pingJob?.cancel()
        webSocket = null

        if (isManuallyDisconnected) {
            Log.d(TAG, "Manual disconnect, skipping reconnect")
            return
        }

        if (code == CLOSE_CODE_AUTH_ERROR || code == 401) {
            Log.w(TAG, "WS auth error ($code), refreshing token...")
            attemptTokenRefreshAndReconnect()
            return
        }

        scheduleReconnect()
    }

    private fun attemptTokenRefreshAndReconnect() {
        scope.launch {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                Log.e(TAG, "No refresh token, notifying unauthorized")
                tokenManager.clearTokens()
                onUnauthorized?.invoke()
                return@launch
            }

            try {
                val response = RetrofitClient.authApi.refreshToken(
                    request = RefreshTokenRequest(refreshToken = refreshToken)
                )
                if (response.isSuccessful && response.body() != null) {
                    val newTokens = response.body()!!
                    tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)
                    Log.i(TAG, "Token refreshed, reconnecting...")
                    connect()
                } else {
                    Log.e(TAG, "Token refresh failed: ${response.code()}")
                    tokenManager.clearTokens()
                    onUnauthorized?.invoke()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during token refresh: ${e.message}")
                tokenManager.clearTokens()
                onUnauthorized?.invoke()
            }
        }
    }

    private fun scheduleReconnect() {
        if (isManuallyDisconnected) return
        reconnectAttempts++
        val delayMillis = (2_000L * (1 shl (reconnectAttempts.coerceAtMost(4) - 1)))
            .coerceAtMost(30_000L)

        Log.d(TAG, "Scheduling reconnect attempt #$reconnectAttempts in ${delayMillis}ms")
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delayMillis)
            connect()
        }
    }

    @Synchronized
    fun disconnect() {
        isManuallyDisconnected = true
        reconnectJob?.cancel()
        pingJob?.cancel()
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        isConnecting.set(false)
        _isConnected.value = false
    }
}