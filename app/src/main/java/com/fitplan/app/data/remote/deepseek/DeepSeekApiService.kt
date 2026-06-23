package com.fitplan.app.data.remote.deepseek

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DeepSeekApiService {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekChatRequest
    ): DeepSeekChatResponse
}
