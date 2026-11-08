package com.example.vent.data.network

import com.example.vent.data.network.models.Chat
import com.example.vent.data.network.models.Message
import com.example.vent.data.network.models.SendMessageRequest
import com.example.vent.data.network.models.FileUploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Part
import com.example.vent.data.network.models.FileDownloadResponse
interface ChatApi {


    @GET("api/files/{file_id}")
    suspend fun getFileInfo(
        @Path("file_id") fileId: Long
    ): Response<FileDownloadResponse>
    @GET("api/chats/")
    suspend fun getChats(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 15
    ): Response<List<Chat>>


    @GET("api/chats/{chat_id}")
    suspend fun getMessages(
        @Path("chat_id") chatId: Long,
        @Query("before_message_id") beforeMessageId: Long? = null,
        @Query("limit") limit: Int = 30
    ): Response<List<Message>>


    @POST("api/chats/{chat_id}/message")
    suspend fun sendMessage(
        @Path("chat_id") chatId: Long,
        @Body request: SendMessageRequest
    ): Response<Message>
    // Загрузка файла на сервер
    @Multipart
    @POST("api/files/")
    suspend fun uploadFile(
        @Query("chat_id") chatId: Long,
        @Part file: MultipartBody.Part
    ): Response<FileUploadResponse>
}

