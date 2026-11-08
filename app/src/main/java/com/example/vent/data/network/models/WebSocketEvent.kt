package com.example.vent.data.network.models

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Базовый конверт WS-сообщения:
 * { "type": "event.name", "payload": { ... } }
 */
@Keep
@JsonClass(generateAdapter = true)
data class WsEnvelope(
    @Json(name = "type") val type: String
)

// ---------- Server -> Client ----------

// 1. chat.message (новое сообщение)
@Keep
@JsonClass(generateAdapter = true)
data class WsChatMessagePayload(
    @Json(name = "delivery_id") val deliveryId: Long,
    @Json(name = "message") val message: Message
)

@Keep
@JsonClass(generateAdapter = true)
data class WsChatMessageEvent(
    @Json(name = "type") val type: String = "chat.message",
    @Json(name = "payload") val payload: WsChatMessagePayload
)

// 2. chat.marked_read (сообщение прочитано собеседником)
@Keep
@JsonClass(generateAdapter = true)
data class WsChatMarkedReadPayload(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "chat_id") val chatId: Long,
    @Json(name = "last_read_message_id") val lastReadMessageId: Long
)

@Keep
@JsonClass(generateAdapter = true)
data class WsChatMarkedReadEvent(
    @Json(name = "type") val type: String = "chat.marked_read",
    @Json(name = "payload") val payload: WsChatMarkedReadPayload
)

// 3. message.edited (сообщение отредактировано)
@Keep
@JsonClass(generateAdapter = true)
data class WsMessageEditedPayload(
    @Json(name = "edited_message") val editedMessage: Message
)

@Keep
@JsonClass(generateAdapter = true)
data class WsMessageEditedEvent(
    @Json(name = "type") val type: String = "message.edited",
    @Json(name = "payload") val payload: WsMessageEditedPayload
)

// 4. message.deleted (сообщения удалены)
@Keep
@JsonClass(generateAdapter = true)
data class WsMessageDeletedPayload(
    @Json(name = "chat_id") val chatId: Long,
    @Json(name = "deleted_message_ids") val deletedMessageIds: List<Long>
)

@Keep
@JsonClass(generateAdapter = true)
data class WsMessageDeletedEvent(
    @Json(name = "type") val type: String = "message.deleted",
    @Json(name = "payload") val payload: WsMessageDeletedPayload
)

// 5. user.status (онлайн/офлайн статус контакта)
@Keep
@JsonClass(generateAdapter = true)
data class WsUserStatusPayload(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "online") val online: Boolean
)

@Keep
@JsonClass(generateAdapter = true)
data class WsUserStatusEvent(
    @Json(name = "type") val type: String = "user.status",
    @Json(name = "payload") val payload: WsUserStatusPayload
)

// 6. error (ошибка сервера)
@Keep
@JsonClass(generateAdapter = true)
data class WsErrorPayload(
    @Json(name = "code") val code: String,
    @Json(name = "detail") val detail: String
)

@Keep
@JsonClass(generateAdapter = true)
data class WsErrorEvent(
    @Json(name = "type") val type: String = "error",
    @Json(name = "payload") val payload: WsErrorPayload
)

// ---------- Client -> Server ----------

// 1. chat.message.ack (подтверждение доставки сообщения)
@Keep
@JsonClass(generateAdapter = true)
data class WsAckPayload(
    @Json(name = "delivery_ids") val deliveryIds: List<Long>
)

@Keep
@JsonClass(generateAdapter = true)
data class WsAckEvent(
    @Json(name = "type") val type: String = "chat.message.ack",
    @Json(name = "payload") val payload: WsAckPayload
)

// 2. ping (heartbeat)
@Keep
@JsonClass(generateAdapter = true)
data class WsPingEvent(
    @Json(name = "type") val type: String = "ping",
    @Json(name = "payload") val payload: Map<String, String> = emptyMap()
)