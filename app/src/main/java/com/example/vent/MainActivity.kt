package com.example.vent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.vent.data.network.RetrofitClient
import com.example.vent.ui.auth.LoginScreen
import com.example.vent.ui.chat.ChatActivity
import com.example.vent.ui.chats.ChatListScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), ImageLoaderFactory {

    private var isAuthorized by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RetrofitClient.init(applicationContext)

        val token = RetrofitClient.getTokenManager().getAccessToken()
        isAuthorized = !token.isNullOrEmpty()
        if (isAuthorized) {
            RetrofitClient.webSocketManager.connect()
            fetchCurrentUserInfo()
        }

        RetrofitClient.onUnauthorizedListener = {
            isAuthorized = false
            RetrofitClient.webSocketManager.disconnect()
            lifecycleScope.launch(Dispatchers.IO) {
                RetrofitClient.chatRepository.clearAll()
            }
        }

        setContent {
            MaterialTheme {
                if (isAuthorized) {
                    val context = LocalContext.current
                    ChatListScreen(
                        onChatClick = { chatId ->
                            ChatActivity.start(context, chatId)
                        }
                    )
                } else {
                    LoginScreen(
                        onLoginSuccess = {
                            isAuthorized = true
                            RetrofitClient.webSocketManager.connect()
                            fetchCurrentUserInfo()
                        }
                    )
                }
            }
        }
    }

    private fun fetchCurrentUserInfo() {
        if (RetrofitClient.getTokenManager().getMyUserId() == null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.authApi.getMe()
                    if (response.isSuccessful && response.body() != null) {
                        RetrofitClient.getTokenManager().saveMyUserId(response.body()!!.userId)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            RetrofitClient.webSocketManager.disconnect()
        }
    }

    // ---- Подключаем Coil к авторизованному клиенту с кэшем ----
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { RetrofitClient.imageOkHttpClient }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)   // 25% свободной памяти
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}