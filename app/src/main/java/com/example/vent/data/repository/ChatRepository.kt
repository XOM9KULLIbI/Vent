package com.example.vent.data.repository

import com.example.vent.data.local.AppDatabase
import com.example.vent.data.local.toDomain
import com.example.vent.data.local.toEntity
import com.example.vent.data.local.toAttachmentEntities
import com.example.vent.data.network.RetrofitClient
import com.example.vent.data.network.models.Chat
import com.example.vent.data.network.models.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(private val db: AppDatabase) {

    // ---------- ЧАТЫ ----------

    fun observeChats(): Flow<List<Chat>> =
        db.chatDao().observeChats().map { list -> list.map { it.toDomain() } }

    fun observeChat(chatId: Long): Flow<Chat?> =
        db.chatDao().observeChat(chatId).map { it?.toDomain() }

    suspend fun refreshChatsFromNetwork(): Result<Unit> = runCatching {
        val response = RetrofitClient.chatApi.getChats()
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        val chats = response.body().orEmpty()
        db.chatDao().upsertAll(chats.map { it.toEntity() })
    }

    // ---------- СООБЩЕНИЯ ----------

    fun observeMessages(chatId: Long): Flow<List<Message>> =
        db.messageDao().observeMessages(chatId).map { list ->
            list.map { it.toDomain() } // уже отсортированы DESC
        }

    /**
     * Загружает свежую страницу и кладёт в БД.
     * @return true если пришло меньше limit — значит, история закончилась.
     */
    suspend fun loadLatestMessages(chatId: Long, limit: Int = 30): Result<Boolean> = runCatching {
        val response = RetrofitClient.chatApi.getMessages(chatId, beforeMessageId = null, limit = limit)
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        val messages = response.body().orEmpty()
        saveMessagesToDb(chatId, messages)
        messages.size < limit
    }

    /**
     * Догрузка более старых сообщений (пагинация вверх).
     */
    suspend fun loadOlderMessages(chatId: Long, limit: Int = 30): Result<Boolean> = runCatching {
        val beforeId = db.messageDao().getMinMessageId(chatId) ?: return@runCatching true
        val response = RetrofitClient.chatApi.getMessages(chatId, beforeMessageId = beforeId, limit = limit)
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        val messages = response.body().orEmpty()
        saveMessagesToDb(chatId, messages)
        messages.size < limit
    }
    suspend fun clearAll() {
        db.messageDao().clearAll()
        db.chatDao().clear()
    }

    private suspend fun saveMessagesToDb(chatId: Long, messages: List<Message>) {
        if (messages.isEmpty()) return
        val entities = messages.map { it.toEntity(chatId) }
        val attachmentEntities = messages.flatMap { it.toAttachmentEntities() }
        db.messageDao().upsertMessages(entities)
        db.messageDao().upsertAttachments(attachmentEntities)
    }

    /** Для WebSocket: одиночное сообщение. */
    suspend fun saveIncomingMessage(chatId: Long, message: Message) {
        if (!db.chatDao().chatExists(chatId)) {
            val stubChat = com.example.vent.data.local.entity.ChatEntity(
                chatId = chatId,
                chatName = null,
                type = "private",
                interlocutorUserId = message.senderId,
                interlocutorUsername = null,
                interlocutorNickname = null,
                interlocutorAvatarUrl = null,
                lastMessageId = message.messageId,
                lastMessageText = message.text,
                unreadCount = 1,
                updatedAt = System.currentTimeMillis()
            )
            db.chatDao().upsertAll(listOf(stubChat))
            refreshChatsFromNetwork()
        } else {
            db.chatDao().updateLastMessage(chatId, message.messageId, message.text, System.currentTimeMillis())
        }
        saveMessagesToDb(chatId, listOf(message))
    }

    suspend fun deleteMessages(chatId: Long, messageIds: List<Long>) {
        if (messageIds.isNotEmpty()) {
            db.messageDao().deleteMessages(messageIds)
        }
    }

    suspend fun markMessagesAsRead(chatId: Long, lastReadMessageId: Long) {
        db.messageDao().markMessagesRead(chatId, lastReadMessageId)
    }

    /** Оптимистичная вставка pending-сообщения (не обязательно, но удобно). */
    suspend fun insertPending(chatId: Long, senderId: Long, text: String, tempId: Long) {
        db.messageDao().upsertMessages(
            listOf(
                com.example.vent.data.local.entity.MessageEntity(
                    messageId = tempId,
                    chatId = chatId,
                    senderId = senderId,
                    text = text,
                    timestamp = null,
                    read = false,
                    readAt = null,
                    edited = false,
                    editedAt = null,
                    pending = true
                )
            )
        )
    }

}