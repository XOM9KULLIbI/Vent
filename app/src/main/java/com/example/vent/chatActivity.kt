package com.example.vent.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class ChatActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chatId = intent.getLongExtra(EXTRA_CHAT_ID, -1L)
        if (chatId == -1L) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                ChatScreen(
                    chatId = chatId,
                    onBackClick = { finish() }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_CHAT_ID = "extra_chat_id"

        fun start(context: Context, chatId: Long) {
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra(EXTRA_CHAT_ID, chatId)
            }
            context.startActivity(intent)
        }
    }
}