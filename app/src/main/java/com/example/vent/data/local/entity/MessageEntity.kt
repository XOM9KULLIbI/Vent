package com.example.vent.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["chatId"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatId"), Index("timestamp")]
)
data class MessageEntity(
    @PrimaryKey val messageId: Long,
    val chatId: Long,
    val senderId: Long,
    val text: String?,
    val timestamp: String?,
    val read: Boolean,
    val readAt: String?,
    val edited: Boolean,
    val editedAt: String?,
    // локальный флаг: сообщение ещё не подтверждено сервером
    val pending: Boolean = false
)