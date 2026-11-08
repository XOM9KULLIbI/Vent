package com.example.vent.data.network

import android.content.Context
import com.example.vent.data.local.AppDatabase
import com.example.vent.data.repository.ChatRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.vent.util.NetworkMonitor
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://messenger-3yfw.onrender.com/"

    private lateinit var tokenManager: TokenManager
    private lateinit var appContext: Context

    private lateinit var database: AppDatabase
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var webSocketManager: VentWebSocketManager
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set

    var onUnauthorizedListener: (() -> Unit)? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        tokenManager = TokenManager(appContext)
        networkMonitor = NetworkMonitor(appContext)
        database = AppDatabase.get(appContext)
        chatRepository = ChatRepository(database)
        webSocketManager = VentWebSocketManager(
            tokenManager = tokenManager,
            chatRepository = chatRepository,
            networkMonitor = networkMonitor,
            onUnauthorized = { onUnauthorizedListener?.invoke() }
        )
    }


    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // ---- Общий клиент для Retrofit (API-запросы) ----
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor(tokenManager) {
                onUnauthorizedListener?.invoke()
            })
            .build()
    }

    // ---- Отдельный клиент для Coil (картинки) с дисковым кэшем ----
    val imageOkHttpClient: OkHttpClient by lazy {
        // Форсим заголовки, чтобы Coil точно закэшировал файл,
        // даже если сервер не отдаёт Cache-Control.
        val cacheControlInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (chain.request().url.encodedPath.startsWith("/api/files/")) {
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=86400") // 1 день
                    .build()
            } else {
                response
            }
        }

        val cacheDir = File(appContext.cacheDir, "http_image_cache")
        val cache = Cache(cacheDir, 100L * 1024 * 1024) // 100 МБ

        OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(AuthInterceptor(tokenManager) { /* 401 для картинок игнорируем */ })
            .addNetworkInterceptor(cacheControlInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val chatApi: ChatApi by lazy { retrofit.create(ChatApi::class.java) }

    fun getTokenManager(): TokenManager = tokenManager
}