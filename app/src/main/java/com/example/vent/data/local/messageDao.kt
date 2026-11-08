package com.example.vent.data.local

import androidx.room.*
import com.example.vent.data.local.entity.AttachmentEntity
import com.example.vent.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY messageId DESC")
    fun observeMessages(chatId: Long): Flow<List<MessageWithAttachments>>

    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY messageId DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesPage(chatId: Long, limit: Int, offset: Int): List<MessageWithAttachments>

    @Upsert
    suspend fun upsertMessages(messages: List<MessageEntity>)

    @Upsert
    suspend fun upsertAttachments(attachments: List<AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessagesIgnore(messages: List<MessageEntity>)

    @Query("SELECT MIN(messageId) FROM messages WHERE chatId = :chatId")
    suspend fun getMinMessageId(chatId: Long): Long?

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearChat(chatId: Long)

    @Query("DELETE FROM messages")
    suspend fun clearAll()

    @Query("DELETE FROM messages WHERE messageId IN (:messageIds)")
    suspend fun deleteMessages(messageIds: List<Long>)

    @Query("UPDATE messages SET read = 1 WHERE chatId = :chatId AND messageId <= :lastReadMessageId AND read = 0")
    suspend fun markMessagesRead(chatId: Long, lastReadMessageId: Long)
}

data class MessageWithAttachments(
    @Embedded val message: MessageEntity,
    @Relation(
        parentColumn = "messageId",
        entityColumn = "messageId"
    )
    val attachments: List<AttachmentEntity>
)