package com.example.vent.data.network.models

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @Json(name = "message") val text: String,
    @Json(name = "attachments_id") val attachmentsId: List<Long> = emptyList(),
    @Json(name = "client_message_id") val clientMessageId: String? = null
)