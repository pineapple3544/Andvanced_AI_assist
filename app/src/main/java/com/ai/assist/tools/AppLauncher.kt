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
        val target = findApp(query)
            ?: return ToolResult(false, "No installed app matched '$query'.")
        val intent = context.packageManager.getLaunchIntentForPackage(target.packageName)
            ?: return ToolResult(false, "${target.label} does not expose a launch intent.")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ToolResult(true, "Launched ${target.label}.")
    }

    private fun findApp(query: String): InstalledApp? {
        val normalized = query.trim().lowercase()
        val apps = listInstalledApps()
        return apps.firstOrNull { it.packageName.equals(normalized, ignoreCase = true) }
            ?: apps.firstOrNull { it.label.lowercase() == normalized }
            ?: apps.firstOrNull {
                it.label.lowercase().contains(normalized) || it.packageName.lowercase().contains(normalized)
            }
            ?: apps.firstOrNull { normalized.isBlank() && it.packageName == "com.android.settings" }
    }
}
