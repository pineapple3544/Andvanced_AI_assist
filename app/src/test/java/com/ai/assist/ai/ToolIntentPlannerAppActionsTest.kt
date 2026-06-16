package com.ai.assist.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ToolIntentPlannerAppActionsTest {
    private val planner = ToolIntentPlanner()

    @Test
    fun searchTextDoesNotCreateToolCall() {
        val call = planner.plan("search Google for Android LiteRT")

        assertNull(call)
    }

    @Test
    fun youtubeSearchTextDoesNotCreateToolCall() {
        val call = planner.plan("search YouTube for Gemma 4 demo")

        assertNull(call)
    }

    @Test
    fun scheduledSearchTextFallsBackToLaunchPlanInsteadOfSearch() {
        val call = planner.plan("schedule search Google for Android LiteRT in 1 minute")

        assertEquals("scheduleLaunch", call?.name)
        assertEquals("search Google for Android LiteRT", call?.arguments?.get("appQuery"))
        assertEquals("1", call?.arguments?.get("delayMinutes"))
    }

    @Test
    fun runningCameraAfterOneMinuteCreatesLaunchPlan() {
        val call = planner.plan("running camera after 1 minutes")

        assertEquals("scheduleLaunch", call?.name)
        assertEquals("launchApp", call?.arguments?.get("toolName"))
        assertEquals("camera", call?.arguments?.get("arg_appQuery"))
        assertEquals("1", call?.arguments?.get("delayMinutes"))
    }

    @Test
    fun launchCameraAfterOneMinuteCreatesLaunchPlan() {
        val call = planner.plan("launch camera after 1minutes")

        assertEquals("scheduleLaunch", call?.name)
        assertEquals("launchApp", call?.arguments?.get("toolName"))
        assertEquals("camera", call?.arguments?.get("arg_appQuery"))
        assertEquals("1", call?.arguments?.get("delayMinutes"))
    }

    @Test
    fun runningGoogleAndSearchDoesNotUseSearchTool() {
        val call = planner.plan("running google and search how to use my phone")

        assertEquals("launchApp", call?.name)
        assertEquals("google", call?.arguments?.get("appQuery"))
    }
}
