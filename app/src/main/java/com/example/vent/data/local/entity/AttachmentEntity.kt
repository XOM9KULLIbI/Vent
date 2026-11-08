package com.example.vent.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["messageId"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId")]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val fileId: Long,
    val filename: String?,
    val fileSize: Long?,
    val contentType: String?,
    val downloadUrl: String?
)