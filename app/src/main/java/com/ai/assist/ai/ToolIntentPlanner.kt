package com.ai.assist.ai

import com.ai.assist.domain.ToolCall

class ToolIntentPlanner {
    fun plan(prompt: String, source: String = "local"): ToolCall? {
        val normalized = prompt.lowercase()
        return when {
            isScheduleRequest(normalized) -> schedulePlan(prompt, normalized, source)

            containsAny(normalized, "list apps", "installed apps", "app list", "앱 목록", "설치된 앱") ->
                ToolCall("listInstalledApps", source = source)

            containsAny(normalized, "device", "capability", "spec", "ram", "기기 사양", "기기 정보") ->
                ToolCall("getDeviceCapability", source = source)

            containsAny(normalized, "calendar event", "add calendar", "일정 추가", "캘린더에") ->
                ToolCall("addCalendarEvent", mapOf("title" to extractCalendarTitle(prompt)), source)

            containsAny(normalized, "email draft", "compose email", "메일 작성", "이메일 작성") ->
                ToolCall(
                    "composeEmail",
                    mapOf(
                        "to" to extractEmailTo(prompt),
                        "subject" to extractAfterToken(prompt, "subject"),
                        "body" to extractAfterToken(prompt, "body"),
                    ),
                    source,
                )

            containsAny(normalized, "summarize screen", "summarize visible", "화면 요약") ->
                ToolCall("summarizeVisibleScreen", source = source)

            isDocumentRequest(normalized) ->
                ToolCall(
                    "createDocument",
                    mapOf(
                        "topic" to extractDocumentTopic(prompt),
                        "format" to extractDocumentFormat(normalized),
                        "style" to "simple",
                        "slideCount" to extractSlideCount(normalized).toString(),
                    ),
                    source,
                )

            containsAny(normalized, "open file", "file ", ".pdf", ".jpg", ".png", ".mp4", ".txt", "파일 열") ->
                ToolCall("openFile", mapOf("pathOrUri" to extractFileTarget(prompt)), source)

            containsAny(normalized, "back", "뒤로") ->
                ToolCall("runAccessibilityAction", mapOf("action" to "back"), source)

            containsAny(normalized, "home", "홈으로") ->
                ToolCall("runAccessibilityAction", mapOf("action" to "home"), source)

            containsAny(normalized, "scroll", "스크롤") ->
                ToolCall("runAccessibilityAction", mapOf("action" to "scrollForward"), source)

            containsAny(normalized, "tap", "click", "눌러", "클릭") ->
                ToolCall(
                    "runAccessibilityAction",
                    mapOf("action" to "clickText", "text" to extractClickText(prompt)),
                    source,
                )

            isLaunchRequest(normalized) ->
                ToolCall("launchApp", mapOf("appQuery" to normalizeAppQuery(extractAppQuery(prompt))), source)

            else -> null
        }
    }

    private fun containsAny(text: String, vararg needles: String): Boolean =
        needles.any { text.contains(it) }

    private fun isLaunchRequest(text: String): Boolean =
        containsAny(text, "launch", "open", "start", "run ", "running ", "실행", "열어", "켜", "틀어")

    private fun isDocumentRequest(text: String): Boolean {
        val wantsCreate = containsAny(
            text,
            "make",
            "create",
            "generate",
            "template",
            "만들",
            "작성",
            "생성",
            "템플릿",
        )
        val docType = containsAny(text, "ppt", "pptx", "pdf", "presentation", "slide", "슬라이드", "피피티")
        return wantsCreate && docType
    }

    private fun isScheduleRequest(text: String): Boolean =
        containsAny(text, "schedule", "later", "after", "every ", "daily", "예약", "뒤", "후", "마다", "매일") ||
            Regex("\\d+\\s*(minute|minutes|min|분|시간)\\b").containsMatchIn(text)

