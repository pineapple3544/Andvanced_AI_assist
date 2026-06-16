package com.ai.assist.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class HybridAiEngine(
    private val local: AiEngine,
    private val remote: AiEngine,
) : AiEngine {
    override fun generate(request: GenerateRequest): Flow<AiEvent> = flow {
        var proposedTool = false
        local.generate(request).collect { event ->
            if (event is AiEvent.ToolCallProposed) proposedTool = true
            if (event !is AiEvent.Done) emit(event)
        }
        if (!proposedTool && shouldAskRemote(request.prompt)) {
            emit(AiEvent.Text("Local planner did not find a tool. Asking the OpenAI-compatible backend."))
            remote.generate(request).collect { event ->
                if (event !is AiEvent.Done) emit(event)
            }
        }
        emit(AiEvent.Done)
    }

    private fun shouldAskRemote(prompt: String): Boolean =
        prompt.length > 24 || prompt.contains("api", ignoreCase = true)
}
