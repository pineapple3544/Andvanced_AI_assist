package com.ai.assist.plan

import com.ai.assist.domain.ToolCall

data class PlanItem(
    val id: String,
    val title: String,
    val toolCall: ToolCall,
    val scheduledAtMillis: Long,
    val status: PlanStatus,
    val createdAtMillis: Long,
    val lastRunAtMillis: Long? = null,
    val lastResult: String? = null,
)

enum class PlanStatus {
    Pending,
    Running,
    Succeeded,
    Failed,
    Canceled,
}
