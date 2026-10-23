package com.example.odumonitor.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.odumonitor.data.local.ChartTimeRange
import com.example.odumonitor.data.local.HistoryRetention
import com.example.odumonitor.data.local.NotificationConfig
import com.example.odumonitor.data.local.RouterCredentials
import com.example.odumonitor.data.local.UiPreferences
import com.example.odumonitor.data.local.WidgetConfig
import com.example.odumonitor.data.local.WidgetPreferences
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.data.repository.OduRepository
import com.example.odumonitor.service.OduMonitorService
import com.example.odumonitor.worker.WidgetUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardUiState(
    val signalState: OduSignalState = OduSignalState(
        isConnected = false,
        connectionType = "Terputus",
        provider = "ODU Terputus (Offline)",
        errorMessage = "Memeriksa koneksi ke ODU..."
    ),
    val signalHistory: List<OduSignalState> = emptyList(),
    val pollingIntervalMs: Long = 3000L,
    val chartTimeRange: ChartTimeRange = ChartTimeRange.FIVE_MINUTES,
    val selected5gMetric: String = "SINR",
    val selected4gMetric: String = "SINR",
    val selectedTab: Int = 0,
    val custom4gBands: Set<Int> = setOf(1, 3, 8, 40),
    val custom5gBands: Set<Int> = setOf(1, 3, 28, 40, 78),
    val historyRetention: HistoryRetention = HistoryRetention.TWENTY_FOUR_HOURS,
    val isRefreshing: Boolean = true,
    val widgetConfig: WidgetConfig = WidgetConfig(),
    val routerCredentials: RouterCredentials = RouterCredentials(),
    val notificationConfig: NotificationConfig = NotificationConfig(),
    val loginStatusMessage: String? = null,
    val isTestingLogin: Boolean = false,
    val isExecutingRadioCommand: Boolean = false,
    val radioCommandMessage: String? = null
)

