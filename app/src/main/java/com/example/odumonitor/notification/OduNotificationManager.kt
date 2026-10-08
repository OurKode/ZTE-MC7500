package com.example.odumonitor.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.odumonitor.MainActivity
import com.example.odumonitor.R
import com.example.odumonitor.data.model.OduSignalState
import java.util.Locale

class OduNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID_MONITOR = "odu_monitor_service_channel"
        const val CHANNEL_ID_ALERTS = "odu_alerts_channel"

        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val NOTIFICATION_ID_BAND_ALERT = 1002
        const val NOTIFICATION_ID_WAN_ALERT = 1003
        const val NOTIFICATION_ID_OFFLINE_ALERT = 1004

        fun formatSpeed(bytesPerSec: Long): String {
            if (bytesPerSec <= 0) return "0 Mbps"
            val bitsPerSec = bytesPerSec * 8.0
            return when {
                bitsPerSec >= 1_000_000_000.0 -> String.format(Locale.US, "%.2f Gbps", bitsPerSec / 1_000_000_000.0)
                bitsPerSec >= 1_000_000.0 -> String.format(Locale.US, "%.1f Mbps", bitsPerSec / 1_000_000.0)
                bitsPerSec >= 1_000.0 -> String.format(Locale.US, "%.1f Kbps", bitsPerSec / 1_000.0)
                else -> String.format(Locale.US, "%.0f bps", bitsPerSec)
            }
        }
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: Ongoing low-priority service indicator
            val monitorChannel = NotificationChannel(
                CHANNEL_ID_MONITOR,
                "Layanan Pemantauan ODU (Aktif)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status koneksi dan telemetri throughput ZTE MC7500 secara real-time"
                setShowBadge(false)
            }

            // Channel 2: High priority alerts (Band change, WAN IP change, ODU offline)
            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Peringatan Jaringan ODU",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi peringatan saat Band berubah, WAN IP berganti, atau koneksi terputus"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(monitorChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    fun buildForegroundNotification(state: OduSignalState): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (state.isConnected) {
            "ODU Online: ${state.provider} (${state.connectionType})"
        } else {
            "ODU Terputus: Mencoba menghubungkan..."
        }

        val text = if (state.isConnected) {
            val speedText = if (state.isRouterLoggedIn) {
                "↓ ${formatSpeed(state.downloadSpeedBps)}  ↑ ${formatSpeed(state.uploadSpeedBps)}"
            } else {
                "RSRP: ${if (state.nrRsrp != 0) "${state.nrRsrp} dBm (5G)" else "${state.lteRsrp} dBm (4G)"}"
            }
            val bandText = if (state.nrBand != "-") state.nrBand else state.lteBand
            "$speedText • Band $bandText"
        } else {
            state.errorMessage ?: "Router ZTE 192.168.254.1 tidak dapat dijangkau"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID_MONITOR)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun sendBandChangeAlert(oldBand: String, newBand: String, is5g: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BAND_ALERT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tech = if (is5g) "5G NR" else "4G LTE"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setContentTitle("Perubahan Band $tech Terdeteksi")
            .setContentText("Beralih ke Band $newBand (sebelumnya $oldBand)")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BAND_ALERT, notification)
    }

    fun sendWanIpChangeAlert(oldIp: String, newIp: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_WAN_ALERT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setContentTitle("Alamat WAN IP Router Berganti")
            .setContentText("IP Baru: $newIp (sebelumnya $oldIp)")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(NOTIFICATION_ID_WAN_ALERT, notification)
    }

    fun sendOduOfflineAlert(reason: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_OFFLINE_ALERT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setContentTitle("Peringatan: ODU ZTE Terputus")
            .setContentText("Router tidak merespons: $reason")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(NOTIFICATION_ID_OFFLINE_ALERT, notification)
    }

    fun sendOduOnlineAlert(provider: String, type: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_OFFLINE_ALERT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setContentTitle("ODU ZTE Kembali Online")
            .setContentText("Terhubung ke jaringan $provider ($type)")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(NOTIFICATION_ID_OFFLINE_ALERT, notification)
    }
}
