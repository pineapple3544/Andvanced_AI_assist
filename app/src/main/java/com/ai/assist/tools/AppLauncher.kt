package com.ai.assist.tools

import android.content.Context
import android.content.Intent
import com.ai.assist.domain.ToolResult

data class InstalledApp(
    val label: String,
    val packageName: String,
)

class AppLauncher(private val context: Context) {
    fun listInstalledApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        return packageManager.getInstalledApplications(0)
            .map {
                InstalledApp(
                    label = it.loadLabel(packageManager).toString(),
                    packageName = it.packageName,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launch(query: String): ToolResult {
        val match = findApp(query)
        val target = match?.app
            ?: return ToolResult(false, "No installed app matched '$query'.")
        val intent = context.packageManager.getLaunchIntentForPackage(target.packageName)
            ?: return ToolResult(false, "${target.label} does not expose a launch intent.")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ToolResult(true, "Launched ${target.label} (${target.packageName}). Match score ${match.score}: ${match.reason}.")
    }

    private fun findApp(query: String): AppMatch? {
        val apps = listInstalledApps()
        return AppMatcher.bestMatch(query, apps)
    }
}
