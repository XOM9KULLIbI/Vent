package com.example.vent.data.network
import com.example.vent.data.network.models.RefreshTokenRequest
import com.example.vent.data.network.models.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
interface AuthApi {
    @FormUrlEncoded
    @POST("api/auth/token")
    suspend fun login(
        @Header("User-Agent") userAgent: String = "VentAndroidApp/1.0",
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<TokenResponse>

    @POST("api/auth/token/refresh")
    suspend fun refreshToken(
        @Header("User-Agent") userAgent: String = "VentAndroidApp/1.0",
        @Body request: RefreshTokenRequest
    ): Response<TokenResponse>

    @POST("api/auth/token/revoke")
    suspend fun revokeToken(
        @Body request: RefreshTokenRequest
    ): Response<Unit>

    @retrofit2.http.GET("api/users/me")
    suspend fun getMe(): Response<com.example.vent.data.network.models.UserMe>
}