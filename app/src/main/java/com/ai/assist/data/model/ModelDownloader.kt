package com.ai.assist.data.model

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import java.io.File

class ModelDownloader(private val context: Context) {
    fun enqueue(candidate: ModelCandidate): Long {
        require(candidate.url.isNotBlank()) { "Model URL is empty." }
        val destination = destinationFor(candidate)
        val request = DownloadManager.Request(Uri.parse(candidate.url))
            .setTitle(candidate.name)
            .setDescription(candidate.description)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                destination.name,
            )
        val manager = context.getSystemService(DownloadManager::class.java)
        return manager.enqueue(request)
    }

    fun query(downloadId: Long): DownloadStatus {
        val manager = context.getSystemService(DownloadManager::class.java)
        val cursor = manager.query(DownloadManager.Query().setFilterById(downloadId))
            ?: return DownloadStatus.Unknown(downloadId, "DownloadManager returned no cursor.")
        cursor.use {
            if (!it.moveToFirst()) {
                return DownloadStatus.Unknown(downloadId, "No DownloadManager row exists for id $downloadId.")
            }
            val status = it.intValue(DownloadManager.COLUMN_STATUS)
            val reason = it.intValue(DownloadManager.COLUMN_REASON)
            val downloaded = it.longValue(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val total = it.longValue(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val localUri = it.stringValue(DownloadManager.COLUMN_LOCAL_URI)
            return when (status) {
                DownloadManager.STATUS_PENDING -> DownloadStatus.Running(downloadId, "Pending", downloaded, total)
                DownloadManager.STATUS_PAUSED -> DownloadStatus.Running(downloadId, "Paused: ${reasonText(reason)}", downloaded, total)
                DownloadManager.STATUS_RUNNING -> DownloadStatus.Running(downloadId, "Running", downloaded, total)
                DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.Success(downloadId, localUri.orEmpty())
                DownloadManager.STATUS_FAILED -> DownloadStatus.Failed(downloadId, reasonText(reason), downloaded, total)
                else -> DownloadStatus.Unknown(downloadId, "Unknown status $status, reason $reason.")
            }
        }
    }

    fun destinationFor(candidate: ModelCandidate): File {
        val fileName = candidate.fileName()
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        return File(dir, fileName)
    }

    private fun ModelCandidate.fileName(): String {
        val fromUrl = Uri.parse(url).lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.endsWith(".litertlm") || it.endsWith(".task") }
        return fromUrl ?: "${name.filter { it.isLetterOrDigit() || it == '-' || it == '_' }}.litertlm"
    }

    private fun reasonText(reason: Int): String = when (reason) {
        DownloadManager.ERROR_CANNOT_RESUME -> "cannot resume"
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "destination device not found"
        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "file already exists"
        DownloadManager.ERROR_FILE_ERROR -> "file error"
        DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "insufficient storage space"
        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "too many redirects"
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "unhandled HTTP status code"
        DownloadManager.ERROR_UNKNOWN -> "unknown download error"
        DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "queued for Wi-Fi"
        DownloadManager.PAUSED_UNKNOWN -> "paused for unknown reason"
        DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "waiting for network"
        DownloadManager.PAUSED_WAITING_TO_RETRY -> "waiting to retry"
        else -> "reason code $reason"
    }

    private fun Cursor.intValue(column: String): Int = getInt(getColumnIndexOrThrow(column))

    private fun Cursor.longValue(column: String): Long = getLong(getColumnIndexOrThrow(column))

    private fun Cursor.stringValue(column: String): String? = getString(getColumnIndexOrThrow(column))
}

sealed interface DownloadStatus {
    val id: Long

    data class Running(
        override val id: Long,
        val state: String,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : DownloadStatus

    data class Success(
        override val id: Long,
        val localUri: String,
    ) : DownloadStatus

    data class Failed(
        override val id: Long,
        val reason: String,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : DownloadStatus

    data class Unknown(
        override val id: Long,
        val message: String,
    ) : DownloadStatus
}
