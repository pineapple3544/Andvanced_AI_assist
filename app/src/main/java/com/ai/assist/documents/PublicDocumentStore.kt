package com.ai.assist.documents

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream

data class PublishedDocument(
    val openPath: String,
    val displayLocation: String,
)

class PublicDocumentStore(private val context: Context) {
    fun publish(source: File, format: DocumentFormat): PublishedDocument {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishToMediaStore(source, format)?.let { return it }
        }
        return PublishedDocument(
            openPath = source.absolutePath,
            displayLocation = source.absolutePath,
        )
    }

    private fun publishToMediaStore(source: File, format: DocumentFormat): PublishedDocument? {
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/AI Assist"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, source.name)
            put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Files.getContentUri("external")
        val uri = resolver.insert(collection, values) ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                FileInputStream(source).use { input -> input.copyTo(output) }
            } ?: error("Could not open MediaStore output stream.")
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null,
            )
            PublishedDocument(
                openPath = uri.toString(),
                displayLocation = "$relativePath/${source.name}",
            )
        }.getOrElse {
            resolver.delete(uri, null, null)
            null
        }
    }
}
