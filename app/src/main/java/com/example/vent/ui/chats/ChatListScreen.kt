package com.example.vent.ui.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vent.data.network.models.Chat
import com.example.vent.ui.common.OfflineBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = viewModel(),
    onChatClick: (chatId: Long) -> Unit = {} // Изменено: String -> Long
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Чаты") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OfflineBanner(
                visible = viewModel.isOffline || (viewModel.errorMessage != null && viewModel.chats.isNotEmpty()),
                message = viewModel.errorMessage ?: "Нет подключения к интернету",
                onRetry = { viewModel.loadChats() }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (viewModel.chats.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(viewModel.chats) { chat ->
                            ChatItemRow(
                                chat = chat,
                                onClick = {
                                    chat.id?.let { id -> onChatClick(id) }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                } else if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (viewModel.errorMessage != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = viewModel.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadChats() }) {
                            Text("Повторить")
                        }
                    }
                } else {
                    Text(
                        text = "Нет чатов",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatItemRow(
    chat: Chat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chat.lastMessage?.text ?: "Нет сообщений",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}