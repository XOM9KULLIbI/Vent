package com.example.vent.ui.chat

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vent.data.network.RetrofitClient
import com.example.vent.data.network.models.Chat
import com.example.vent.data.network.models.Message
import com.example.vent.data.network.models.SendMessageRequest
import com.example.vent.util.FileUtils
import com.example.vent.util.isNetworkException
import kotlinx.coroutines.launch

class ChatViewModel(private val chatId: Long) : ViewModel() {

    var messages by mutableStateOf<List<Message>>(emptyList())
        private set
    var currentChat by mutableStateOf<Chat?>(null)
        private set
    var isLoading by mutableStateOf(false); private set
    var isSending by mutableStateOf(false); private set
    var isUploadingFile by mutableStateOf(false); private set
    var isOffline by mutableStateOf(false); private set
    var errorMessage by mutableStateOf<String?>(null); private set

    val attachedFileIds = mutableStateListOf<Long>()
    private var canLoadMore = true

    init {
        // 1. Подписка на БД — UI обновляется автоматически из кэша
        viewModelScope.launch {
            RetrofitClient.chatRepository.observeMessages(chatId).collect {
                messages = it
            }
        }
        // 2. Подписка на информацию о текущем чате (для заголовка и ID собеседника)
        viewModelScope.launch {
            RetrofitClient.chatRepository.observeChat(chatId).collect {
                currentChat = it
            }
        }
        // 3. Отслеживание сети: при подключении пробуем подгрузить новые сообщения
        viewModelScope.launch {
            RetrofitClient.networkMonitor.isOnline.collect { online ->
                isOffline = !online
                if (online) {
                    errorMessage = null
                    loadMessages()
                }
            }
        }
    }

    fun isMyMessage(message: Message): Boolean {
        val myId = RetrofitClient.getTokenManager().getMyUserId()
        if (myId != null && myId > 0L) {
            return message.senderId == myId
        }
        val interlocutorId = currentChat?.interlocutor?.userId
        if (interlocutorId != null && interlocutorId > 0L) {
            return message.senderId != interlocutorId
        }
        return false
    }

    fun loadMessages() {
        viewModelScope.launch {
            isLoading = messages.isEmpty()
            RetrofitClient.chatRepository.loadLatestMessages(chatId)
                .onSuccess {
                    canLoadMore = !it
                    errorMessage = null
                    isOffline = false
                }
                .onFailure { error ->
                    if (isNetworkException(error) || !RetrofitClient.networkMonitor.isOnlineNow()) {
                        isOffline = true
                        errorMessage = "Нет подключения к интернету"
                    } else {
                        errorMessage = error.localizedMessage ?: "Ошибка сети"
                    }
                }
            isLoading = false
        }
    }

    /** Вызывать при скролле вверх. */
    fun loadOlderMessages() {
        if (!canLoadMore) return
        viewModelScope.launch {
            RetrofitClient.chatRepository.loadOlderMessages(chatId)
                .onSuccess { canLoadMore = !it }
        }
    }

    fun uploadAttachment(context: Context, uri: Uri) {
        val bodyPart = FileUtils.prepareFilePart(context, "file", uri) ?: return
        viewModelScope.launch {
            isUploadingFile = true
            try {
                val response = RetrofitClient.chatApi.uploadFile(chatId, bodyPart)
                if (response.isSuccessful) {
                    response.body()?.let { attachedFileIds.add(it.fileId) }
                } else errorMessage = "Ошибка загрузки файла: ${response.code()}"
            } catch (e: Exception) {
                errorMessage = "Ошибка сети: ${e.localizedMessage}"
            } finally {
                isUploadingFile = false
            }
        }
    }

    fun removeAttachment(fileId: Long) = attachedFileIds.remove(fileId)

    fun sendMessage(text: String) {
        if ((text.isBlank() && attachedFileIds.isEmpty()) || isSending) return

        viewModelScope.launch {
            isSending = true
            try {
                val response = RetrofitClient.chatApi.sendMessage(
                    chatId = chatId,
                    request = SendMessageRequest(
                        text = text.trim(),
                        attachmentsId = attachedFileIds.toList()
                    )
                )
                if (response.isSuccessful) {
                    response.body()?.let { newMessage ->
                        // Сеть вернула сообщение — кладём в БД, Flow сам обновит UI
                        RetrofitClient.chatRepository.saveIncomingMessage(chatId, newMessage)
                        attachedFileIds.clear()
                    }
                } else errorMessage = "Не удалось отправить: ${response.code()}"
            } catch (e: Exception) {
                errorMessage = "Ошибка отправки: ${e.localizedMessage}"
            } finally {
                isSending = false
            }
        }
    }
}
class ChatViewModelFactory(private val chatId: Long) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}