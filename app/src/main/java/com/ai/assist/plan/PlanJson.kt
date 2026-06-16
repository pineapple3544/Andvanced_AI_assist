package com.ai.assist.plan

import com.ai.assist.domain.ToolCall
import org.json.JSONArray
import org.json.JSONObject

object PlanJson {
    fun encode(items: List<PlanItem>): String {
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        return array.toString()
    }

    fun decode(raw: String): List<PlanItem> {
        if (raw.isBlank()) return emptyList()
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toPlanItem())
            }
        }
    }

    private fun PlanItem.toJson(): JSONObject {
        val args = JSONObject()
        toolCall.arguments.forEach { (key, value) -> args.put(key, value) }
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("toolName", toolCall.name)
            .put("toolSource", toolCall.source)
            .put("arguments", args)
            .put("scheduledAtMillis", scheduledAtMillis)
            .put("status", status.name)
            .put("createdAtMillis", createdAtMillis)
            .put("lastRunAtMillis", lastRunAtMillis ?: JSONObject.NULL)
            .put("lastResult", lastResult ?: JSONObject.NULL)
    }

    private fun JSONObject.toPlanItem(): PlanItem {
        val argsJson = getJSONObject("arguments")
        val args = buildMap {
            argsJson.keys().forEach { key -> put(key, argsJson.optString(key)) }
        }
        return PlanItem(
            id = getString("id"),
            title = getString("title"),
            toolCall = ToolCall(
                name = getString("toolName"),
                arguments = args,
                source = optString("toolSource", "plan"),
            ),
            scheduledAtMillis = getLong("scheduledAtMillis"),
            status = runCatching { PlanStatus.valueOf(getString("status")) }.getOrDefault(PlanStatus.Pending),
            createdAtMillis = getLong("createdAtMillis"),
            lastRunAtMillis = if (isNull("lastRunAtMillis")) null else getLong("lastRunAtMillis"),
            lastResult = if (isNull("lastResult")) null else getString("lastResult"),
        )
    }
}
