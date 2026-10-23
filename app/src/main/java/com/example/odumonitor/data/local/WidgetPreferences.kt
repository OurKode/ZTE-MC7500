package com.example.odumonitor.data.local

import android.content.Context
import android.content.SharedPreferences

data class WidgetConfig(
    val show5g: Boolean = true,
    val show4g: Boolean = true,
    val showProviderStatus: Boolean = true,
    val showPciTower: Boolean = true,
    val showBandInfo: Boolean = true,
    val showSinrRsrq: Boolean = true,
    val showTimestamp: Boolean = true,
    val updateIntervalMinutes: Int = 15 // Options: 15, 30, 60, 180, 360, -1 (manual)
)

data class RouterCredentials(
    val host: String = "192.168.254.1",
    val username: String = "Admin",
    val password: String = "",
    val isLoginEnabled: Boolean = true
)

data class NotificationConfig(
    val isServiceEnabled: Boolean = true,
    val notifyBandChange: Boolean = true,
    val notifyWanIpChange: Boolean = true,
    val notifyOduOffline: Boolean = true
)

data class UiPreferences(
    val pollingIntervalMs: Long = 3000L,
    val chartTimeRange: ChartTimeRange = ChartTimeRange.FIVE_MINUTES,
    val selected5gMetric: String = "SINR",
    val selected4gMetric: String = "SINR",
    val lastSelectedTab: Int = 0,
    val custom4gBands: Set<Int> = setOf(1, 3, 8, 40),
    val custom5gBands: Set<Int> = setOf(1, 3, 28, 40, 78)
)

class WidgetPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "widget_preferences"
        private const val KEY_SHOW_5G = "show_5g"
        private const val KEY_SHOW_4G = "show_4g"
        private const val KEY_SHOW_PROVIDER_STATUS = "show_provider_status"
        private const val KEY_SHOW_PCI_TOWER = "show_pci_tower"
        private const val KEY_SHOW_BAND_INFO = "show_band_info"
        private const val KEY_SHOW_SINR_RSRQ = "show_sinr_rsrq"
        private const val KEY_SHOW_TIMESTAMP = "show_timestamp"
        private const val KEY_UPDATE_INTERVAL = "update_interval_minutes"

        private const val KEY_RETENTION_SETTING = "history_retention_setting"

        private const val KEY_ROUTER_HOST = "router_host"
        private const val KEY_ROUTER_USERNAME = "router_username"
        private const val KEY_ROUTER_PASSWORD = "router_password"
        private const val KEY_ENABLE_ROUTER_LOGIN = "enable_router_login"

        private const val KEY_ENABLE_MONITOR_SERVICE = "enable_monitor_service"
        private const val KEY_NOTIFY_BAND_CHANGE = "notify_band_change"
        private const val KEY_NOTIFY_WAN_IP_CHANGE = "notify_wan_ip_change"
        private const val KEY_NOTIFY_ODU_OFFLINE = "notify_odu_offline"

        // User UI Preferences
        private const val KEY_POLLING_INTERVAL_MS = "ui_polling_interval_ms"
        private const val KEY_CHART_TIME_RANGE = "ui_chart_time_range"
        private const val KEY_SELECTED_5G_METRIC = "ui_selected_5g_metric"
        private const val KEY_SELECTED_4G_METRIC = "ui_selected_4g_metric"
        private const val KEY_LAST_SELECTED_TAB = "ui_last_selected_tab"
        private const val KEY_CUSTOM_4G_BANDS = "ui_custom_4g_bands"
        private const val KEY_CUSTOM_5G_BANDS = "ui_custom_5g_bands"
    }

    fun getUiPreferences(): UiPreferences {
        val raw4g = prefs.getString(KEY_CUSTOM_4G_BANDS, "1,3,8,40") ?: "1,3,8,40"
        val raw5g = prefs.getString(KEY_CUSTOM_5G_BANDS, "1,3,28,40,78") ?: "1,3,28,40,78"
        val parsed4g = raw4g.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        val parsed5g = raw5g.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

        return UiPreferences(
            pollingIntervalMs = prefs.getLong(KEY_POLLING_INTERVAL_MS, 3000L).coerceIn(1000L, 60000L),
            chartTimeRange = ChartTimeRange.fromName(prefs.getString(KEY_CHART_TIME_RANGE, ChartTimeRange.FIVE_MINUTES.name)),
            selected5gMetric = prefs.getString(KEY_SELECTED_5G_METRIC, "SINR") ?: "SINR",
            selected4gMetric = prefs.getString(KEY_SELECTED_4G_METRIC, "SINR") ?: "SINR",
            lastSelectedTab = prefs.getInt(KEY_LAST_SELECTED_TAB, 0).coerceIn(0, 2),
            custom4gBands = if (parsed4g.isNotEmpty()) parsed4g else setOf(1, 3, 8, 40),
            custom5gBands = if (parsed5g.isNotEmpty()) parsed5g else setOf(1, 3, 28, 40, 78)
        )
    }

    fun saveUiPreferences(uiPrefs: UiPreferences) {
        prefs.edit().apply {
            putLong(KEY_POLLING_INTERVAL_MS, uiPrefs.pollingIntervalMs)
            putString(KEY_CHART_TIME_RANGE, uiPrefs.chartTimeRange.name)
            putString(KEY_SELECTED_5G_METRIC, uiPrefs.selected5gMetric)
            putString(KEY_SELECTED_4G_METRIC, uiPrefs.selected4gMetric)
            putInt(KEY_LAST_SELECTED_TAB, uiPrefs.lastSelectedTab)
            putString(KEY_CUSTOM_4G_BANDS, uiPrefs.custom4gBands.sorted().joinToString(","))
            putString(KEY_CUSTOM_5G_BANDS, uiPrefs.custom5gBands.sorted().joinToString(","))
            apply()
        }
    }

    fun getNotificationConfig(): NotificationConfig {
        return NotificationConfig(
            isServiceEnabled = prefs.getBoolean(KEY_ENABLE_MONITOR_SERVICE, true),
            notifyBandChange = prefs.getBoolean(KEY_NOTIFY_BAND_CHANGE, true),
            notifyWanIpChange = prefs.getBoolean(KEY_NOTIFY_WAN_IP_CHANGE, true),
            notifyOduOffline = prefs.getBoolean(KEY_NOTIFY_ODU_OFFLINE, true)
        )
    }

    fun saveNotificationConfig(config: NotificationConfig) {
        prefs.edit().apply {
            putBoolean(KEY_ENABLE_MONITOR_SERVICE, config.isServiceEnabled)
            putBoolean(KEY_NOTIFY_BAND_CHANGE, config.notifyBandChange)
            putBoolean(KEY_NOTIFY_WAN_IP_CHANGE, config.notifyWanIpChange)
            putBoolean(KEY_NOTIFY_ODU_OFFLINE, config.notifyOduOffline)
            apply()
        }
    }

    fun getRouterCredentials(): RouterCredentials {
        return RouterCredentials(
            host = prefs.getString(KEY_ROUTER_HOST, "192.168.254.1") ?: "192.168.254.1",
            username = prefs.getString(KEY_ROUTER_USERNAME, "Admin") ?: "Admin",
            password = prefs.getString(KEY_ROUTER_PASSWORD, "") ?: "",
            isLoginEnabled = prefs.getBoolean(KEY_ENABLE_ROUTER_LOGIN, true)
        )
    }

    fun saveRouterCredentials(creds: RouterCredentials) {
        prefs.edit().apply {
            putString(KEY_ROUTER_HOST, creds.host.trim())
            putString(KEY_ROUTER_USERNAME, creds.username.trim())
            putString(KEY_ROUTER_PASSWORD, creds.password)
            putBoolean(KEY_ENABLE_ROUTER_LOGIN, creds.isLoginEnabled)
            apply()
        }
    }

    fun getWidgetConfig(): WidgetConfig {
        return WidgetConfig(
            show5g = prefs.getBoolean(KEY_SHOW_5G, true),
            show4g = prefs.getBoolean(KEY_SHOW_4G, true),
            showProviderStatus = prefs.getBoolean(KEY_SHOW_PROVIDER_STATUS, true),
            showPciTower = prefs.getBoolean(KEY_SHOW_PCI_TOWER, true),
            showBandInfo = prefs.getBoolean(KEY_SHOW_BAND_INFO, true),
            showSinrRsrq = prefs.getBoolean(KEY_SHOW_SINR_RSRQ, true),
            showTimestamp = prefs.getBoolean(KEY_SHOW_TIMESTAMP, true),
            updateIntervalMinutes = prefs.getInt(KEY_UPDATE_INTERVAL, 15)
        )
    }

    fun saveWidgetConfig(config: WidgetConfig) {
        prefs.edit().apply {
            putBoolean(KEY_SHOW_5G, config.show5g)
            putBoolean(KEY_SHOW_4G, config.show4g)
            putBoolean(KEY_SHOW_PROVIDER_STATUS, config.showProviderStatus)
            putBoolean(KEY_SHOW_PCI_TOWER, config.showPciTower)
            putBoolean(KEY_SHOW_BAND_INFO, config.showBandInfo)
            putBoolean(KEY_SHOW_SINR_RSRQ, config.showSinrRsrq)
            putBoolean(KEY_SHOW_TIMESTAMP, config.showTimestamp)
            putInt(KEY_UPDATE_INTERVAL, config.updateIntervalMinutes)
            apply()
        }
    }

    fun getHistoryRetention(): HistoryRetention {
        val name = prefs.getString(KEY_RETENTION_SETTING, HistoryRetention.TWENTY_FOUR_HOURS.name)
        return HistoryRetention.fromName(name)
    }

    fun saveHistoryRetention(retention: HistoryRetention) {
        prefs.edit().putString(KEY_RETENTION_SETTING, retention.name).apply()
    }
}
