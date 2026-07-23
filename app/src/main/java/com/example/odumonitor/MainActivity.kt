package com.example.odumonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.odumonitor.ui.dashboard.DashboardScreen
import com.example.odumonitor.ui.dashboard.DashboardViewModel
import com.example.odumonitor.ui.theme.OduMonitorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OduMonitorTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                DashboardScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.refreshImmediately() },
                    onPollingIntervalSelected = { viewModel.setPollingInterval(it) },
                    onTabSelected = { viewModel.setActiveTab(it) },
                    onHistoryRetentionSelected = { viewModel.setHistoryRetention(it) },
                    onClearHistory = { viewModel.clearHistory() },
                    onWidgetConfigChanged = { viewModel.updateWidgetConfig(it) }
                )
            }
        }
    }
}
