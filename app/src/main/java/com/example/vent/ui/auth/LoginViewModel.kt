package com.example.vent.ui.auth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vent.data.network.RetrofitClient
import kotlinx.coroutines.launch
class LoginViewModel: ViewModel() {
    // Состояния полей ввода
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    // Состояния процесса входа
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var accessToken by mutableStateOf<String?>(null)

    fun login() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Заполните все поля"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val response = RetrofitClient.authApi.login(
                    username = username,
                    password = password
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        accessToken = body.accessToken

                        // Сохраняем токены в SharedPreferences через наш TokenManager
                        RetrofitClient.getTokenManager().saveTokens(
                            accessToken = body.accessToken,
                            refreshToken = body.refreshToken
                        )
                    }
                } else {
                    errorMessage = "Ошибка входа: ${response.code()}"
                }
            } finally {
                isLoading = false
            }
        }
    }
}