package com.ai.assist.plan

import android.content.Context
import com.ai.assist.domain.ToolCall
import java.util.UUID

class PlanRepository(context: Context) {
    private val prefs = context.getSharedPreferences("plans", Context.MODE_PRIVATE)

    fun list(): List<PlanItem> =
        PlanJson.decode(prefs.getString(KEY_ITEMS, "") ?: "")
            .sortedByDescending { it.createdAtMillis }

    fun get(id: String): PlanItem? = list().firstOrNull { it.id == id }

    fun addLaunchPlan(appQuery: String, delayMinutes: Long): PlanItem {
        return addPlan(
            title = "Launch $appQuery",
            toolCall = ToolCall("launchApp", mapOf("appQuery" to appQuery), source = "plan"),
            delayMinutes = delayMinutes,
        )
    }

    fun addPlan(title: String, toolCall: ToolCall, delayMinutes: Long): PlanItem {
        val now = System.currentTimeMillis()
        val plan = PlanItem(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { toolCall.name },
            toolCall = toolCall.copy(source = "plan"),
            scheduledAtMillis = now + delayMinutes.coerceAtLeast(1L) * 60_000L,
            status = PlanStatus.Pending,
            createdAtMillis = now,
        )
        upsert(plan)
        return plan
    }

    fun upsert(item: PlanItem) {
        val items = list().filterNot { it.id == item.id } + item
        save(items)
    }

    fun markRunning(id: String) {
        get(id)?.let {
            upsert(it.copy(status = PlanStatus.Running, lastRunAtMillis = System.currentTimeMillis()))
        }
    }

    fun markResult(id: String, success: Boolean, message: String) {
        get(id)?.let {
            upsert(
                it.copy(
                    status = if (success) PlanStatus.Succeeded else PlanStatus.Failed,
                    lastRunAtMillis = System.currentTimeMillis(),
                    lastResult = message,
                ),
            )
        }
    }

    fun cancel(id: String) {
        get(id)?.let { upsert(it.copy(status = PlanStatus.Canceled, lastResult = "Canceled by user.")) }
    }

    fun delete(id: String) {
        save(list().filterNot { it.id == id })
    }

    fun cancelAll(): Int {
        val items = list()
        save(items.map { it.copy(status = PlanStatus.Canceled, lastResult = "Canceled by user.") })
        return items.count { it.status == PlanStatus.Pending || it.status == PlanStatus.Running }
    }

    private fun save(items: List<PlanItem>) {
        prefs.edit().putString(KEY_ITEMS, PlanJson.encode(items)).apply()
    }

    private companion object {
        const val KEY_ITEMS = "items"
    }
}
