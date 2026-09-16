package com.agroland.feature.chat.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Чат REST API (Flutter ChatRemoteService): тізім fallback, файл/дауыс
 * жүктеу, статус/жою/тазарту әрекеттері. Хабарлама ағынының өзі Socket.IO.
 */
interface ChatApi {

    /** GET /chat/?chat_status=ACTIVE|ARCHIVED — REST fallback (socket жоқ күй). */
    @GET("chat/")
    suspend fun getChats(@Query("chat_status") chatStatus: String): JsonElement

    /** POST /chat/upload/file — ≤10МБ, file_url қайтарады. */
    @Multipart
    @POST("chat/upload/file")
    suspend fun uploadFile(@Part file: MultipartBody.Part): JsonElement

    /** POST /chat/upload/audio — ≤10МБ, audio_url қайтарады. */
    @Multipart
    @POST("chat/upload/audio")
    suspend fun uploadAudio(@Part file: MultipartBody.Part): JsonElement

    /** PATCH /chat/{id}/{status} — active|archived. */
    @PATCH("chat/{chatId}/{status}")
    suspend fun updateChatStatus(
        @Path("chatId") chatId: Long,
        @Path("status") status: String,
    ): JsonElement

    /** DELETE /chat/message/{message_id} — өз хабарламасын әркімге жою. */
    @DELETE("chat/message/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: Long): JsonElement

    /** PATCH /chat/{id}/clear — чат тарихын тазалау. */
    @PATCH("chat/{chatId}/clear")
    suspend fun clearChatHistory(@Path("chatId") chatId: Long): JsonElement

    /** DELETE /chat/{id} — чатты толық жою. */
    @DELETE("chat/{chatId}")
    suspend fun deleteChat(@Path("chatId") chatId: Long): JsonElement
}

@Module
@InstallIn(SingletonComponent::class)
object ChatApiModule {

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)
}