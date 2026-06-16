package com.ai.assist.scheduler

import android.content.Context

class ScheduledActionStore(context: Context) {
    private val prefs = context.getSharedPreferences("scheduled_actions", Context.MODE_PRIVATE)

    fun add(id: String, description: String) {
        prefs.edit().putString(id, description).apply()
    }

    fun list(): List<String> = prefs.all.values.map { it.toString() }

    fun clearAll(): Int {
        val count = prefs.all.size
        prefs.edit().clear().apply()
        return count
    }
}
