package com.example.vent.util

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import okhttp3.Request
import com.example.vent.data.network.RetrofitClient

fun createAuthenticatedImageLoader(context: Context): ImageLoader {
    val tokenManager = RetrofitClient.getTokenManager()

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = tokenManager.getAccessToken()
            val request: Request = if (!token.isNullOrEmpty()) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        .build()

    return ImageLoader.Builder(context)
        .okHttpClient(okHttpClient)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(50L * 1024 * 1024)
                .build()
        }
        .build()
}