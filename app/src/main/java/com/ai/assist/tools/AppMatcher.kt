package com.ai.assist.tools

data class AppMatch(
    val app: InstalledApp,
    val score: Int,
    val reason: String,
)

object AppMatcher {
    fun bestMatch(query: String, apps: List<InstalledApp>): AppMatch? {
        val cleanedQuery = cleanQuery(query)
        if (cleanedQuery.isBlank()) {
            return apps.firstOrNull { it.packageName == "com.android.settings" }
                ?.let { AppMatch(it, 100, "blank query fallback") }
        }
        return apps.asSequence()
            .mapNotNull { app -> score(cleanedQuery, app) }
            .filter { it.score >= 45 }
            .sortedWith(compareByDescending<AppMatch> { it.score }.thenBy { it.app.label.length })
            .firstOrNull()
    }

    private fun score(query: String, app: InstalledApp): AppMatch? {
        val aliases = queryAliases(query)
        val label = normalize(app.label)
        val packageName = normalize(app.packageName)
        val packageTokens = app.packageName.lowercase().split('.', '_', '-').filter { it.isNotBlank() }
        var bestScore = 0
        var reason = "no match"

        for (alias in aliases) {
            val normalizedAlias = normalize(alias)
            val appAliasPackages = AppAliasRegistry.packageCandidates(alias)
            when {
                appAliasPackages.any { it.equals(app.packageName, ignoreCase = true) } -> {
                    bestScore = maxOf(bestScore, 120)
                    reason = "known alias package"
                }

                packageName == normalizedAlias -> {
                    bestScore = maxOf(bestScore, 110)
                    reason = "exact package"
                }

                label == normalizedAlias -> {
                    bestScore = maxOf(bestScore, 100)
                    reason = "exact label"
                }

                packageTokens.any { it == normalizedAlias } -> {
                    bestScore = maxOf(bestScore, 92)
                    reason = "exact package token"
                }

                label.startsWith(normalizedAlias) -> {
                    bestScore = maxOf(bestScore, 82)
                    reason = "label prefix"
                }

                label.contains(normalizedAlias) -> {
                    val penalty = if (label.length > normalizedAlias.length + 9) 18 else 0
                    bestScore = maxOf(bestScore, 72 - penalty)
                    reason = "label contains"
                }

                packageName.contains(normalizedAlias) -> {
                    bestScore = maxOf(bestScore, 66)
                    reason = "package contains"
                }

                levenshtein(label, normalizedAlias) <= 2 && normalizedAlias.length >= 4 -> {
                    bestScore = maxOf(bestScore, 58)
                    reason = "fuzzy label"
                }
            }
        }

        val adjusted = applyPenalties(query, app, bestScore)
        return if (adjusted > 0) AppMatch(app, adjusted, reason) else null
    }

    private fun applyPenalties(query: String, app: InstalledApp, score: Int): Int {
        var adjusted = score
        val label = normalize(app.label)
        val packageName = app.packageName.lowercase()
        val queryAliases = queryAliases(query).map { normalize(it) }
        if (queryAliases.contains("camera") && label.contains("assistant")) adjusted -= 45
        if (queryAliases.contains("camera") && packageName.contains("assistant")) adjusted -= 45
        if (queryAliases.contains("settings") && !packageName.contains("settings")) adjusted -= 25
        return adjusted
    }

    private fun cleanQuery(query: String): String =
        query.lowercase()
            .replace(Regex("\\b(launch|open|start|run|running|schedule|after|later|in)\\b"), " ")
            .replace(Regex("\\d+\\s*(minute|minutes|min|hour|hours|분|시간)\\s*(뒤에|뒤|후에|후|마다)?"), " ")
            .replace(Regex("(실행하라|실행해|실행|열어줘|열어|켜줘|켜|예약|뒤에|뒤|후에|후|마다)"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun queryAliases(query: String): List<String> {
        val cleaned = cleanQuery(query)
        val aliases = buildList {
            add(cleaned)
            when {
                cleaned.contains("카메라") -> add("camera")
                cleaned.contains("설정") -> add("settings")
                cleaned.contains("유튜브") -> add("youtube")
                cleaned.contains("크롬") -> add("chrome")
                cleaned.contains("캘린더") -> add("calendar")
                cleaned.contains("메일") || cleaned.contains("이메일") -> add("email")
            }
        }
        return aliases.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9가-힣]+"), "")
            .trim()

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val previous = IntArray(b.length + 1) { it }
        val current = IntArray(b.length + 1)
        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + cost,
                )
            }
            for (j in previous.indices) previous[j] = current[j]
        }
        return previous[b.length]
    }
}
