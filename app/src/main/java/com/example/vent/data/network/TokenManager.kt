package com.example.vent.data.network

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {

    // SharedPreferences — это системное key-value хранилище в Android (как локальный JSON на диске).
    // Функция getSharedPreferences создает файл "vent_prefs" внутри приватной папки приложения.
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vent_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_MY_USER_ID = "my_user_id"
    }

    // Сохраняем оба токена при успешном входе
    fun saveTokens(accessToken: String, refreshToken: String) {
        val editor = prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)

        extractUserIdFromJwt(accessToken)?.let { userId ->
            editor.putLong(KEY_MY_USER_ID, userId)
        }

        editor.apply()
    }

    fun saveMyUserId(userId: Long) {
        prefs.edit().putLong(KEY_MY_USER_ID, userId).apply()
    }

    fun getMyUserId(): Long? {
        val id = prefs.getLong(KEY_MY_USER_ID, -1L)
        return if (id != -1L) id else null
    }

    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    // Очищаем токены при выходе из аккаунта (Logout)
    fun clearTokens() {
        prefs.edit().clear().apply()
    }

    private fun extractUserIdFromJwt(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val decoded = android.util.Base64.decode(
                    parts[1],
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                )
                val json = org.json.JSONObject(String(decoded, Charsets.UTF_8))
                if (json.has("user_id")) json.getLong("user_id")
                else if (json.has("sub")) json.getString("sub").toLongOrNull()
                else if (json.has("id")) json.getLong("id")
                else null
            } else null
        } catch (_: Exception) {
            null
        }
    }
}