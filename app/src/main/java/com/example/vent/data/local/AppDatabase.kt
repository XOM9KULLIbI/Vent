package com.example.vent.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.vent.data.local.entity.AttachmentEntity
import com.example.vent.data.local.entity.ChatEntity
import com.example.vent.data.local.entity.MessageEntity

@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        AttachmentEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vent.db"
                )
                    .fallbackToDestructiveMigration() // на dev-этапе ок
                    .build()
                    .also { INSTANCE = it }
            }
    }
}