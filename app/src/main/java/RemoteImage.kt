package com.example.vent.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.vent.data.network.FileUrlCache
import com.example.vent.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @param downloadUrl готовый URL из Message.Attachment (S3 signed, живёт 24 ч).
 *                    Если null — резолвим через GET /api/files/{fileId} (fallback).
 * @param fileId нужен только для fallback-запроса.
 */
@Composable
fun RemoteImage(
    downloadUrl: String?,
    fileId: Long? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onClick: (() -> Unit)? = null
) {
    // 1. Если URL пришёл с сервера — используем сразу, никаких запросов.
    // 2. Если URL есть в FileUrlCache — тоже сразу.
    // 3. Иначе — fallback через getFileInfo(fileId).
    var url by remember(downloadUrl, fileId) {
        mutableStateOf(downloadUrl ?: fileId?.let { FileUrlCache.get(it) })
    }
    var error by remember(downloadUrl, fileId) { mutableStateOf(false) }

    LaunchedEffect(downloadUrl, fileId) {
        if (url != null) return@LaunchedEffect
        if (fileId == null) {
            error = true
            return@LaunchedEffect
        }

        try {
            val resp = withContext(Dispatchers.IO) {
                RetrofitClient.chatApi.getFileInfo(fileId)
            }
            val fetched = if (resp.isSuccessful) resp.body()?.downloadUrl else null
            if (fetched != null) {
                FileUrlCache.put(fileId, fetched)
                url = fetched
            } else {
                error = true
            }
        } catch (e: Exception) {
            error = true
        }
    }

    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier

    when {
        url != null -> AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier.then(clickModifier),
            contentScale = contentScale
        )

        error -> Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "Ошибка загрузки",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        else -> Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        }
    }
}