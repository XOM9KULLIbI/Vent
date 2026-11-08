package com.example.vent.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            // Fallback: emit true if registration fails
            trySend(true)
        }

        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val initial = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(initial)

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()

    fun isOnlineNow(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/**
 * Проверяет, является ли ошибка ошибкой отсутствия интернет-соединения
 */
fun isNetworkException(t: Throwable): Boolean {
    var cause: Throwable? = t
    while (cause != null) {
        if (cause is UnknownHostException ||
            cause is ConnectException ||
            cause is SocketTimeoutException ||
            (cause is IOException && cause.message?.contains("Unable to resolve host", ignoreCase = true) == true)
        ) {
            return true
        }
        cause = cause.cause
    }
    return false
}
