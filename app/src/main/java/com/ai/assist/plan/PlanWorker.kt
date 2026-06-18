package com.ai.assist.plan

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ai.assist.tools.ToolRouter

class PlanWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val planId = inputData.getString(KEY_PLAN_ID).orEmpty()
        val repository = PlanRepository(applicationContext)
        val plan = repository.get(planId) ?: return Result.failure()
        if (plan.status != PlanStatus.Pending) return Result.success()

        repository.markRunning(planId)
        val result = ToolRouter(applicationContext).execute(plan.toolCall)
        val updated = repository.markRunResultAndReschedule(planId, result.success, result.message)
        if (updated != null && updated.status == PlanStatus.Pending && updated.scheduleType != ScheduleType.Once) {
            PlanScheduler(applicationContext).schedule(updated)
        }
        return if (result.success) Result.success() else Result.failure()
    }

    companion object {
        const val KEY_PLAN_ID = "planId"
    }
}
