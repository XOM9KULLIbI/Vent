package com.example.vent.data.network.models
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String
)