class DashboardViewModel(
    private val context: Context,
    private val repository: OduRepository = OduRepository(context)
) : ViewModel() {

    private val widgetPrefs = WidgetPreferences(context)
    private val initialUiPrefs = widgetPrefs.getUiPreferences()

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            pollingIntervalMs = initialUiPrefs.pollingIntervalMs,
            chartTimeRange = initialUiPrefs.chartTimeRange,
            selected5gMetric = initialUiPrefs.selected5gMetric,
            selected4gMetric = initialUiPrefs.selected4gMetric,
            selectedTab = initialUiPrefs.lastSelectedTab,
            custom4gBands = initialUiPrefs.custom4gBands,
            custom5gBands = initialUiPrefs.custom5gBands,
            historyRetention = repository.getHistoryRetention(),
            widgetConfig = widgetPrefs.getWidgetConfig(),
            routerCredentials = widgetPrefs.getRouterCredentials(),
            notificationConfig = widgetPrefs.getNotificationConfig()
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    private fun persistCurrentUiPreferences(state: DashboardUiState = _uiState.value) {
        widgetPrefs.saveUiPreferences(
            UiPreferences(
                pollingIntervalMs = state.pollingIntervalMs,
                chartTimeRange = state.chartTimeRange,
                selected5gMetric = state.selected5gMetric,
                selected4gMetric = state.selected4gMetric,
                lastSelectedTab = state.selectedTab,
                custom4gBands = state.custom4gBands,
                custom5gBands = state.custom5gBands
            )
        )
    }

    private suspend fun loadHistoryForActiveRange(
        range: ChartTimeRange,
        latestState: OduSignalState
    ): List<OduSignalState> = withContext(Dispatchers.IO) {
        if (!latestState.isConnected) return@withContext emptyList()
        val dbHistory = repository.getHistoryForRange(range, maxPoints = 60)
        if (dbHistory.isNotEmpty()) {
            dbHistory
        } else {
            listOf(latestState)
        }
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            repository.getSignalStream(_uiState.value.pollingIntervalMs).collect { newState ->
                val updatedHistory = loadHistoryForActiveRange(_uiState.value.chartTimeRange, newState)
                _uiState.value = _uiState.value.copy(
                    signalState = newState,
                    signalHistory = updatedHistory,
                    isRefreshing = false
                )
            }
        }
    }

    fun setPollingInterval(intervalMs: Long) {
        val nextState = _uiState.value.copy(pollingIntervalMs = intervalMs)
        _uiState.value = nextState
        persistCurrentUiPreferences(nextState)
        startPolling()
    }

    fun setChartTimeRange(range: ChartTimeRange) {
        val nextState = _uiState.value.copy(chartTimeRange = range)
        _uiState.value = nextState
        persistCurrentUiPreferences(nextState)
        viewModelScope.launch {
            val currentSignal = _uiState.value.signalState
            val updatedHistory = loadHistoryForActiveRange(range, currentSignal)
            _uiState.value = _uiState.value.copy(signalHistory = updatedHistory)
        }
    }

    fun setSelected5gMetric(metric: String) {
        val nextState = _uiState.value.copy(selected5gMetric = metric)
        _uiState.value = nextState
        persistCurrentUiPreferences(nextState)
    }

    fun setSelected4gMetric(metric: String) {
        val nextState = _uiState.value.copy(selected4gMetric = metric)
        _uiState.value = nextState
        persistCurrentUiPreferences(nextState)
    }

    fun setSelectedTab(tabIndex: Int) {
        val nextState = _uiState.value.copy(selectedTab = tabIndex.coerceIn(0, 2))
        _uiState.value = nextState
        persistCurrentUiPreferences(nextState)
    }

    fun setCustom4gBands(bands: Set<Int>) {
        val nextState = _uiState.value.copy(custom4gBands = bands)
        _uiState.value = nextState
        persistCurrentUiPreferences(nextState)
    }

    fun setCustom5gBands(bands: Set<Int>) {
        val nextState = _uiState.value.copy(custom5gBands = bands)
        _uiState.value = nextState
        persistCurrentUiPreferences(nextState)
    }

    fun updateHistoryRetention(retention: HistoryRetention) {
        repository.saveHistoryRetention(retention)
        _uiState.value = _uiState.value.copy(historyRetention = retention)
    }

    fun clearSignalHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllHistory()
            _uiState.value = _uiState.value.copy(signalHistory = emptyList())
        }
    }

    fun refreshImmediately() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val newState = repository.fetchCurrentSignalOnce()
            val updatedHistory = loadHistoryForActiveRange(_uiState.value.chartTimeRange, newState)
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

    private fun executeRadioAction(actionName: String, block: suspend () -> Result<Boolean>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExecutingRadioCommand = true,
                radioCommandMessage = "Menerapkan $actionName..."
            )
            val res = block()
            val msg = if (res.isSuccess) {
                "Berhasil menerapkan $actionName. Menunggu modem re-sinkronisasi..."
            } else {
                "Gagal menerapkan $actionName: ${res.exceptionOrNull()?.message}"
            }
            _uiState.value = _uiState.value.copy(
                isExecutingRadioCommand = false,
                radioCommandMessage = msg
            )
            if (res.isSuccess) {
                kotlinx.coroutines.delay(1200)
                refreshImmediately()
            }
        }
    }

    fun setNetworkSelect(mode: String) {
        val label = com.example.odumonitor.util.FrequencyConverter.formatBearerModeName(mode)
        executeRadioAction("Mode Jaringan ($label)") {
            repository.setNetworkSelect(mode)
        }
    }

    fun set4gBandLock(maskStr: String, label: String) {
        executeRadioAction("Kunci Band 4G ($label)") {
            repository.set4gBandLock(maskStr)
        }
    }

    fun set5gBandLock(bandsStr: String, label: String) {
        executeRadioAction("Kunci Band 5G ($label)") {
            repository.set5gBandLock(bandsStr)
        }
    }

    fun lock4gCell(pci: Int, earfcn: Int) {
        executeRadioAction("Kunci Sel 4G (PCI $pci, EARFCN $earfcn)") {
            repository.lock4gCell(pci, earfcn)
        }
    }

    fun unlock4gCell() {
        executeRadioAction("Buka Kunci Sel 4G") {
            repository.unlock4gCell()
        }
    }

    fun lock5gCell(pci: Int, earfcnOrArfcn: Int, bandNumber: Int) {
        executeRadioAction("Kunci Sel 5G (PCI $pci, ARFCN $earfcnOrArfcn, n$bandNumber)") {
            repository.lock5gCell(pci, earfcnOrArfcn, bandNumber)
        }
    }

    fun unlock5gCell() {
        executeRadioAction("Buka Kunci Sel 5G") {
            repository.unlock5gCell()
        }
    }

    fun clearRadioCommandMessage() {
        _uiState.value = _uiState.value.copy(radioCommandMessage = null)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(context) as T
        }
    }
}
