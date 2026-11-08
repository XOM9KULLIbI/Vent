package com.example.vent.data.network.models

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class FileUploadResponse(
    @Json(name = "file_id") val fileId: Long,
    @Json(name = "filename") val filename: String,
    @Json(name = "content_type") val contentType: String?,
    @Json(name = "file_size") val fileSize: Long?
)