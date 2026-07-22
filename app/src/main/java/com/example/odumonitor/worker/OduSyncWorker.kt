package com.example.odumonitor.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.odumonitor.data.repository.OduRepository

class OduSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository = OduRepository()

    override suspend fun doWork(): Result {
        return try {
            val signalState = repository.fetchCurrentSignalOnce()
            // In a production setup, notify Glance AppWidgetManager update or local alert manager if thresholds fail
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
