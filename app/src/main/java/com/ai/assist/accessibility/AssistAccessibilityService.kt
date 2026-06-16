package com.ai.assist.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ai.assist.domain.ToolResult

class AssistAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun performBack(): ToolResult =
        ToolResult(performGlobalAction(GLOBAL_ACTION_BACK), "Requested global Back action.")

    fun performHome(): ToolResult =
        ToolResult(performGlobalAction(GLOBAL_ACTION_HOME), "Requested global Home action.")

    fun performScrollForward(): ToolResult {
        val node = rootInActiveWindow ?: return ToolResult(false, "No active window to scroll.")
        val scrollable = findFirst(node) { it.isScrollable }
        return if (scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true) {
            ToolResult(true, "Scrolled the active window forward.")
        } else {
            ToolResult(false, "No scrollable node found.")
        }
    }

    fun clickText(text: String): ToolResult {
        if (text.isBlank()) return ToolResult(false, "Text to click is empty.")
        val root = rootInActiveWindow ?: return ToolResult(false, "No active window.")
        val target = findFirst(root) { node ->
            val label = node.text?.toString().orEmpty() + " " + node.contentDescription?.toString().orEmpty()
            label.contains(text, ignoreCase = true) && node.isClickable
        }
        return if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
            ToolResult(true, "Clicked text matching '$text'.")
        } else {
            ToolResult(false, "Could not find clickable text '$text'.")
        }
    }

    fun collectVisibleText(maxChars: Int = 4000): ToolResult {
        val root = rootInActiveWindow ?: return ToolResult(false, "No active window.")
        val texts = mutableListOf<String>()
        collectText(root, texts)
        val joined = texts.distinct()
            .joinToString(separator = "\n")
            .take(maxChars)
        return if (joined.isBlank()) {
            ToolResult(false, "No visible text was found.")
        } else {
            ToolResult(true, joined)
        }
    }

    fun inputText(text: String): ToolResult {
        val root = rootInActiveWindow ?: return ToolResult(false, "No active window.")
        val editable = findFirst(root) { it.isEditable }
            ?: return ToolResult(false, "No editable field found.")
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return if (editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
            ToolResult(true, "Input text into focused editable field.")
        } else {
            ToolResult(false, "Failed to input text.")
        }
    }

    fun runMacro(steps: List<MacroStep>): ToolResult {
        if (steps.isEmpty()) return ToolResult(false, "Macro has no steps.")
        if (steps.size > 8) return ToolResult(false, "Macro step limit is 8.")
        val logs = mutableListOf<String>()
        steps.forEachIndexed { index, step ->
            val result = when (step.action) {
                "clickText" -> clickText(step.value)
                "inputText" -> inputText(step.value)
                "scrollForward" -> performScrollForward()
                "back" -> performBack()
                "home" -> performHome()
                "wait" -> {
                    Thread.sleep(step.value.toLongOrNull()?.coerceIn(100L, 3000L) ?: 500L)
                    ToolResult(true, "Waited.")
                }

                else -> ToolResult(false, "Unsupported macro step '${step.action}'.")
            }
            logs.add("${index + 1}. ${step.action}: ${result.message}")
            if (!result.success) return ToolResult(false, logs.joinToString("\n"))
        }
        return ToolResult(true, logs.joinToString("\n"))
    }

    private fun collectText(node: AccessibilityNodeInfo, output: MutableList<String>) {
        if (!node.isPassword && !node.isEditable) {
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let(output::add)
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(output::add)
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            collectText(child, output)
        }
    }

    private fun findFirst(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val match = findFirst(child, predicate)
            if (match != null) return match
        }
        return null
    }

    companion object {
        @Volatile
        var instance: AssistAccessibilityService? = null
            private set
    }
}

data class MacroStep(
    val action: String,
    val value: String = "",
)
