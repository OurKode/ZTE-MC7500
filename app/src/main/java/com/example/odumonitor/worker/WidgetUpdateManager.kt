package com.example.odumonitor.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WidgetUpdateManager {
    private const val WORK_NAME = "OduWidgetSyncWork"
    private const val IMMEDIATE_WORK_NAME = "OduWidgetImmediateSyncWork"

    fun scheduleWidgetUpdates(context: Context, intervalMinutes: Int) {
        val workManager = WorkManager.getInstance(context)
        if (intervalMinutes <= 0) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        // WorkManager minimum periodic interval is 15 minutes
        val safeInterval = intervalMinutes.coerceAtLeast(15)
        val request = PeriodicWorkRequestBuilder<OduSyncWorker>(
            safeInterval.toLong(),
            TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        // Trigger immediate sync so widget updates immediately without waiting for interval
        triggerImmediateSync(context)
    }

    fun triggerImmediateSync(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val immediateRequest = OneTimeWorkRequestBuilder<OduSyncWorker>().build()
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
    }
}
