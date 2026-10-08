package com.example.odumonitor.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.glance.appwidget.updateAll
import com.example.odumonitor.data.local.WidgetPreferences
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.data.repository.OduRepository
import com.example.odumonitor.notification.OduNotificationManager
import com.example.odumonitor.widget.OduCompactWidget
import com.example.odumonitor.widget.OduDetailedWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OduMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: OduRepository
    private lateinit var widgetPrefs: WidgetPreferences
    private lateinit var notificationManager: OduNotificationManager

    private var previousState: OduSignalState? = null

    companion object {
        private const val ACTION_START = "ACTION_START"
        private const val ACTION_STOP = "ACTION_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, OduMonitorService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, OduMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = OduRepository(applicationContext)
        widgetPrefs = WidgetPreferences(applicationContext)
        notificationManager = OduNotificationManager(applicationContext)

        val cached = repository.getLastCachedSignal() ?: OduSignalState(
            isConnected = false,
            connectionType = "Menghubungkan",
            provider = "ODU ZTE MC7500"
        )
        previousState = cached

        val initialNotif = notificationManager.buildForegroundNotification(cached)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                OduNotificationManager.NOTIFICATION_ID_FOREGROUND,
                initialNotif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(OduNotificationManager.NOTIFICATION_ID_FOREGROUND, initialNotif)
        }

        startTelemetryMonitoring()
    }

    private fun startTelemetryMonitoring() {
        serviceScope.launch {
            repository.getSignalStream(pollingIntervalMs = 3000L).collect { newState ->
                handleStateChange(newState)
            }
        }
    }

    private suspend fun handleStateChange(newState: OduSignalState) {
        val config = widgetPrefs.getNotificationConfig()
        val prev = previousState

        if (prev != null) {
            // 1. ODU Offline / Reconnected Trigger
            if (config.notifyOduOffline) {
                if (prev.isConnected && !newState.isConnected) {
                    notificationManager.sendOduOfflineAlert(
                        newState.errorMessage ?: "Koneksi ke router ZTE terputus"
                    )
                } else if (!prev.isConnected && newState.isConnected) {
                    notificationManager.sendOduOnlineAlert(newState.provider, newState.connectionType)
                }
            }

            // 2. Band Change Trigger (when connected)
            if (newState.isConnected && config.notifyBandChange) {
                if (prev.nrBand != "-" && newState.nrBand != "-" && prev.nrBand != newState.nrBand) {
                    notificationManager.sendBandChangeAlert(prev.nrBand, newState.nrBand, is5g = true)
                }
                if (prev.lteBand != "-" && newState.lteBand != "-" && prev.lteBand != newState.lteBand) {
                    notificationManager.sendBandChangeAlert(prev.lteBand, newState.lteBand, is5g = false)
                }
            }

            // 3. WAN IP Change Trigger (when connected)
            if (newState.isConnected && config.notifyWanIpChange) {
                val oldIp = prev.wanIp
                val newIp = newState.wanIp
                if (!oldIp.isNullOrBlank() && !newIp.isNullOrBlank() && oldIp != newIp) {
                    notificationManager.sendWanIpChangeAlert(oldIp, newIp)
                }
            }
        }

        previousState = newState

        // Update foreground persistent status
        val updatedNotif = notificationManager.buildForegroundNotification(newState)
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notifManager.notify(OduNotificationManager.NOTIFICATION_ID_FOREGROUND, updatedNotif)

        // Sync with Glance widgets
        runCatching {
            OduCompactWidget().updateAll(applicationContext)
            OduDetailedWidget().updateAll(applicationContext)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
