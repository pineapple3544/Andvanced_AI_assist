package com.ai.assist.plan

import java.util.Calendar

object PlanScheduleCalculator {
    fun nextIntervalTime(nowMillis: Long, intervalMinutes: Long): Long =
        nowMillis + intervalMinutes.coerceAtLeast(1L) * 60_000L

    fun nextDailyTime(nowMillis: Long, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= nowMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    fun nextTimeAfterRun(plan: PlanItem, nowMillis: Long = System.currentTimeMillis()): Long? =
        when (plan.scheduleType) {
            ScheduleType.Once -> null
            ScheduleType.Interval -> nextIntervalTime(nowMillis, plan.repeatIntervalMinutes ?: 1L)
            ScheduleType.Daily -> nextDailyTime(nowMillis, plan.dailyHour ?: 9, plan.dailyMinute ?: 0)
        }

    fun label(plan: PlanItem): String =
        when (plan.scheduleType) {
            ScheduleType.Once -> "One-time"
            ScheduleType.Interval -> "Every ${plan.repeatIntervalMinutes ?: 1} minutes"
            ScheduleType.Daily -> "Daily at %02d:%02d".format(plan.dailyHour ?: 9, plan.dailyMinute ?: 0)
        }
}
