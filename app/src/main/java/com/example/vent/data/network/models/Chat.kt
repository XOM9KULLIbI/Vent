package com.example.vent.data.network.models

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
@Keep
@JsonClass(generateAdapter = true)
data class Interlocutor(
    @Json(name = "user_id") val userId: Long? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "avatar_file_url") val avatarFileUrl: String? = null
)
@Keep
@JsonClass(generateAdapter = true)
data class Chat(
    @Json(name = "chat_id") val id: Long? = null,
    @Json(name = "chat_name") val chatName: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "interlocutor") val interlocutor: Interlocutor? = null,
    @Json(name = "last_message") val lastMessage: Message? = null,
    @Json(name = "unread_count") val unreadCount: Int? = 0
) {
    // Вспомогательное свойство для получения названия чата в UI
    val displayName: String
        get() = chatName
            ?: interlocutor?.nickname
            ?: interlocutor?.username
            ?: "Чат без названия"
}