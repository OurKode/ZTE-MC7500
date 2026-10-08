package com.example.odumonitor.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.odumonitor.data.local.NotificationConfig
import com.example.odumonitor.data.local.RouterCredentials
import com.example.odumonitor.data.local.WidgetConfig
import com.example.odumonitor.data.local.WidgetPreferences
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.data.repository.OduRepository
import com.example.odumonitor.service.OduMonitorService
import com.example.odumonitor.worker.WidgetUpdateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val signalState: OduSignalState = OduSignalState(),
    val signalHistory: List<OduSignalState> = emptyList(),
    val pollingIntervalMs: Long = 3000L,
    val isRefreshing: Boolean = false,
    val widgetConfig: WidgetConfig = WidgetConfig(),
    val routerCredentials: RouterCredentials = RouterCredentials(),
    val notificationConfig: NotificationConfig = NotificationConfig(),
    val loginStatusMessage: String? = null,
    val isTestingLogin: Boolean = false
)

class DashboardViewModel(
    private val context: Context,
    private val repository: OduRepository = OduRepository(context)
) : ViewModel() {

    private val widgetPrefs = WidgetPreferences(context)
    private val _uiState = MutableStateFlow(
        DashboardUiState(
            widgetConfig = widgetPrefs.getWidgetConfig(),
            routerCredentials = widgetPrefs.getRouterCredentials(),
            notificationConfig = widgetPrefs.getNotificationConfig()
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            // Seed initial history from recent cache if available
            val cachedHistory = repository.getHistoryList().takeLast(30)
            if (cachedHistory.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    signalHistory = cachedHistory,
                    signalState = cachedHistory.last()
                )
            }
        }
        startPolling()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            repository.getSignalStream(_uiState.value.pollingIntervalMs).collect { newState ->
                val currentHistory = _uiState.value.signalHistory
                val updatedHistory = if (newState.isConnected) {
                    (currentHistory + newState).takeLast(30)
                } else {
                    currentHistory
                }
                _uiState.value = _uiState.value.copy(
                    signalState = newState,
                    signalHistory = updatedHistory,
                    isRefreshing = false
                )
            }
        }
    }

    fun setPollingInterval(intervalMs: Long) {
        _uiState.value = _uiState.value.copy(pollingIntervalMs = intervalMs)
        startPolling()
    }

    fun refreshImmediately() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val newState = repository.fetchCurrentSignalOnce()
            val currentHistory = _uiState.value.signalHistory
            val updatedHistory = if (newState.isConnected) {
                (currentHistory + newState).takeLast(30)
            } else {
                currentHistory
            }
            _uiState.value = _uiState.value.copy(
                signalState = newState,
                signalHistory = updatedHistory,
                isRefreshing = false
            )
        }
    }

    fun updateWidgetConfig(config: WidgetConfig) {
        widgetPrefs.saveWidgetConfig(config)
        _uiState.value = _uiState.value.copy(widgetConfig = config)
        WidgetUpdateManager.scheduleWidgetUpdates(context, config.updateIntervalMinutes)
    }

    fun updateRouterCredentials(creds: RouterCredentials) {
        widgetPrefs.saveRouterCredentials(creds)
        _uiState.value = _uiState.value.copy(routerCredentials = creds)
        refreshImmediately()
    }

    fun testRouterLogin(creds: RouterCredentials) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingLogin = true, loginStatusMessage = null)
            val res = repository.testLogin(creds)
            val msg = if (res.isSuccess) {
                "Berhasil login! Sesi aktif: ${res.getOrNull()?.take(8)}..."
            } else {
                "Gagal: ${res.exceptionOrNull()?.message}"
            }
            _uiState.value = _uiState.value.copy(
                isTestingLogin = false,
                loginStatusMessage = msg
            )
            if (res.isSuccess) {
                updateRouterCredentials(creds)
            }
        }
    }

    fun updateNotificationConfig(config: NotificationConfig) {
        widgetPrefs.saveNotificationConfig(config)
        _uiState.value = _uiState.value.copy(notificationConfig = config)
        if (config.isServiceEnabled) {
            OduMonitorService.startService(context)
        } else {
            OduMonitorService.stopService(context)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(context) as T
        }
    }
}
