package com.ai.assist.data.settings

import android.content.Context
import com.ai.assist.domain.AiMode
import com.ai.assist.domain.ApiSettings
import com.ai.assist.domain.AppSettings

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("assist_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        aiMode = runCatching { AiMode.valueOf(prefs.getString("ai_mode", AiMode.Local.name) ?: AiMode.Local.name) }
            .getOrDefault(AiMode.Local),
        modelPath = prefs.getString("model_path", "") ?: "",
        api = ApiSettings(
            baseUrl = prefs.getString("api_base_url", ApiSettings().baseUrl) ?: ApiSettings().baseUrl,
            apiKey = prefs.getString("api_key", "") ?: "",
            model = prefs.getString("api_model", ApiSettings().model) ?: ApiSettings().model,
            temperature = prefs.getFloat("api_temperature", ApiSettings().temperature),
            maxTokens = prefs.getInt("api_max_tokens", ApiSettings().maxTokens),
        ),
        autoApproveToolCalls = prefs.getBoolean("auto_approve_tool_calls", false),
        monochromeUi = prefs.getBoolean("monochrome_ui", false),
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString("ai_mode", settings.aiMode.name)
            .putString("model_path", settings.modelPath)
            .putString("api_base_url", settings.api.baseUrl)
            .putString("api_key", settings.api.apiKey)
            .putString("api_model", settings.api.model)
            .putFloat("api_temperature", settings.api.temperature)
            .putInt("api_max_tokens", settings.api.maxTokens)
            .putBoolean("auto_approve_tool_calls", settings.autoApproveToolCalls)
            .putBoolean("monochrome_ui", settings.monochromeUi)
            .apply()
    }
}
