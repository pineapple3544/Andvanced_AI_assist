package com.ai.assist.tools

object AppAliasRegistry {
    private val aliases = mapOf(
        "camera" to listOf(
            "com.sec.android.app.camera",
            "com.google.android.GoogleCamera",
            "com.android.camera2",
            "com.android.camera",
        ),
        "settings" to listOf("com.android.settings"),
        "youtube" to listOf("com.google.android.youtube"),
        "chrome" to listOf("com.android.chrome"),
        "calendar" to listOf(
            "com.google.android.calendar",
            "com.samsung.android.calendar",
            "com.android.calendar",
        ),
        "email" to listOf(
            "com.google.android.gm",
            "com.samsung.android.email.provider",
            "com.android.email",
        ),
        "gmail" to listOf("com.google.android.gm"),
    )

    fun packageCandidates(query: String): List<String> {
        val normalized = query.trim().lowercase()
        return aliases.entries.firstOrNull { (alias, _) ->
            normalized == alias || normalized.contains(alias)
        }?.value.orEmpty()
    }
}
