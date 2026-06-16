package com.ai.assist.domain

data class ChatMessage(
    val id: Long,
    val role: ChatRole,
    val text: String,
)

enum class ChatRole {
    User,
    Assistant,
    System,
    Tool,
}
