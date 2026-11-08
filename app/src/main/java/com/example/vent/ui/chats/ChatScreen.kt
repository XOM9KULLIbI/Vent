package com.example.vent.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vent.data.network.RetrofitClient
import com.example.vent.data.network.models.Message
import com.example.vent.ui.common.OfflineBanner
import com.example.vent.util.DateUtils

/**
 * Элементы списка чата: либо сообщение, либо сепаратор с датой.
 */
sealed interface ChatUiItem {
    data class MessageItem(val message: Message) : ChatUiItem
    data class DateSeparator(val label: String, val dateKey: String) : ChatUiItem
}

/**
 * Данные для полноэкранного просмотра.
 * downloadUrl может быть null (тогда RemoteImage резолвит через fileId).
 */
data class FullscreenImage(
    val downloadUrl: String?,
    val fileId: Long?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: Long,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(chatId))
) {
    var inputText by remember { mutableStateOf("") }
    var fullscreenImage by remember { mutableStateOf<FullscreenImage?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadAttachment(context, it) }
    }

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadOlderMessages()
    }

    // Формируем список с сепараторами дат между сообщениями разных дней
    val chatUiItems = remember(viewModel.messages) {
        val items = ArrayList<ChatUiItem>(viewModel.messages.size + 10)
        for (i in viewModel.messages.indices) {
            val currentMessage = viewModel.messages[i]
            items.add(ChatUiItem.MessageItem(currentMessage))

            val currentDate = DateUtils.extractLocalDate(currentMessage.timestamp)
            val nextMessage = viewModel.messages.getOrNull(i + 1)
            val nextDate = nextMessage?.let { DateUtils.extractLocalDate(it.timestamp) }

            // Если текущее сообщение имеет дату и следующее (более старое) сообщение
            // относится к другому дню или отсутствует (конец истории)
            if (currentDate != null && currentDate != nextDate) {
                val label = DateUtils.formatDateSeparator(currentDate)
                items.add(ChatUiItem.DateSeparator(label = label, dateKey = currentDate.toString()))
            }
        }
        items
    }

    Scaffold(
        topBar = {
            val partnerName = viewModel.currentChat?.displayName ?: "Чат"
            val interlocutorId = viewModel.currentChat?.interlocutor?.userId
            val onlineMap by RetrofitClient.webSocketManager.userOnlineStatus.collectAsState()
            val isPartnerOnline = interlocutorId?.let { onlineMap[it] } == true

            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = partnerName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isPartnerOnline) {
                            Text(
                                text = "в сети",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSendClick = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                onAttachClick = { filePickerLauncher.launch("*/*") },
                attachedIds = viewModel.attachedFileIds,
                onRemoveAttachment = { viewModel.removeAttachment(it) },
                isSending = viewModel.isSending,
                isUploading = viewModel.isUploadingFile
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OfflineBanner(
                visible = viewModel.isOffline || (viewModel.errorMessage != null && viewModel.messages.isNotEmpty()),
                message = viewModel.errorMessage ?: "Нет подключения к интернету",
                onRetry = { viewModel.loadMessages() }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (viewModel.messages.isNotEmpty()) {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = chatUiItems,
                            key = { item ->
                                when (item) {
                                    is ChatUiItem.MessageItem -> item.message.messageId
                                    is ChatUiItem.DateSeparator -> "date_${item.dateKey}"
                                }
                            }
                        ) { item ->
                            when (item) {
                                is ChatUiItem.MessageItem -> {
                                    MessageItem(
                                        message = item.message,
                                        isMyMessage = viewModel.isMyMessage(item.message),
                                        onImageClick = { url, id ->
                                            fullscreenImage = FullscreenImage(url, id)
                                        }
                                    )
                                }
                                is ChatUiItem.DateSeparator -> {
                                    DateSeparatorItem(text = item.label)
                                }
                            }
                        }
                    }
                } else if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                        Button(onClick = { viewModel.loadMessages() }) {
                            Text("Повторить")
                        }
                    }
                } else {
                    Text(
                        text = "В этом чате пока нет сообщений",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                fullscreenImage?.let { img ->
                    FullscreenImageDialog(
                        downloadUrl = img.downloadUrl,
                        fileId = img.fileId,
                        onDismiss = { fullscreenImage = null }
                    )
                }
            }
        }
    }
}

/**
 * Элемент сепаратора даты между группами сообщений
 */
@Composable
fun DateSeparatorItem(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    isMyMessage: Boolean,
    onImageClick: (String?, Long?) -> Unit
) {
    val formattedTime = remember(message.timestamp) {
        DateUtils.formatTime(message.timestamp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 70.dp, max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMyMessage) 16.dp else 4.dp,
                        bottomEnd = if (isMyMessage) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isMyMessage) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            message.attachments?.forEach { attachment ->
                RemoteImage(
                    downloadUrl = attachment.downloadUrl,
                    fileId = attachment.fileId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    onClick = {
                        onImageClick(attachment.downloadUrl, attachment.fileId)
                    }
                )
            }

            if (!message.text.isNullOrBlank()) {
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    color = if (isMyMessage) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            if (formattedTime.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.edited == true) {
                        Text(
                            text = "изм. ",
                            fontSize = 11.sp,
                            color = if (isMyMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    }
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = if (isMyMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
    attachedIds: List<Long>,
    onRemoveAttachment: (Long) -> Unit,
    isSending: Boolean,
    isUploading: Boolean
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (attachedIds.isNotEmpty() || isUploading) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(attachedIds) { fileId ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            // Здесь URL ещё нет — файл только что загружен.
                            // RemoteImage сам сходит в getFileInfo(fileId).
                            RemoteImage(
                                downloadUrl = null,
                                fileId = fileId,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { onRemoveAttachment(fileId) },
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Удалить",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    if (isUploading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onAttachClick, enabled = !isUploading && !isSending) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Прикрепить файл")
                }

                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение...") },
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSendClick,
                    enabled = (text.isNotBlank() || attachedIds.isNotEmpty()) && !isSending && !isUploading
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить",
                            tint = if (text.isNotBlank() || attachedIds.isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenImageDialog(
    downloadUrl: String?,
    fileId: Long?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            RemoteImage(
                downloadUrl = downloadUrl,
                fileId = fileId,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = Color.White
                )
            }
        }
    }
}
