package com.example.odumonitor

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

        // Request notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        setContent {
            OduMonitorTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                DashboardScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.refreshImmediately() },
                    onPollingIntervalSelected = { viewModel.setPollingInterval(it) },
                    onWidgetConfigChanged = { viewModel.updateWidgetConfig(it) },
                    onRouterCredentialsChanged = { viewModel.updateRouterCredentials(it) },
                    onTestRouterLogin = { viewModel.testRouterLogin(it) },
                    onNotificationConfigChanged = { viewModel.updateNotificationConfig(it) }
                )
            }
        }
    }
}
