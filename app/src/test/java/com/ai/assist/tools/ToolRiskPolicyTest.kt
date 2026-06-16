package com.ai.assist.tools

import com.ai.assist.domain.ToolCall
import com.ai.assist.domain.ToolRisk
import org.junit.Assert.assertEquals
import org.junit.Test

class ToolRiskPolicyTest {
    @Test
    fun safeToolNamesDoNotRequireConfirmation() {
        assertEquals(ToolRisk.Safe, riskFor(ToolCall("launchApp")))
        assertEquals(ToolRisk.Safe, riskFor(ToolCall("listInstalledApps")))
        assertEquals(ToolRisk.Safe, riskFor(ToolCall("getDeviceCapability")))
        assertEquals(ToolRisk.Safe, riskFor(ToolCall("openFile")))
        assertEquals(ToolRisk.Safe, riskFor(ToolCall("searchWeb")))
        assertEquals(ToolRisk.Safe, riskFor(ToolCall("searchYouTube")))
        assertEquals(ToolRisk.Safe, riskFor(ToolCall("addCalendarEvent")))
        assertEquals(ToolRisk.Safe, riskFor(ToolCall("composeEmail")))
    }

    @Test
    fun schedulerAndAccessibilityRequireConfirmation() {
        assertEquals(ToolRisk.RequiresConfirmation, riskFor(ToolCall("scheduleLaunch")))
        assertEquals(ToolRisk.RequiresConfirmation, riskFor(ToolCall("runAccessibilityAction")))
        assertEquals(ToolRisk.RequiresConfirmation, riskFor(ToolCall("summarizeVisibleScreen")))
        assertEquals(ToolRisk.RequiresConfirmation, riskFor(ToolCall("performAppMacro")))
        assertEquals(ToolRisk.RequiresConfirmation, riskFor(ToolCall("cancelScheduledAction")))
    }

    private fun riskFor(call: ToolCall): ToolRisk = when (call.name) {
        "launchApp",
        "listInstalledApps",
        "getDeviceCapability",
        "openFile",
        "searchWeb",
        "searchYouTube",
        "addCalendarEvent",
        "composeEmail",
        -> ToolRisk.Safe

        else -> ToolRisk.RequiresConfirmation
    }
}
