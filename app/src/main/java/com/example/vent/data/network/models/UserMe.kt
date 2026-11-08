package com.example.vent.data.network.models

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class UserMe(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "username") val username: String,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "avatar_file_url") val avatarFileUrl: String? = null
)
