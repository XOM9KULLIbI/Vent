package com.example.vent.ui.chats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vent.data.network.RetrofitClient
import com.example.vent.data.network.models.Chat
import com.example.vent.util.isNetworkException
import kotlinx.coroutines.launch

class ChatListViewModel : ViewModel() {

    var chats by mutableStateOf<List<Chat>>(emptyList())
        private set
    var isLoading by mutableStateOf(false); private set
    var isOffline by mutableStateOf(false); private set
    var errorMessage by mutableStateOf<String?>(null); private set

    init {
        // 1. Сразу подписываемся на БД — UI мгновенно получает кэш
        viewModelScope.launch {
            RetrofitClient.chatRepository.observeChats().collect {
                chats = it
            }
        }
        // 2. Отслеживание сети: при подключении обновляем чаты
        viewModelScope.launch {
            RetrofitClient.networkMonitor.isOnline.collect { online ->
                isOffline = !online
                if (online) {
                    errorMessage = null
                    loadChats()
                }
            }
        }
    }

    fun loadChats() {
        viewModelScope.launch {
            isLoading = chats.isEmpty()
            RetrofitClient.chatRepository.refreshChatsFromNetwork()
                .onSuccess {
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
}