    private fun extractDelayMinutes(text: String): Long {
        val english = Regex("(\\d+)\\s*(minute|minutes|min|hour|hours)").find(text)
        if (english != null) {
            val amount = english.groupValues[1].toLongOrNull()?.coerceAtLeast(1L) ?: 1L
            return if (english.groupValues[2].startsWith("hour")) amount * 60L else amount
        }
        val korean = Regex("(\\d+)\\s*(분|시간)").find(text)
        if (korean != null) {
            val amount = korean.groupValues[1].toLongOrNull()?.coerceAtLeast(1L) ?: 1L
            return if (korean.groupValues[2] == "시간") amount * 60L else amount
        }
        return 1L
    }

    private fun extractAppQuery(prompt: String): String {
        var value = prompt
            .replace(Regex("\\band\\s+search\\b.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\d+\\s*(minute|minutes|min|hour|hours|분|시간)\\s*(뒤에|뒤|후에|후|later|after|in)?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b(launch|open|start|running|run|schedule|later|after|in)\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("(예약|실행하라|실행해줘|실행해|실행|열어줘|열어|켜줘|켜|틀어줘|틀어|뒤에|뒤|후에|후)"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        value = value.replace(Regex("\\d+\\s*(minute|minutes|min|hour|hours|분|시간)\\b.*", RegexOption.IGNORE_CASE), "").trim()
        return stripKoreanParticlesAndCommands(value).ifBlank { "settings" }
    }

    private fun normalizeAppQuery(query: String): String {
        val cleaned = stripKoreanParticlesAndCommands(query)
        val normalized = cleaned.lowercase()
        return when {
            normalized.contains("카메라") || normalized == "camera" -> "camera"
            normalized.contains("설정") || normalized == "settings" -> "settings"
            normalized.contains("유튜브") || normalized == "youtube" -> "youtube"
            normalized.contains("구글") || normalized == "google" -> "google"
            normalized.contains("크롬") || normalized == "chrome" -> "chrome"
            normalized.contains("캘린더") || normalized == "calendar" -> "calendar"
            normalized.contains("메일") || normalized.contains("이메일") || normalized == "email" -> "email"
            else -> cleaned
        }
    }

    private fun stripKoreanParticlesAndCommands(value: String): String =
        value
            .replace(Regex("(?<=\\S)(을|를|이|가|은|는|으로|로|에게|에)$"), "")
            .replace(Regex("\\b(해|줘|해줘|켜줘|열어줘)$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun extractClickText(prompt: String): String =
        prompt.substringAfter("click", "")
            .substringAfter("tap", "")
            .substringAfter("클릭", "")
            .substringAfter("눌러", "")
            .trim()
            .ifBlank { prompt }

    private fun extractFileTarget(prompt: String): String =
        prompt.substringAfter("open file", prompt)
            .replace("open", "", ignoreCase = true)
            .replace("file", "", ignoreCase = true)
            .replace("파일", "")
            .replace("열어", "")
            .trim()
            .ifBlank { prompt.trim() }

    private fun extractDocumentFormat(normalized: String): String =
        if (normalized.contains("pdf")) "pdf" else "pptx"

    private fun extractSlideCount(normalized: String): Int {
        val match = Regex("(\\d+)\\s*(slides|slide|pages|page|장|페이지)").find(normalized)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 6
    }

    private fun extractDocumentTopic(prompt: String): String {
        val markers = listOf("about ", "주제로", "주제의", "바탕으로")
        val afterTopicMarker = markers.firstNotNullOfOrNull { marker ->
            prompt.substringAfter(marker, missingDelimiterValue = "").takeIf { it.isNotBlank() }
        } ?: prompt
        val cleaned = afterTopicMarker
            .replace(Regex("\\b(make|create|generate|template|pptx|ppt|pdf|presentation|slide|slides)\\b", RegexOption.IGNORE_CASE), "")
            .replace("만들어줘", "")
            .replace("만들어 줘", "")
            .replace("작성해줘", "")
            .replace("생성해줘", "")
            .replace("템플릿", "")
            .replace("피피티", "")
            .replace("슬라이드", "")
            .replace(Regex("\\d+\\s*(slides|slide|pages|page|장|페이지)", RegexOption.IGNORE_CASE), "")
            .trim()
        return cleaned.ifBlank { prompt.trim() }
    }

    private fun extractCalendarTitle(prompt: String): String =
        prompt.replace("add", "", ignoreCase = true)
            .replace("calendar", "", ignoreCase = true)
            .replace("event", "", ignoreCase = true)
            .replace("일정", "")
            .replace("추가", "")
            .replace("캘린더", "")
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
        val repeatInterval = extractRepeatIntervalMinutes(normalized)
        val dailyTime = extractDailyTime(normalized)
        val withoutSchedule = prompt
            .replace(Regex("schedule", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bevery\\s+\\d+\\s*(minute|minutes|min|hour|hours)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bevery\\s+day\\s*(at\\s+\\d{1,2}(:\\d{2})?)?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bdaily\\s*(at\\s+\\d{1,2}(:\\d{2})?)?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bafter\\s+\\d+\\s*(minute|minutes|min|hour|hours)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bin\\s+\\d+\\s*(minute|minutes|min|hour|hours)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\d+\\s*(minute|minutes|min|hour|hours|분|시간)\\s*(뒤에|뒤|후에|후|마다)?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b(later|after|in)\\b", RegexOption.IGNORE_CASE), "")
            .replace("예약", "")
            .replace("매일", "")
            .trim()
        val nested = if (isScheduleRequest(withoutSchedule.lowercase())) null else plan(withoutSchedule, source = source)
        return if (nested != null && nested.name != "scheduleLaunch") {
            val args = buildMap {
                put("toolName", nested.name)
                applyScheduleArgs(delay, repeatInterval, dailyTime)
                nested.arguments.forEach { (key, value) -> put("arg_$key", value) }
            }
            ToolCall("scheduleLaunch", args, source)
        } else {
            ToolCall(
                name = "scheduleLaunch",
                arguments = buildMap {
                    put("appQuery", normalizeAppQuery(extractAppQuery(withoutSchedule)))
                    applyScheduleArgs(delay, repeatInterval, dailyTime)
                },
                source = source,
            )
        }
    }

    private fun MutableMap<String, String>.applyScheduleArgs(
        delay: String,
        repeatInterval: Long?,
        dailyTime: Pair<Int, Int>?,
    ) {
        when {
            repeatInterval != null -> put("repeatIntervalMinutes", repeatInterval.toString())
            dailyTime != null -> {
                put("dailyHour", dailyTime.first.toString())
                put("dailyMinute", dailyTime.second.toString())
            }

            else -> put("delayMinutes", delay)
        }
    }

    private fun extractRepeatIntervalMinutes(text: String): Long? {
        val english = Regex("\\bevery\\s+(\\d+)\\s*(minute|minutes|min|hour|hours)\\b").find(text)
        if (english != null) {
            val amount = english.groupValues[1].toLongOrNull() ?: return null
            return if (english.groupValues[2].startsWith("hour")) amount * 60L else amount
        }
        val korean = Regex("(\\d+)\\s*(분|시간)\\s*마다").find(text) ?: return null
        val amount = korean.groupValues[1].toLongOrNull() ?: return null
        return if (korean.groupValues[2] == "시간") amount * 60L else amount
    }

    private fun extractDailyTime(text: String): Pair<Int, Int>? {
        if (!containsAny(text, "daily", "every day", "매일")) return null
        val english = Regex("\\b(?:daily|every\\s+day)\\s*(?:at\\s*)?(\\d{1,2})(?::(\\d{2}))?").find(text)
        val korean = Regex("매일\\s*(?:(\\d{1,2})시)?\\s*(?:(\\d{1,2})분)?").find(text)
        val hour = english?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: korean?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }?.toIntOrNull()
            ?: 9
        val minute = english?.groupValues?.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull()
            ?: korean?.groupValues?.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull()
            ?: 0
        return hour.coerceIn(0, 23) to minute.coerceIn(0, 59)
    }
}
