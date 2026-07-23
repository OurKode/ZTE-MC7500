package com.example.odumonitor.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.odumonitor.data.local.HistoryRetention
import com.example.odumonitor.data.local.WidgetConfig
import com.example.odumonitor.data.local.WidgetPreferences
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.data.repository.OduRepository
import com.example.odumonitor.worker.WidgetUpdateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val signalState: OduSignalState = OduSignalState(),
    val pollingIntervalMs: Long = 3000L,
    val isRefreshing: Boolean = false,
    val activeTab: Int = 0,
    val historyList: List<OduSignalState> = emptyList(),
    val historyRetention: HistoryRetention = HistoryRetention.TWENTY_FOUR_HOURS,
    val widgetConfig: WidgetConfig = WidgetConfig()
)

class DashboardViewModel(
    private val context: Context,
    private val repository: OduRepository = OduRepository(context)
) : ViewModel() {

    private val widgetPrefs = WidgetPreferences(context)
    private val _uiState = MutableStateFlow(
        DashboardUiState(
            historyRetention = repository.getHistoryRetention(),
            widgetConfig = widgetPrefs.getWidgetConfig()
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        startPolling()
        loadHistory()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            repository.getSignalStream(_uiState.value.pollingIntervalMs).collect { newState ->
                _uiState.value = _uiState.value.copy(
                    signalState = newState,
                    isRefreshing = false
                )
                if (_uiState.value.activeTab == 1) {
                    loadHistory()
                }
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
            _uiState.value = _uiState.value.copy(
                signalState = newState,
                isRefreshing = false
            )
            loadHistory()
        }
    }

    fun setActiveTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tabIndex)
        if (tabIndex == 1) {
            loadHistory()
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            val history = repository.getHistoryList()
            _uiState.value = _uiState.value.copy(historyList = history)
        }
    }

    fun setHistoryRetention(retention: HistoryRetention) {
        repository.saveHistoryRetention(retention)
        _uiState.value = _uiState.value.copy(historyRetention = retention)
        loadHistory()
    }

    fun clearHistory() {
        repository.clearAllHistory()
        loadHistory()
    }

    fun updateWidgetConfig(config: WidgetConfig) {
        widgetPrefs.saveWidgetConfig(config)
        _uiState.value = _uiState.value.copy(widgetConfig = config)
        WidgetUpdateManager.scheduleWidgetUpdates(context, config.updateIntervalMinutes)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(context) as T
        }
    }
}
