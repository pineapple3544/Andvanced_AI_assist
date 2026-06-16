package com.ai.assist.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ai.assist.tools.AppLauncher

class ScheduledLaunchWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val query = inputData.getString(KEY_APP_QUERY).orEmpty()
        val result = AppLauncher(applicationContext).launch(query)
        return if (result.success) Result.success() else Result.failure()
    }

    companion object {
        const val KEY_APP_QUERY = "appQuery"
    }
}
