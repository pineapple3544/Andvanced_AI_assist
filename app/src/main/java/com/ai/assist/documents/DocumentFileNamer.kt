package com.ai.assist.documents

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentFileNamer {
    fun generatedDir(context: Context): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "generated").apply { mkdirs() }

    fun outputFile(
        context: Context,
        topic: String,
        format: DocumentFormat,
        nowMillis: Long = System.currentTimeMillis(),
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(nowMillis))
        return File(generatedDir(context), "ai-assist-$timestamp-${safeTopic(topic)}.${format.extension}")
    }

    fun safeTopic(topic: String): String {
        val normalized = topic
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(40)
            .trim('-')
        return normalized.ifBlank { "document" }
    }
}
