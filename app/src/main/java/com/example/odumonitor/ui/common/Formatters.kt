package com.example.odumonitor.ui.common

import java.util.Locale

enum class ChartMetric(val label: String, val unit: String) {
    RSRQ("RSRQ", "dB"),
    RSRP("RSRP", "dBm"),
    SINR("SINR", "dB")
}

enum class DashboardNavTab(val label: String) {
    MONITOR("Monitor"),
    RADIO("Radio"),
    SETTINGS("Settings")
}

data class TimedMetricPoint(
    val timestampMs: Long,
    val value: Float
)

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 4)
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

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
