package com.ai.assist.ai

import com.ai.assist.domain.AppSettings
import com.ai.assist.domain.ToolCall
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class OpenAiCompatibleEngine(
    private val planner: ToolIntentPlanner = ToolIntentPlanner(),
) : AiEngine {
    override fun generate(request: GenerateRequest): Flow<AiEvent> = flow {
        if (request.settings.api.apiKey.isBlank()) {
            emit(AiEvent.Text("API key is empty. Falling back to the local intent planner."))
            planner.plan(request.prompt, source = "api-fallback")?.let {
                emit(AiEvent.ToolCallProposed(it))
            } ?: emit(AiEvent.Text("No tool matched the request."))
            emit(AiEvent.Done)
            return@flow
        }

        runCatching { callApi(request) }
            .onSuccess { content ->
                emit(AiEvent.Text(content))
                extractToolCall(content)?.let { emit(AiEvent.ToolCallProposed(it.copy(source = "api"))) }
                    ?: planner.plan(content, source = "api")?.let { emit(AiEvent.ToolCallProposed(it)) }
            }
            .onFailure { error ->
                emit(AiEvent.Error("API request failed: ${error.message ?: error::class.java.simpleName}"))
            }
        emit(AiEvent.Done)
    }

    suspend fun completeText(
        settings: AppSettings,
        systemPrompt: String,
        prompt: String,
    ): String {
        if (settings.api.apiKey.isBlank()) error("API key is empty. Add an OpenAI-compatible API key in Settings.")
        return callApi(
            request = GenerateRequest(prompt, settings, availableTools = ""),
            systemPrompt = systemPrompt,
        )
    }

    private suspend fun callApi(
        request: GenerateRequest,
        systemPrompt: String = SYSTEM_PROMPT,
    ): String = withContext(Dispatchers.IO) {
        val endpoint = request.settings.api.baseUrl.trimEnd('/') + "/chat/completions"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${request.settings.api.apiKey}")
        }
        val body = JSONObject()
            .put("model", request.settings.api.model)
            .put("temperature", request.settings.api.temperature.toDouble())
            .put("max_tokens", request.settings.api.maxTokens)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", request.prompt)),
            )
        OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(stream.reader()).use { it.readText() }
        if (connection.responseCode !in 200..299) error(response)
        JSONObject(response)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .optString("content")
    }

    private fun extractToolCall(content: String): ToolCall? {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching {
            val json = JSONObject(content.substring(start, end + 1))
            val name = json.optString("tool")
            if (name.isBlank()) return null
            val args = json.optJSONObject("arguments")
            val map = buildMap {
                if (args != null) {
                    args.keys().forEach { key -> put(key, args.optString(key)) }
                }
            }
            ToolCall(name, map, source = "api")
        }.getOrNull()
    }

    private companion object {
        const val SYSTEM_PROMPT =
            "You are a mobile AI assistant planner. If an Android tool should run, answer with compact JSON like {\"tool\":\"launchApp\",\"arguments\":{\"appQuery\":\"Settings\"}}. Valid tools: listInstalledApps, launchApp, addCalendarEvent, composeEmail, createDocument, summarizeVisibleScreen, performAppMacro, scheduleLaunch, cancelScheduledAction, getDeviceCapability, openFile, runAccessibilityAction. For document requests like making a PPT/PDF template, use createDocument with topic, format pptx/pdf, style, and slideCount. For schedules, scheduleLaunch supports delayMinutes, repeatIntervalMinutes, or dailyHour/dailyMinute. Do not use search tools from normal chat; search is handled by the app's explicit Search button. Otherwise answer normally."
    }
}
