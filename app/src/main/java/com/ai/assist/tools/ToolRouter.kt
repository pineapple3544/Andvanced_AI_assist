package com.ai.assist.tools

import android.content.Context
import com.ai.assist.domain.ToolCall
import com.ai.assist.domain.ToolResult
import com.ai.assist.domain.ToolRisk
import com.ai.assist.plan.PlanRepository
import com.ai.assist.plan.PlanScheduler

class ToolRouter(
    private val context: Context,
    private val appLauncher: AppLauncher = AppLauncher(context),
    private val planRepository: PlanRepository = PlanRepository(context),
    private val planScheduler: PlanScheduler = PlanScheduler(context),
    private val deviceCapability: DeviceCapability = DeviceCapability(context),
    private val accessibilityTool: AccessibilityTool = AccessibilityTool(),
    private val fileOpener: FileOpener = FileOpener(context),
    private val appActionTools: AppActionTools = AppActionTools(context),
) {
    fun riskFor(call: ToolCall): ToolRisk = when (call.name) {
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

    fun execute(call: ToolCall): ToolResult = when (call.name) {
        "listInstalledApps" -> {
            val apps = appLauncher.listInstalledApps().take(12).joinToString { it.label }
            ToolResult(true, "Installed apps: $apps")
        }

        "launchApp" -> appLauncher.launch(call.arguments["appQuery"].orEmpty())

        "scheduleLaunch" -> {
            val nestedTool = call.arguments["toolName"]
            if (!nestedTool.isNullOrBlank()) {
                val delay = call.arguments["delayMinutes"]?.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
                val nestedCall = call.toNestedToolCall()
                val plan = planRepository.addPlan("Run ${nestedCall.name}", nestedCall, delay)
                planScheduler.schedule(plan)
                ToolResult(true, "Added plan '${plan.title}' for $delay minute(s) from now. Plan id: ${plan.id}")
            } else {
                val query = call.arguments["appQuery"].orEmpty()
                val delay = call.arguments["delayMinutes"]?.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
                val plan = planRepository.addLaunchPlan(query, delay)
                planScheduler.schedule(plan)
                ToolResult(true, "Added plan '${plan.title}' for $delay minute(s) from now. Plan id: ${plan.id}")
            }
        }

        "cancelScheduledAction" -> {
            val planId = call.arguments["planId"].orEmpty()
            if (planId.isNotBlank()) {
                planRepository.cancel(planId)
                planScheduler.cancel(planId)
                ToolResult(true, "Canceled plan $planId.")
            } else {
                val plans = planRepository.list()
                val count = planRepository.cancelAll()
                planScheduler.cancelAll(plans)
                ToolResult(true, "Canceled $count pending/running plan(s).")
            }
        }

        "getDeviceCapability" -> ToolResult(true, deviceCapability.describe())

        "openFile" -> fileOpener.open(
            call.arguments["pathOrUri"].orEmpty(),
            call.arguments["mimeHint"],
        )

        "searchWeb" -> appActionTools.searchWeb(
            query = call.arguments["query"].orEmpty(),
            provider = call.arguments["provider"],
        )

        "searchYouTube" -> appActionTools.searchYouTube(call.arguments["query"].orEmpty())

        "addCalendarEvent" -> appActionTools.addCalendarEvent(
            title = call.arguments["title"].orEmpty(),
            startMillis = call.arguments["startMillis"]?.toLongOrNull(),
            endMillis = call.arguments["endMillis"]?.toLongOrNull(),
            location = call.arguments["location"],
            description = call.arguments["description"],
        )

        "composeEmail" -> appActionTools.composeEmail(
            to = call.arguments["to"],
            subject = call.arguments["subject"],
            body = call.arguments["body"],
        )

        "summarizeVisibleScreen" -> accessibilityTool.summarizeVisibleScreen()

        "performAppMacro" -> accessibilityTool.runMacro(call.arguments["steps"].orEmpty())

        "runAccessibilityAction" -> accessibilityTool.run(call.arguments)

        else -> ToolResult(false, "Unknown tool: ${call.name}")
    }

    fun executeForModel(call: ToolCall): ToolResult {
        return if (riskFor(call) == ToolRisk.RequiresConfirmation) {
            ToolResult(false, "Approval required before executing ${call.name}.")
        } else {
            execute(call)
        }
    }

    private fun ToolCall.toNestedToolCall(): ToolCall {
        val nestedName = arguments["toolName"].orEmpty()
        val nestedArgs = arguments
            .filterKeys { it != "toolName" && it != "delayMinutes" }
            .mapKeys { (key, _) -> key.removePrefix("arg_") }
        return ToolCall(nestedName, nestedArgs, source = source)
    }
}
