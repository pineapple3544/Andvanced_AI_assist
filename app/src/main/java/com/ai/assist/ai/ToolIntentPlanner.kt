package com.ai.assist.ai

import com.ai.assist.domain.ToolCall

class ToolIntentPlanner {
    fun plan(prompt: String, source: String = "local"): ToolCall? {
        val normalized = prompt.lowercase()
        return when {
            isScheduleRequest(normalized) ->
                schedulePlan(prompt, normalized, source)

            containsAny(normalized, "list apps", "installed apps", "app list") ->
                ToolCall("listInstalledApps", source = source)

            containsAny(normalized, "device", "capability", "spec", "ram") ->
                ToolCall("getDeviceCapability", source = source)

            containsAny(normalized, "calendar event", "add calendar") ->
                ToolCall("addCalendarEvent", mapOf("title" to extractCalendarTitle(prompt)), source)

            containsAny(normalized, "email draft", "compose email") ->
                ToolCall(
                    "composeEmail",
                    mapOf(
                        "to" to extractEmailTo(prompt),
                        "subject" to extractAfterToken(prompt, "subject"),
                        "body" to extractAfterToken(prompt, "body"),
                    ),
                    source,
                )

            containsAny(normalized, "summarize screen", "summarize visible") ->
                ToolCall("summarizeVisibleScreen", source = source)

            containsAny(normalized, "open file", "file ", ".pdf", ".jpg", ".png", ".mp4", ".txt") ->
                ToolCall("openFile", mapOf("pathOrUri" to extractFileTarget(prompt)), source)

            containsAny(normalized, "back") ->
                ToolCall("runAccessibilityAction", mapOf("action" to "back"), source)

            containsAny(normalized, "home") ->
                ToolCall("runAccessibilityAction", mapOf("action" to "home"), source)

            containsAny(normalized, "scroll") ->
                ToolCall("runAccessibilityAction", mapOf("action" to "scrollForward"), source)

            containsAny(normalized, "tap", "click") ->
                ToolCall(
                    "runAccessibilityAction",
                    mapOf("action" to "clickText", "text" to extractClickText(prompt)),
                    source,
                )

            containsAny(normalized, "launch", "open", "start", "run ", "running ") ->
                ToolCall("launchApp", mapOf("appQuery" to extractAppQuery(prompt)), source)

            else -> null
        }
    }

    private fun containsAny(text: String, vararg needles: String): Boolean =
        needles.any { text.contains(it) }

    private fun isScheduleRequest(text: String): Boolean =
        containsAny(text, "schedule", "later", "after") ||
            Regex("\\d+\\s*(minute|minutes|min)\\b").containsMatchIn(text)

    private fun extractDelayMinutes(text: String): Long {
        val match = Regex("(\\d+)\\s*(minute|minutes|min)").find(text)
        return match?.groupValues?.getOrNull(1)?.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
    }

    private fun extractAppQuery(prompt: String): String {
        var value = prompt
            .replace(Regex("\\band\\s+search\\b.*", RegexOption.IGNORE_CASE), "")
            .replace("launch", "", ignoreCase = true)
            .replace("open", "", ignoreCase = true)
            .replace("start", "", ignoreCase = true)
            .replace("running", "", ignoreCase = true)
            .replace("run", "", ignoreCase = true)
            .replace("schedule", "", ignoreCase = true)
            .replace("later", "", ignoreCase = true)
            .replace("after", "", ignoreCase = true)
            .trim()
        value = value.replace(Regex("\\d+\\s*(minute|minutes|min)\\b.*", RegexOption.IGNORE_CASE), "").trim()
        return value.ifBlank { "settings" }
    }

    private fun extractClickText(prompt: String): String =
        prompt.substringAfter("click", "").substringAfter("tap", "").trim().ifBlank { prompt }

    private fun extractFileTarget(prompt: String): String =
        prompt.substringAfter("open file", prompt)
            .replace("open", "", ignoreCase = true)
            .replace("file", "", ignoreCase = true)
            .trim()
            .ifBlank { prompt.trim() }

    private fun extractCalendarTitle(prompt: String): String =
        prompt.replace("add", "", ignoreCase = true)
            .replace("calendar", "", ignoreCase = true)
            .replace("event", "", ignoreCase = true)
            .trim()
            .ifBlank { "New event" }

    private fun extractEmailTo(prompt: String): String {
        val match = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").find(prompt)
        return match?.value.orEmpty()
    }

    private fun extractAfterToken(prompt: String, token: String): String {
        val marker = "$token "
        return prompt.substringAfter(marker, "")
            .substringBefore(" body ")
            .substringBefore(" subject ")
            .trim()
    }

    private fun schedulePlan(prompt: String, normalized: String, source: String): ToolCall {
        val delay = extractDelayMinutes(normalized).toString()
        val withoutSchedule = prompt
            .replace(Regex("schedule", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bafter\\s+\\d+\\s*(minute|minutes|min)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bin\\s+\\d+\\s*(minute|minutes|min)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\d+\\s*(minute|minutes|min)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\blater\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bafter\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bin\\b", RegexOption.IGNORE_CASE), "")
            .trim()
        val nested = if (isScheduleRequest(withoutSchedule.lowercase())) null else plan(withoutSchedule, source = source)
        return if (nested != null && nested.name != "scheduleLaunch") {
            val args = buildMap {
                put("toolName", nested.name)
                put("delayMinutes", delay)
                nested.arguments.forEach { (key, value) -> put("arg_$key", value) }
            }
            ToolCall("scheduleLaunch", args, source)
        } else {
            ToolCall(
                name = "scheduleLaunch",
                arguments = mapOf(
                    "appQuery" to extractAppQuery(withoutSchedule),
                    "delayMinutes" to delay,
                ),
                source = source,
            )
        }
    }
}
