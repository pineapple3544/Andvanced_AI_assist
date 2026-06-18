package com.ai.assist.domain

data class ApiSettings(
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4.1-mini",
    val temperature: Float = 0.2f,
    val maxTokens: Int = 512,
)

data class AppSettings(
    val aiMode: AiMode = AiMode.Local,
    val modelPath: String = "",
    val api: ApiSettings = ApiSettings(),
    val autoApproveToolCalls: Boolean = false,
    val monochromeUi: Boolean = false,
)
