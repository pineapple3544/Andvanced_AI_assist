package com.ai.assist.scheduler

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.UUID
import java.util.concurrent.TimeUnit

class Scheduler(private val context: Context) {
    fun scheduleLaunch(appQuery: String, delayMinutes: Long): UUID {
        val request = OneTimeWorkRequestBuilder<ScheduledLaunchWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(ScheduledLaunchWorker.KEY_APP_QUERY to appQuery))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("launch-$appQuery-${request.id}", ExistingWorkPolicy.REPLACE, request)
        ScheduledActionStore(context).add(
            request.id.toString(),
            "Launch '$appQuery' in $delayMinutes minute(s).",
        )
        return request.id
    }

    fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWork()
    }
}
