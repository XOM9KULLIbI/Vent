package com.example.vent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: Long,
    val chatName: String?,
    val type: String?,
    val interlocutorUserId: Long?,
    val interlocutorUsername: String?,
    val interlocutorNickname: String?,
    val interlocutorAvatarUrl: String?,
    val lastMessageId: Long?,
    val lastMessageText: String? = null,
    val unreadCount: Int,
    val updatedAt: Long = System.currentTimeMillis()
)