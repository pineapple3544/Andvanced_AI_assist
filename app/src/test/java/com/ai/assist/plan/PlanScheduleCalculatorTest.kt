package com.ai.assist.plan

import com.ai.assist.domain.ToolCall
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanScheduleCalculatorTest {
    @Test
    fun intervalAddsMinutes() {
        assertEquals(600_000L, PlanScheduleCalculator.nextIntervalTime(0L, 10L))
    }

    @Test
    fun dailyUsesTodayWhenFuture() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 17, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val next = PlanScheduleCalculator.nextDailyTime(calendar.timeInMillis, 9, 30)
        val nextCalendar = Calendar.getInstance().apply { timeInMillis = next }

        assertEquals(Calendar.JUNE, nextCalendar.get(Calendar.MONTH))
        assertEquals(17, nextCalendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, nextCalendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, nextCalendar.get(Calendar.MINUTE))
    }

    @Test
    fun dailyMovesToTomorrowWhenPast() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 17, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val next = PlanScheduleCalculator.nextDailyTime(calendar.timeInMillis, 9, 30)

        assertTrue(next > calendar.timeInMillis)
        assertEquals(24 * 60 * 60 * 1000L - 30 * 60 * 1000L, next - calendar.timeInMillis)
    }

    @Test
    fun labelShowsDailyTime() {
        val plan = PlanItem(
            id = "p",
            title = "Daily",
            toolCall = ToolCall("launchApp"),
            scheduledAtMillis = 0L,
            scheduleType = ScheduleType.Daily,
            dailyHour = 8,
            dailyMinute = 5,
            status = PlanStatus.Pending,
            createdAtMillis = 0L,
        )

        assertEquals("Daily at 08:05", PlanScheduleCalculator.label(plan))
    }
}
