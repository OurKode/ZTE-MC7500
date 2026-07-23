package com.example.odumonitor.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.odumonitor.data.repository.OduRepository
import com.example.odumonitor.widget.OduCompactWidget
import com.example.odumonitor.widget.OduDetailedWidget

class OduSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = OduRepository(appContext)
            repository.fetchCurrentSignalOnce()
            
            // Trigger Glance widget redraw
            OduCompactWidget().updateAll(appContext)
            OduDetailedWidget().updateAll(appContext)
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
