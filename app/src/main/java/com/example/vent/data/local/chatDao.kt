package com.example.vent.data.local

import androidx.room.*
import com.example.vent.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun observeChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE chatId = :chatId LIMIT 1")
    fun observeChat(chatId: Long): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE chatId = :chatId LIMIT 1")
    suspend fun getChat(chatId: Long): ChatEntity?

    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    suspend fun getChats(): List<ChatEntity>

    @Upsert
    suspend fun upsertAll(chats: List<ChatEntity>)

    @Query("SELECT COUNT(*) > 0 FROM chats WHERE chatId = :chatId")
    suspend fun chatExists(chatId: Long): Boolean

    @Query("UPDATE chats SET updatedAt = :updatedAt, lastMessageId = :lastMessageId, lastMessageText = :lastMessageText WHERE chatId = :chatId")
    suspend fun updateLastMessage(
        chatId: Long,
        lastMessageId: Long,
        lastMessageText: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM chats")
    suspend fun clear()
}