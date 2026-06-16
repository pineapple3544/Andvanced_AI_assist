package com.ai.assist.ai

import com.ai.assist.domain.AppSettings
import com.ai.assist.domain.ToolCall
import kotlinx.coroutines.flow.Flow

data class GenerateRequest(
    val prompt: String,
    val settings: AppSettings,
    val availableTools: String = "",
)

sealed interface AiEvent {
    data class Text(val value: String) : AiEvent
    data class ToolCallProposed(val call: ToolCall) : AiEvent
    data class Error(val message: String) : AiEvent
    data object Done : AiEvent
}

interface AiEngine {
    fun generate(request: GenerateRequest): Flow<AiEvent>
}
