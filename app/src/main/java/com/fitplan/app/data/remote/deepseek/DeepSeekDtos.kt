package com.fitplan.app.data.remote.deepseek

import com.google.gson.annotations.SerializedName

data class DeepSeekChatRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    @SerializedName("response_format")
    val responseFormat: DeepSeekResponseFormat = DeepSeekResponseFormat(),
    @SerializedName("max_tokens")
    val maxTokens: Int = 4096,
    val temperature: Double = 0.6,
    val stream: Boolean = false
)

data class DeepSeekMessage(
    val role: String,
    val content: String
)

data class DeepSeekResponseFormat(
    val type: String = "json_object"
)

data class DeepSeekChatResponse(
    val choices: List<DeepSeekChoice> = emptyList()
)

data class DeepSeekChoice(
    val message: DeepSeekResponseMessage? = null
)

data class DeepSeekResponseMessage(
    val content: String? = null
)
