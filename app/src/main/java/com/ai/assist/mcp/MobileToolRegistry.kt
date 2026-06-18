package com.ai.assist.mcp

object MobileToolRegistry {
    val descriptions: String = """
        - listInstalledApps(): list installed Android launchable apps.
        - launchApp(appQuery: string): launch an installed Android app by label or package.
        - getDeviceCapability(): describe Android version, model, RAM, and ABI.
        - openFile(pathOrUri: string, mimeHint?: string): open a document or media file through Android apps.
        - createDocument(topic: string, format: string, style?: string, slideCount?: string): approval required, create a lightweight editable PPTX or slide-style PDF template on the phone.
        - addCalendarEvent(title: string, startMillis?: string, endMillis?: string, location?: string, description?: string): open Android calendar event editor.
        - composeEmail(to?: string, subject?: string, body?: string): open email draft editor.
        - summarizeVisibleScreen(): approval required, summarize text currently visible through Accessibility.
        - performAppMacro(steps: string): approval required, run limited Accessibility macro steps as JSON array or "action:value; action:value".
        - scheduleLaunch(appQuery?: string, toolName?: string, delayMinutes?: string, repeatIntervalMinutes?: string, dailyHour?: string, dailyMinute?: string, arg_title?: string, arg_to?: string, arg_subject?: string, arg_body?: string): approval required, add one-time, interval, or daily app-owned Plan.
        - cancelScheduledAction(): approval required, cancel scheduled actions.
        - runAccessibilityAction(action: string, text?: string): approval required, actions are back, home, scrollForward, clickText.
    """.trimIndent()
}
