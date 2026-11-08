package com.example.vent.data.network.models

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class Attachment(
    @Json(name = "file_id") val fileId: Long,
    @Json(name = "filename") val filename: String? = null,
    @Json(name = "file_size") val fileSize: Long? = null,
    @Json(name = "content_type") val contentType: String? = null,
    @Json(name = "download_url") val downloadUrl: String? = null   // ← добавили
)

@Keep
@JsonClass(generateAdapter = true)
data class Message(
    @Json(name = "message_id") val messageId: Long,
    @Json(name = "sender_id") val senderId: Long,
    @Json(name = "chat_id") val chatId: Long? = null,
    @Json(name = "message") val text: String?,
    @Json(name = "timestamp") val timestamp: String?,
    @Json(name = "read") val read: Boolean? = false,
    @Json(name = "read_at") val readAt: String? = null,
    @Json(name = "edited") val edited: Boolean? = false,
    @Json(name = "edited_at") val editedAt: String? = null,
    @Json(name = "attachments") val attachments: List<Attachment>? = emptyList()
)