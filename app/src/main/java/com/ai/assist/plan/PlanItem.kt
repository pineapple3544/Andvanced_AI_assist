package com.ai.assist.plan

import com.ai.assist.domain.ToolCall

data class PlanItem(
    val id: String,
    val title: String,
    val toolCall: ToolCall,
    val scheduledAtMillis: Long,
    val scheduleType: ScheduleType = ScheduleType.Once,
    val repeatIntervalMinutes: Long? = null,
    val dailyHour: Int? = null,
    val dailyMinute: Int? = null,
    val status: PlanStatus,
    val createdAtMillis: Long,
    val lastRunAtMillis: Long? = null,
    val lastResult: String? = null,
)

enum class ScheduleType {
    Once,
    Interval,
    Daily,
}

enum class PlanStatus {
    Pending,
    Running,
    Succeeded,
    Failed,
    Canceled,
}
