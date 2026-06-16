package com.ai.assist.tools

import com.ai.assist.accessibility.AssistAccessibilityService
import com.ai.assist.accessibility.MacroStep
import com.ai.assist.domain.ToolResult
import org.json.JSONArray

class AccessibilityTool {
    fun run(arguments: Map<String, String>): ToolResult {
        val service = AssistAccessibilityService.instance
            ?: return ToolResult(false, "Accessibility service is not enabled.")
        return when (val action = arguments["action"].orEmpty()) {
            "back" -> service.performBack()
            "home" -> service.performHome()
            "scrollForward" -> service.performScrollForward()
            "clickText" -> service.clickText(arguments["text"].orEmpty())
            "inputText" -> service.inputText(arguments["text"].orEmpty())
            else -> ToolResult(false, "Unsupported accessibility action: $action")
        }
    }

    fun summarizeVisibleScreen(): ToolResult {
        val service = AssistAccessibilityService.instance
            ?: return ToolResult(false, "Accessibility service is not enabled.")
        val visibleText = service.collectVisibleText()
        if (!visibleText.success) return visibleText
        return ToolResult(true, summarizeText(visibleText.message))
    }

    fun runMacro(rawSteps: String): ToolResult {
        val service = AssistAccessibilityService.instance
            ?: return ToolResult(false, "Accessibility service is not enabled.")
        val steps = parseSteps(rawSteps)
        return service.runMacro(steps)
    }

    private fun summarizeText(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val preview = lines.take(8).joinToString("\n")
        return "Visible screen summary (${lines.size} text lines):\n$preview"
    }

    private fun parseSteps(raw: String): List<MacroStep> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(MacroStep(item.optString("action"), item.optString("value")))
                }
            }
        }.getOrElse {
            raw.split(";")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map {
                    val parts = it.split(":", limit = 2)
                    MacroStep(parts[0].trim(), parts.getOrNull(1)?.trim().orEmpty())
                }
        }
    }
}
