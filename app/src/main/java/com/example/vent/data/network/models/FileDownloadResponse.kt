package com.example.vent.data.network.models

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class FileDownloadResponse(
    @Json(name = "file_id") val fileId: Long,
    @Json(name = "filename") val filename: String?,
    @Json(name = "content_type") val contentType: String?,
    @Json(name = "file_size") val fileSize: Long?,
    @Json(name = "uploaded_at") val uploadedAt: String?,
    @Json(name = "download_url") val downloadUrl: String
)