package com.example.vent.data.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val onUnauthorized: (() -> Unit)? = null
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getAccessToken()

        val requestToSend = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(requestToSend)

        // Если прилетел 401 — чистим токены и дергаем колбэк
        if (response.code == 401) {
            tokenManager.clearTokens()
            onUnauthorized?.invoke()
        }

        return response
    }
}