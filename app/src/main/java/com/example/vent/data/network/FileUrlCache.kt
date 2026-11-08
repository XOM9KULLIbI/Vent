package com.example.vent.data.network

/**
 * In-memory кэш download_url по fileId.
 * Живёт, пока жив процесс.
 */
object FileUrlCache {

    private val urls = mutableMapOf<Long, String>()

    @Synchronized
    fun get(fileId: Long): String? = urls[fileId]

    @Synchronized
    fun put(fileId: Long, url: String) {
        urls[fileId] = url
    }

    @Synchronized
    fun evict(fileId: Long) {
        urls.remove(fileId)
    }

    @Synchronized
    fun clear() {
        urls.clear()
    }
}