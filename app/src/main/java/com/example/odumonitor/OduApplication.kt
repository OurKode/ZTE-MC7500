package com.example.odumonitor

import android.app.Application
import com.example.odumonitor.data.local.WidgetPreferences
import com.example.odumonitor.notification.OduNotificationManager
import com.example.odumonitor.service.OduMonitorService
import com.example.odumonitor.worker.WidgetUpdateManager

class OduApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = WidgetPreferences(this)
        
        // Initialize notification channels
        OduNotificationManager(this)

        // Schedule WorkManager updates for widgets
        val widgetConfig = prefs.getWidgetConfig()
        WidgetUpdateManager.scheduleWidgetUpdates(this, widgetConfig.updateIntervalMinutes)

        // Start Foreground Service if enabled
        val notifConfig = prefs.getNotificationConfig()
        if (notifConfig.isServiceEnabled) {
            runCatching {
                OduMonitorService.startService(this)
            }
        }
    }
}
