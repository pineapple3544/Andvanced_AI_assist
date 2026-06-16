package com.ai.assist.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ToolIntentPlannerTest {
    private val planner = ToolIntentPlanner()

    @Test
    fun launchRequestMapsToLaunchApp() {
        val call = planner.plan("Settings open")

        assertNotNull(call)
        assertEquals("launchApp", call?.name)
        assertEquals("Settings", call?.arguments?.get("appQuery"))
    }

    @Test
    fun scheduleRequestIncludesDelay() {
        val call = planner.plan("Settings 5 minutes schedule")

        assertNotNull(call)
        assertEquals("scheduleLaunch", call?.name)
        assertEquals("5", call?.arguments?.get("delayMinutes"))
    }

    @Test
    fun accessibilityBackMapsToToolCall() {
        val call = planner.plan("back")

        assertNotNull(call)
        assertEquals("runAccessibilityAction", call?.name)
        assertEquals("back", call?.arguments?.get("action"))
    }
}
