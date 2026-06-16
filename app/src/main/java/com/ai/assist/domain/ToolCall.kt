package com.ai.assist.domain

data class ToolCall(
    val name: String,
    val arguments: Map<String, String> = emptyMap(),
    val source: String = "local",
) {
    val summary: String
        get() = if (arguments.isEmpty()) name else "$name $arguments"
}

data class ToolResult(
    val success: Boolean,
    val message: String,
)

enum class ToolRisk {
    Safe,
    RequiresConfirmation,
}
