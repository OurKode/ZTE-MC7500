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
        DashboardViewModel.Factory(com.example.odumonitor.data.repository.OduRepository())
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
                    onToggleDemoMode = { viewModel.toggleDemoMode(it) }
                )
            }
        }
    }
}
