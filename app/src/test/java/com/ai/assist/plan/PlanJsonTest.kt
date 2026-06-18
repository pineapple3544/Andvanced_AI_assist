package com.ai.assist.plan

import com.ai.assist.domain.ToolCall
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanJsonTest {
    @Test
    fun roundTripsPlanItems() {
        val item = PlanItem(
            id = "p1",
            title = "Launch Settings",
            toolCall = ToolCall("launchApp", mapOf("appQuery" to "Settings"), "plan"),
            scheduledAtMillis = 1000L,
            scheduleType = ScheduleType.Once,
            status = PlanStatus.Pending,
            createdAtMillis = 500L,
        )

        val decoded = PlanJson.decode(PlanJson.encode(listOf(item)))

        assertEquals(listOf(item), decoded)
    }

    @Test
    fun roundTripsNonLaunchToolCalls() {
        val item = PlanItem(
            id = "p2",
            title = "Search",
            toolCall = ToolCall("searchWeb", mapOf("query" to "Android LiteRT"), "plan"),
            scheduledAtMillis = 2000L,
            scheduleType = ScheduleType.Interval,
            repeatIntervalMinutes = 10L,
            status = PlanStatus.Pending,
            createdAtMillis = 1000L,
        )

        val decoded = PlanJson.decode(PlanJson.encode(listOf(item)))

        assertEquals("searchWeb", decoded.single().toolCall.name)
        assertEquals("Android LiteRT", decoded.single().toolCall.arguments["query"])
        assertEquals(ScheduleType.Interval, decoded.single().scheduleType)
        assertEquals(10L, decoded.single().repeatIntervalMinutes)
    }
}
