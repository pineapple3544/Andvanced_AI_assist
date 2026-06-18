package com.ai.assist.plan

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class PlanScheduler(private val context: Context) {
    fun schedule(plan: PlanItem) {
        val delayMillis = (plan.scheduledAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<PlanWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(PlanWorker.KEY_PLAN_ID to plan.id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(plan.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(planId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(planId))
    }

    fun cancelAll(plans: List<PlanItem>) {
        plans.forEach { cancel(it.id) }
    }

    fun rescheduleActivePlans(repository: PlanRepository = PlanRepository(context)) {
        repository.activePlans().forEach { schedule(it) }
    }

    private fun workName(planId: String): String = "plan-$planId"
}
