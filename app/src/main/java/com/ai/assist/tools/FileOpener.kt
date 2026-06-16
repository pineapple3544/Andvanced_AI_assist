package com.ai.assist.tools

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ai.assist.domain.ToolResult
import java.io.File

class FileOpener(private val context: Context) {
    fun open(pathOrUri: String, mimeHint: String? = null): ToolResult {
        if (pathOrUri.isBlank()) {
            return ToolResult(false, "File path or URI is empty.")
        }

        val uri = when {
            pathOrUri.startsWith("content://") -> Uri.parse(pathOrUri)
            pathOrUri.startsWith("file://") -> uriForFile(File(Uri.parse(pathOrUri).path.orEmpty()))
            else -> uriForFile(resolveFile(pathOrUri))
        } ?: return ToolResult(false, "File does not exist or is not accessible: $pathOrUri")

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeHint?.takeIf { it.isNotBlank() } ?: "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            context.startActivity(Intent.createChooser(intent, "Open with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ToolResult(true, "Opened file with Android app resolver: $pathOrUri")
        } catch (error: ActivityNotFoundException) {
            ToolResult(false, "No app can open this file type.")
        } catch (error: SecurityException) {
            ToolResult(false, "Android denied access to this file: ${error.message}")
        }
    }

    fun canResolve(pathOrUri: String): Boolean {
        if (pathOrUri.startsWith("content://")) return true
        val file = if (pathOrUri.startsWith("file://")) {
            File(Uri.parse(pathOrUri).path.orEmpty())
        } else {
            resolveFile(pathOrUri)
        }
        return file.exists() && file.canRead()
    }

    private fun uriForFile(file: File): Uri? {
        if (!file.exists() || !file.canRead()) return null
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun resolveFile(path: String): File {
        val raw = File(path)
        if (raw.isAbsolute) return raw
        val externalDownload = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), path)
        if (externalDownload.exists()) return externalDownload
        return File(context.filesDir, path)
    }
}
