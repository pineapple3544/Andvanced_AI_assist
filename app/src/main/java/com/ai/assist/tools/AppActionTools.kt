package com.ai.assist.tools

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.ai.assist.domain.ToolResult
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AppActionTools(private val context: Context) {
    fun searchWeb(query: String, provider: String? = null): ToolResult {
        if (query.isBlank()) return ToolResult(false, "Search query is empty.")
        val encoded = encode(query)
        val uri = when (provider?.lowercase()) {
            "youtube" -> Uri.parse("https://www.youtube.com/results?search_query=$encoded")
            else -> Uri.parse("https://www.google.com/search?q=$encoded")
        }
        return start(Intent(Intent.ACTION_VIEW, uri), "Started web search for '$query'.", allowChooser = false)
    }

    fun searchYouTube(query: String): ToolResult {
        if (query.isBlank()) return ToolResult(false, "YouTube search query is empty.")
        val encoded = encode(query)
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://results?search_query=$encoded"))
            .setPackage("com.google.android.youtube")
        val appResult = start(appIntent, "Started YouTube app search for '$query'.", allowChooser = false)
        return if (appResult.success) {
            appResult
        } else {
            start(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")),
                "Started YouTube web search for '$query'.",
                allowChooser = false,
            )
        }
    }

    fun addCalendarEvent(
        title: String,
        startMillis: Long?,
        endMillis: Long?,
        location: String?,
        description: String?,
    ): ToolResult {
        if (title.isBlank()) return ToolResult(false, "Calendar title is empty.")
        val start = startMillis ?: System.currentTimeMillis() + 60 * 60 * 1000L
        val end = endMillis ?: start + 60 * 60 * 1000L
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, location.orEmpty())
            .putExtra(CalendarContract.Events.DESCRIPTION, description.orEmpty())
        return start(intent, "Opened calendar event editor for '$title'.")
    }

    fun composeEmail(to: String?, subject: String?, body: String?): ToolResult {
        val intent = Intent(Intent.ACTION_SENDTO)
            .setData(Uri.parse("mailto:"))
            .putExtra(Intent.EXTRA_EMAIL, to?.takeIf { it.isNotBlank() }?.let { arrayOf(it) } ?: emptyArray<String>())
            .putExtra(Intent.EXTRA_SUBJECT, subject.orEmpty())
            .putExtra(Intent.EXTRA_TEXT, body.orEmpty())
        return start(intent, "Opened email draft.")
    }

    private fun start(intent: Intent, successMessage: String, allowChooser: Boolean = true): ToolResult {
        val launchIntent = if (allowChooser) Intent.createChooser(intent, "Open with") else intent
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            ToolResult(true, successMessage)
        } catch (error: ActivityNotFoundException) {
            ToolResult(false, "No app can handle this action.")
        } catch (error: SecurityException) {
            ToolResult(false, "Android denied this action: ${error.message}")
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
