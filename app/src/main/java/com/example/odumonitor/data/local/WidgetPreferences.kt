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
