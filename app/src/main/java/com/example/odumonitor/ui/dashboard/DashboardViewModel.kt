package com.example.odumonitor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.data.repository.OduRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val signalState: OduSignalState = OduSignalState(),
    val pollingIntervalMs: Long = 3000L,
    val isDemoMode: Boolean = false,
    val isRefreshing: Boolean = false
)

class DashboardViewModel(
    private val repository: OduRepository = OduRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            repository.getSignalStream(_uiState.value.pollingIntervalMs).collect { newState ->
                _uiState.value = _uiState.value.copy(
                    signalState = newState,
                    isRefreshing = false
                )
            }
        }
    }

    fun setPollingInterval(intervalMs: Long) {
        _uiState.value = _uiState.value.copy(pollingIntervalMs = intervalMs)
        startPolling()
    }

    fun toggleDemoMode(enabled: Boolean) {
        repository.isDemoMode = enabled
        _uiState.value = _uiState.value.copy(isDemoMode = enabled)
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
        }
    }

    class Factory(private val repository: OduRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
