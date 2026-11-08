package com.example.vent.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit // Добавили параметр
) {
    // Отслеживаем появления токена в ViewModel и сразу дергаем колбэк для смены экрана
    LaunchedEffect(viewModel.accessToken) {
        if (!viewModel.accessToken.isNullOrEmpty()) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Вход в Vent",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Поле логина
        OutlinedTextField(
            value = viewModel.username,
            onValueChange = { viewModel.username = it },
            label = { Text("Логин") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Поле пароля
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка входа / индикатор загрузки
        if (viewModel.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.login() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Войти")
            }
        }

        // Отображение ошибки
        viewModel.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}