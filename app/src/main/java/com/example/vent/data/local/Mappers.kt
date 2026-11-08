package com.example.vent.data.local

import com.example.vent.data.local.entity.AttachmentEntity
import com.example.vent.data.local.entity.ChatEntity
import com.example.vent.data.local.entity.MessageEntity
import com.example.vent.data.network.models.Attachment
import com.example.vent.data.network.models.Chat
import com.example.vent.data.network.models.Interlocutor
import com.example.vent.data.network.models.Message

fun ChatEntity.toDomain(): Chat = Chat(
    id = chatId,
    chatName = chatName,
    type = type,
    interlocutor = if (interlocutorUserId != null) Interlocutor(
        userId = interlocutorUserId,
        username = interlocutorUsername,
        nickname = interlocutorNickname,
        avatarFileUrl = interlocutorAvatarUrl
    ) else null,
    lastMessage = if (lastMessageText != null || lastMessageId != null) Message(
        messageId = lastMessageId ?: 0L,
        senderId = 0L,
        chatId = chatId,
        text = lastMessageText,
        timestamp = null
    ) else null,
    unreadCount = unreadCount
)

fun Chat.toEntity(): ChatEntity = ChatEntity(
    chatId = id ?: 0L,
    chatName = chatName,
    type = type,
    interlocutorUserId = interlocutor?.userId,
    interlocutorUsername = interlocutor?.username,
    interlocutorNickname = interlocutor?.nickname,
    interlocutorAvatarUrl = interlocutor?.avatarFileUrl,
    lastMessageId = lastMessage?.messageId,
    lastMessageText = lastMessage?.text,
    unreadCount = unreadCount ?: 0
)

fun MessageWithAttachments.toDomain(): Message = Message(
    messageId = message.messageId,
    senderId = message.senderId,
    chatId = message.chatId,
    text = message.text,
    timestamp = message.timestamp,
    read = message.read,
    readAt = message.readAt,
    edited = message.edited,
    editedAt = message.editedAt,
    attachments = attachments.map { it.toDomain() }
)

fun Message.toEntity(chatId: Long): MessageEntity = MessageEntity(
    messageId = messageId,
    chatId = chatId,
    senderId = senderId,
    text = text,
    timestamp = timestamp,
    read = read == true,
    readAt = readAt,
    edited = edited == true,
    editedAt = editedAt
)

fun Message.toAttachmentEntities(): List<AttachmentEntity> =
    attachments.orEmpty().map { att ->
        AttachmentEntity(
            messageId = messageId,
            fileId = att.fileId,
            filename = att.filename,
            fileSize = att.fileSize,
            contentType = att.contentType,
            downloadUrl = att.downloadUrl
        )
    }

fun AttachmentEntity.toDomain(): Attachment = Attachment(
    fileId = fileId,
    filename = filename,
    fileSize = fileSize,
    contentType = contentType,
    downloadUrl = downloadUrl
)