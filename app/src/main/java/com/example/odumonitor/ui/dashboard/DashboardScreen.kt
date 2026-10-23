package com.example.odumonitor.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.odumonitor.data.local.ChartTimeRange
import com.example.odumonitor.data.local.HistoryRetention
import com.example.odumonitor.data.local.NotificationConfig
import com.example.odumonitor.data.local.RouterCredentials
import com.example.odumonitor.data.local.WidgetConfig
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

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
        bitsPerSec >= 1_000_000.0 -> String.format(Locale.US, "%.1f Kbps", bitsPerSec / 1_000.0)
        bitsPerSec >= 1_000.0 -> String.format(Locale.US, "%.1f Kbps", bitsPerSec / 1_000.0)
        else -> String.format(Locale.US, "%.0f bps", bitsPerSec)
    }
}

enum class ChartMetric(val label: String, val unit: String) {
    RSRQ("RSRQ", "dB"),
    RSRP("RSRP", "dBm"),
    SINR("SINR", "dB")
}

enum class DashboardNavTab(val label: String) {
    MONITOR("Monitor"),
    RADIO("Radio & Band"),
    SETTINGS("Pengaturan")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onPollingIntervalSelected: (Long) -> Unit,
    onChartTimeRangeSelected: (ChartTimeRange) -> Unit = {},
    onSelect5gMetric: (String) -> Unit = {},
    onSelect4gMetric: (String) -> Unit = {},
    onSelectTab: (Int) -> Unit = {},
    onUpdateCustom4gBands: (Set<Int>) -> Unit = {},
    onUpdateCustom5gBands: (Set<Int>) -> Unit = {},
    onUpdateHistoryRetention: (HistoryRetention) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onWidgetConfigChanged: (WidgetConfig) -> Unit,
    onRouterCredentialsChanged: (RouterCredentials) -> Unit = {},
    onTestRouterLogin: (RouterCredentials) -> Unit = {},
    onNotificationConfigChanged: (NotificationConfig) -> Unit = {},
    onSetNetworkSelect: (String) -> Unit = {},
    onSet4gBandLock: (String, String) -> Unit = { _, _ -> },
    onSet5gBandLock: (String, String) -> Unit = { _, _ -> },
    onLock4gCell: (Int, Int) -> Unit = { _, _ -> },
    onUnlock4gCell: () -> Unit = {},
    onLock5gCell: (Int, Int, Int) -> Unit = { _, _, _ -> },
    onUnlock5gCell: () -> Unit = {},
    onClearRadioMessage: () -> Unit = {}
) {
    val activeNavTab = DashboardNavTab.entries.getOrElse(uiState.selectedTab) { DashboardNavTab.MONITOR }
    var settingsSubTab by rememberSaveable { mutableIntStateOf(0) }
    val selected5gMetric = ChartMetric.entries.firstOrNull { it.name == uiState.selected5gMetric } ?: ChartMetric.SINR
    val selected4gMetric = ChartMetric.entries.firstOrNull { it.name == uiState.selected4gMetric } ?: ChartMetric.SINR

    val isConnected = uiState.signalState.isConnected && uiState.signalState.errorMessage == null
    val isDark = isSystemInDarkTheme()
    val onlineDotColor = if (isDark) SignalExcellent else SignalExcellentLight

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Router else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ZTE MC7500",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isConnected) {
                                    "Terhubung (${uiState.routerCredentials.host})"
                                } else {
                                    "ODU Terputus (${uiState.routerCredentials.host})"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isConnected) onlineDotColor else MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(48.dp)
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Muat Ulang Telemetri",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                NavigationBarItem(
                    selected = activeNavTab == DashboardNavTab.MONITOR,
                    onClick = { onSelectTab(DashboardNavTab.MONITOR.ordinal) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = DashboardNavTab.MONITOR.label
                        )
                    },
                    label = { Text(DashboardNavTab.MONITOR.label) }
                )
                NavigationBarItem(
                    selected = activeNavTab == DashboardNavTab.RADIO,
                    onClick = { onSelectTab(DashboardNavTab.RADIO.ordinal) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SettingsInputAntenna,
                            contentDescription = DashboardNavTab.RADIO.label
                        )
                    },
                    label = { Text(DashboardNavTab.RADIO.label) }
                )
                NavigationBarItem(
                    selected = activeNavTab == DashboardNavTab.SETTINGS,
                    onClick = { onSelectTab(DashboardNavTab.SETTINGS.ordinal) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = DashboardNavTab.SETTINGS.label
                        )
                    },
                    label = { Text(DashboardNavTab.SETTINGS.label) }
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpandedWidth = maxWidth >= 600.dp
            val horizontalPad = if (isExpandedWidth) 24.dp else 14.dp

            when (activeNavTab) {
                DashboardNavTab.MONITOR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = horizontalPad, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ConnectionStatusBanner(
                            signal = uiState.signalState,
                            routerHost = uiState.routerCredentials.host,
                            pollingInterval = uiState.pollingIntervalMs,
                            chartTimeRange = uiState.chartTimeRange,
                            isRefreshing = uiState.isRefreshing,
                            onPollingIntervalSelected = onPollingIntervalSelected,
                            onChartTimeRangeSelected = onChartTimeRangeSelected,
                            onRefresh = onRefresh,
                            onOpenRouterSettings = {
                                settingsSubTab = 0
                                onSelectTab(DashboardNavTab.SETTINGS.ordinal)
                            }
                        )

                        RouterTrafficCard(
                            signal = uiState.signalState,
                            history = uiState.signalHistory,
                            chartTimeRange = uiState.chartTimeRange,
                            isExpandedWidth = isExpandedWidth,
                            onOpenSettings = {
                                settingsSubTab = 0
                                onSelectTab(DashboardNavTab.SETTINGS.ordinal)
                            }
                        )

                        if (isExpandedWidth) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SignalTechCard(
                                        title = "5G NR",
                                        isConnected = isConnected,
                                        band = uiState.signalState.nrBand,
                                        rsrp = uiState.signalState.nrRsrp,
                                        sinr = uiState.signalState.nrSinr,
                                        rsrq = uiState.signalState.nrRsrq,
                                        pci = uiState.signalState.nrPci,
                                        cellId = uiState.signalState.nrCellId,
                                        earfcnOrArfcn = uiState.signalState.nrArfcn,
                                        dlFreqMhz = uiState.signalState.nrDlFreqMhz,
                                        bandwidth = uiState.signalState.nrBandwidth,
                                        history = uiState.signalHistory,
                                        chartTimeRange = uiState.chartTimeRange,
                                        selectedMetric = selected5gMetric,
                                        onMetricSelected = { onSelect5gMetric(it.name) },
                                        is5g = true
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    SignalTechCard(
                                        title = "4G LTE",
                                        isConnected = isConnected,
                                        band = uiState.signalState.lteBand,
                                        rsrp = uiState.signalState.lteRsrp,
                                        sinr = uiState.signalState.lteSinr,
                                        rsrq = uiState.signalState.lteRsrq,
                                        pci = uiState.signalState.ltePci,
                                        cellId = uiState.signalState.lteCellId,
                                        earfcnOrArfcn = uiState.signalState.lteEarfcn,
                                        dlFreqMhz = uiState.signalState.lteDlFreqMhz,
                                        bandwidth = uiState.signalState.lteBandwidth,
                                        history = uiState.signalHistory,
                                        chartTimeRange = uiState.chartTimeRange,
                                        selectedMetric = selected4gMetric,
                                        onMetricSelected = { onSelect4gMetric(it.name) },
                                        is5g = false
                                    )
                                }
                            }
                        } else {
                            SignalTechCard(
                                title = "5G NR",
                                isConnected = isConnected,
                                band = uiState.signalState.nrBand,
                                rsrp = uiState.signalState.nrRsrp,
                                sinr = uiState.signalState.nrSinr,
                                rsrq = uiState.signalState.nrRsrq,
                                pci = uiState.signalState.nrPci,
                                cellId = uiState.signalState.nrCellId,
                                earfcnOrArfcn = uiState.signalState.nrArfcn,
                                dlFreqMhz = uiState.signalState.nrDlFreqMhz,
                                bandwidth = uiState.signalState.nrBandwidth,
                                history = uiState.signalHistory,
                                chartTimeRange = uiState.chartTimeRange,
                                selectedMetric = selected5gMetric,
                                onMetricSelected = { onSelect5gMetric(it.name) },
                                is5g = true
                            )

                            SignalTechCard(
                                title = "4G LTE",
                                isConnected = isConnected,
                                band = uiState.signalState.lteBand,
                                rsrp = uiState.signalState.lteRsrp,
                                sinr = uiState.signalState.lteSinr,
                                rsrq = uiState.signalState.lteRsrq,
                                pci = uiState.signalState.ltePci,
                                cellId = uiState.signalState.lteCellId,
                                earfcnOrArfcn = uiState.signalState.lteEarfcn,
                                dlFreqMhz = uiState.signalState.lteDlFreqMhz,
                                bandwidth = uiState.signalState.lteBandwidth,
                                history = uiState.signalHistory,
                                chartTimeRange = uiState.chartTimeRange,
                                selectedMetric = selected4gMetric,
                                onMetricSelected = { onSelect4gMetric(it.name) },
                                is5g = false
                            )
                        }

                        CarrierAggregationCard(signal = uiState.signalState)

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                DashboardNavTab.RADIO -> {
                    RadioControlScreen(
                        signalState = uiState.signalState,
                        horizontalPad = horizontalPad,
                        isExecutingRadioCommand = uiState.isExecutingRadioCommand,
                        radioCommandMessage = uiState.radioCommandMessage,
                        custom4gBands = uiState.custom4gBands,
                        custom5gBands = uiState.custom5gBands,
                        onUpdateCustom4gBands = onUpdateCustom4gBands,
                        onUpdateCustom5gBands = onUpdateCustom5gBands,
                        onSetNetworkSelect = onSetNetworkSelect,
                        onSet4gBandLock = onSet4gBandLock,
                        onSet5gBandLock = onSet5gBandLock,
                        onLock4gCell = onLock4gCell,
                        onUnlock4gCell = onUnlock4gCell,
                        onLock5gCell = onLock5gCell,
                        onUnlock5gCell = onUnlock5gCell,
                        onClearRadioMessage = onClearRadioMessage,
                        onGoToRouterLogin = {
                            settingsSubTab = 0
                            onSelectTab(DashboardNavTab.SETTINGS.ordinal)
                        }
                    )
                }

                DashboardNavTab.SETTINGS -> {
                    SettingsScreenContent(
                        selectedSubTab = settingsSubTab,
                        horizontalPad = horizontalPad,
                        onSubTabSelected = { settingsSubTab = it },
                        config = uiState.widgetConfig,
                        routerCreds = uiState.routerCredentials,
                        notifConfig = uiState.notificationConfig,
                        historyRetention = uiState.historyRetention,
                        isTestingLogin = uiState.isTestingLogin,
                        loginStatusMessage = uiState.loginStatusMessage,
                        onConfigChanged = onWidgetConfigChanged,
                        onRouterCredentialsChanged = onRouterCredentialsChanged,
                        onTestLogin = onTestRouterLogin,
                        onNotificationConfigChanged = onNotificationConfigChanged,
                        onHistoryRetentionChanged = onUpdateHistoryRetention,
                        onClearHistory = onClearHistory
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusBanner(
    signal: OduSignalState,
    routerHost: String,
    pollingInterval: Long,
    chartTimeRange: ChartTimeRange,
    isRefreshing: Boolean,
    onPollingIntervalSelected: (Long) -> Unit,
    onChartTimeRangeSelected: (ChartTimeRange) -> Unit,
    onRefresh: () -> Unit,
    onOpenRouterSettings: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val isOk = signal.isConnected && signal.errorMessage == null
    val statusColor = if (isOk) {
        if (isDark) SignalExcellent else SignalExcellentLight
    } else {
        MaterialTheme.colorScheme.error
    }
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    if (!isOk) {
        // Minimalist Utilitarian Pastel Red Alert Card (1dp border, 10dp radius, zero heavy shadow)
        val offlineBg = if (isDark) Color(0xFF2B1618) else Color(0xFFFDEBEC)
        val offlineBorder = if (isDark) Color(0xFF5C282B) else Color(0xFFF5C6C8)
        val offlineText = if (isDark) Color(0xFFFFB4AB) else Color(0xFF9F2F2D)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = offlineBg,
            border = BorderStroke(1.dp, offlineBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(offlineText)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ODU TERPUTUS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = offlineText
                        )
                    }

                    Text(
                        text = "$routerHost • ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(signal.lastUpdated))}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = offlineText.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = signal.errorMessage ?: "Tidak dapat menjangkau ZTE MC7500. Periksa koneksi Wi-Fi/LAN ke router.",
                    style = MaterialTheme.typography.bodySmall,
                    color = offlineText.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = offlineText,
                            contentColor = if (isDark) Color(0xFF1A1111) else Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
                    ) {
                        Text(
                            text = if (isRefreshing) "Menghubungi..." else "Coba Lagi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenRouterSettings,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = offlineText),
                        border = BorderStroke(1.dp, offlineBorder),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Cek IP Router", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    } else {
        // Minimalist Status & Control Strip (1dp border, 10dp radius, compact 2-row layout)
        val nextInterval = when (pollingInterval) {
            1000L -> 3000L
            3000L -> 5000L
            else -> 1000L
        }
        val intervalLabel = "${pollingInterval / 1000}s"

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = signal.provider,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${signal.connectionType} • ${signal.signalBar}/5",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Tap-to-cycle Refresh Rate Pill (>= 44dp touch target)
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onPollingIntervalSelected(nextInterval) },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REFRESH $intervalLabel",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 1.dp, color = borderColor.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Single-Row Minimalist Time Range Bar (1m | 5m | 15m | 1j | 6j | 24j) with >=44dp tap targets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartTimeRange.entries.forEach { rangeOption ->
                        val isSelected = chartTimeRange == rangeOption
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onChartTimeRangeSelected(rangeOption) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                Color.Transparent
                            },
                            border = if (isSelected) {
                                null
                            } else {
                                BorderStroke(1.dp, borderColor.copy(alpha = 0.6f))
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = rangeOption.shortLabel.uppercase(),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.surface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalTechCard(
    title: String,
    isConnected: Boolean,
    band: String,
    rsrp: Int,
    sinr: Float,
    rsrq: Int,
    pci: Int,
    cellId: Long,
    earfcnOrArfcn: Int? = null,
    dlFreqMhz: Float? = null,
    bandwidth: String? = null,
    history: List<OduSignalState>,
    chartTimeRange: ChartTimeRange,
    selectedMetric: ChartMetric,
    onMetricSelected: (ChartMetric) -> Unit,
    is5g: Boolean
) {
    val hasValidSignal = isConnected && rsrp != 0
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Editorial Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val cleanBand = when {
                    band.startsWith("LTE BAND ", ignoreCase = true) -> "B" + band.substringAfter("LTE BAND ", "").trim()
                    band.startsWith("BAND ", ignoreCase = true) -> "B" + band.substringAfter("BAND ", "").trim()
                    else -> band
                }
                val freqPart = dlFreqMhz?.let { " • ${it.toInt()} MHz" } ?: ""
                val displayBand = when {
                    !isConnected -> "OFFLINE"
                    cleanBand.isNotBlank() && cleanBand != "-" -> {
                        val prefix = if (cleanBand.startsWith("B", true) || cleanBand.startsWith("n", true)) "" else "B"
                        "$prefix$cleanBand$freqPart"
                    }
                    else -> "STANDBY"
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isConnected) {
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = displayBand,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 1.dp, color = borderColor.copy(alpha = 0.5f))

            // Flat 3-Column Interactive Bento Metric Strip (Tapping any metric switches the chart below; >=56dp height)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricColumnItem(
                    label = "RSRP",
                    valueText = if (hasValidSignal) "$rsrp" else "--",
                    unitText = "dBm",
                    valueColor = if (hasValidSignal) adaptiveRsrpColor(rsrp) else MaterialTheme.colorScheme.onSurfaceVariant,
                    isSelected = selectedMetric == ChartMetric.RSRP,
                    onClick = { onMetricSelected(ChartMetric.RSRP) },
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(borderColor.copy(alpha = 0.5f))
                )

                MetricColumnItem(
                    label = "SINR",
                    valueText = if (hasValidSignal) "$sinr" else "--",
                    unitText = "dB",
                    valueColor = if (hasValidSignal) adaptiveSinrColor(sinr) else MaterialTheme.colorScheme.onSurfaceVariant,
                    isSelected = selectedMetric == ChartMetric.SINR,
                    onClick = { onMetricSelected(ChartMetric.SINR) },
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(borderColor.copy(alpha = 0.5f))
                )

                MetricColumnItem(
                    label = "RSRQ",
                    valueText = if (hasValidSignal) "$rsrq" else "--",
                    unitText = "dB",
                    valueColor = if (hasValidSignal) adaptiveRsrqColor(rsrq) else MaterialTheme.colorScheme.onSurfaceVariant,
                    isSelected = selectedMetric == ChartMetric.RSRQ,
                    onClick = { onMetricSelected(ChartMetric.RSRQ) },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(thickness = 1.dp, color = borderColor.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Minimalist Frameless Sparkline Chart
            SignalLineChart(
                history = history,
                isConnected = isConnected,
                metric = selectedMetric,
                chartTimeRange = chartTimeRange,
                is5g = is5g
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 1.dp, color = borderColor.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Monospace Technical Metadata Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val chanPrefix = if (is5g) "ARFCN" else "EARFCN"
                val chanText = if (isConnected && earfcnOrArfcn != null) "$chanPrefix $earfcnOrArfcn" else "$chanPrefix -"
                val bwText = if (isConnected && bandwidth != null) " • $bandwidth" else ""
                Text(
                    text = "$chanText$bwText",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isConnected) {
                        "PCI ${if (pci > 0) pci else "-"} • CELL ${if (cellId > 0) cellId else "-"}"
                    } else {
                        "PCI - • CELL -"
                    },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricColumnItem(
    label: String,
    valueText: String,
    unitText: String = "",
    valueColor: Color,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 58.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.8.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = valueText,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
            if (unitText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unitText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

private data class TimedMetricPoint(
    val timestampMs: Long,
    val value: Float
)

@Composable
fun SignalLineChart(
    history: List<OduSignalState>,
    isConnected: Boolean,
    metric: ChartMetric,
    chartTimeRange: ChartTimeRange = ChartTimeRange.FIVE_MINUTES,
    is5g: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val dotCenterColor = MaterialTheme.colorScheme.surface

    val timedPoints = remember(history, metric, is5g, isConnected) {
        if (!isConnected) {
            emptyList()
        } else {
            history.mapNotNull { state ->
                val raw = if (is5g) {
                    when (metric) {
                        ChartMetric.RSRQ -> state.nrRsrq.toFloat()
                        ChartMetric.RSRP -> state.nrRsrp.toFloat()
                        ChartMetric.SINR -> state.nrSinr
                    }
                } else {
                    when (metric) {
                        ChartMetric.RSRQ -> state.lteRsrq.toFloat()
                        ChartMetric.RSRP -> state.lteRsrp.toFloat()
                        ChartMetric.SINR -> state.lteSinr
                    }
                }
                // Filter out invalid 0 dBm RSRP readings so they never spike the chart to 0
                if (metric == ChartMetric.RSRP && raw == 0f) {
                    null
                } else {
                    TimedMetricPoint(state.lastUpdated, raw)
                }
            }
        }
    }

    if (timedPoints.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (!isConnected) "OFFLINE — GRAFIK DIJEDA" else "MENGUMPULKAN DATA...",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val minVal = timedPoints.minOf { it.value }
    val maxVal = timedPoints.maxOf { it.value }
    val avgVal = timedPoints.map { it.value.toDouble() }.average().toFloat()
    val currentVal = timedPoints.last().value

    // Ratcheted Y-Axis: locks scale and only expands when a new reading exceeds the current bounds
    var lockedYMin by remember(chartTimeRange, metric, is5g) { mutableFloatStateOf(Float.NaN) }
    var lockedYMax by remember(chartTimeRange, metric, is5g) { mutableFloatStateOf(Float.NaN) }

    val minSpan = if (metric == ChartMetric.RSRQ) 12f else 16f
    if (lockedYMin.isNaN() || lockedYMax.isNaN()) {
        val mid = (minVal + maxVal) / 2f
        val halfSpan = maxOf((maxVal - minVal) * 0.65f, minSpan / 2f)
        lockedYMin = kotlin.math.floor(mid - halfSpan)
        lockedYMax = kotlin.math.ceil(mid + halfSpan)
    } else {
        if (minVal < lockedYMin + 1f) {
            lockedYMin = kotlin.math.floor(minVal - 2f)
        }
        if (maxVal > lockedYMax - 1f) {
            lockedYMax = kotlin.math.ceil(maxVal + 2f)
        }
    }

    val yMin = lockedYMin
    val yMax = lockedYMax
    val totalYSpan = (yMax - yMin).coerceAtLeast(minSpan)

    val metricColor = when (metric) {
        ChartMetric.RSRQ -> getRsrqColor(currentVal.toInt(), isDark)
        ChartMetric.RSRP -> getRsrpColor(currentVal.toInt(), isDark)
        ChartMetric.SINR -> getSinrColor(currentVal, isDark)
    }

    val windowEndMs = timedPoints.last().timestampMs
    val windowDurationMs = chartTimeRange.durationMillis.coerceAtLeast(1000L)
    val windowStartMs = windowEndMs - windowDurationMs
    // Break line if there is an offline gap larger than 4 buckets (or at least 15s)
    val maxGapMs = maxOf((windowDurationMs / 60L) * 4L, 15_000L)

    Column {
        // Y-Axis Ceiling & Timeline Start Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "-${chartTimeRange.shortLabel.uppercase()}",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "SKALA ${yMin.toInt()}..${yMax.toInt()} ${metric.unit}",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "0s",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(vertical = 4.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val gridColor = outlineColor.copy(alpha = 0.35f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, 0f),
                    end = Offset(width, 0f),
                    strokeWidth = 1f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, height / 2f),
                    end = Offset(width, height / 2f),
                    strokeWidth = 1f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, height),
                    end = Offset(width, height),
                    strokeWidth = 1f
                )

                // Map each point to its true proportional timestamp coordinate on [windowStartMs..windowEndMs]
                val mappedPoints = timedPoints.map { pt ->
                    val timeFraction = ((pt.timestampMs - windowStartMs).toFloat() / windowDurationMs.toFloat()).coerceIn(0f, 1f)
                    val x = timeFraction * width
                    val normalizedY = ((pt.value - yMin) / totalYSpan).coerceIn(0f, 1f)
                    val y = height - (normalizedY * height)
                    Pair(pt.timestampMs, Offset(x, y))
                }

                if (mappedPoints.size == 1) {
                    val p = mappedPoints.first().second
                    drawCircle(color = metricColor, radius = 3.5.dp.toPx(), center = p)
                    return@Canvas
                }

                // Build contiguous segments (breaking across offline gaps) using exact linear segments
                val strokePath = Path()
                val fillPath = Path()
                var segmentStartX = mappedPoints.first().second.x

                strokePath.moveTo(mappedPoints.first().second.x, mappedPoints.first().second.y)
                fillPath.moveTo(mappedPoints.first().second.x, height)
                fillPath.lineTo(mappedPoints.first().second.x, mappedPoints.first().second.y)

                for (i in 1 until mappedPoints.size) {
                    val prev = mappedPoints[i - 1]
                    val curr = mappedPoints[i]
                    if (curr.first - prev.first > maxGapMs) {
                        fillPath.lineTo(prev.second.x, height)
                        fillPath.lineTo(segmentStartX, height)
                        fillPath.close()

                        segmentStartX = curr.second.x
                        strokePath.moveTo(curr.second.x, curr.second.y)
                        fillPath.moveTo(curr.second.x, height)
                        fillPath.lineTo(curr.second.x, curr.second.y)
                    } else {
                        strokePath.lineTo(curr.second.x, curr.second.y)
                        fillPath.lineTo(curr.second.x, curr.second.y)
                    }
                }

                fillPath.lineTo(mappedPoints.last().second.x, height)
                fillPath.lineTo(segmentStartX, height)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            metricColor.copy(alpha = 0.16f),
                            metricColor.copy(alpha = 0.0f)
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                drawPath(
                    path = strokePath,
                    color = metricColor,
                    style = Stroke(
                        width = 1.75.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                val latestPoint = mappedPoints.last().second
                drawCircle(
                    color = metricColor,
                    radius = 3.5.dp.toPx(),
                    center = latestPoint
                )
                drawCircle(
                    color = dotCenterColor,
                    radius = 1.5.dp.toPx(),
                    center = latestPoint
                )
            }
        }

        val avgFormatted = if (metric == ChartMetric.SINR) {
            String.format(Locale.US, "%.1f", avgVal)
        } else {
            avgVal.toInt().toString()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${metric.label} (${chartTimeRange.shortLabel.uppercase()})",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "MIN ${minVal.toInt()} • AVG $avgFormatted • MAX ${maxVal.toInt()} ${metric.unit}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CarrierAggregationCard(signal: OduSignalState) {
    val isDark = isSystemInDarkTheme()
    val activeColor = if (isDark) SignalExcellent else SignalExcellentLight
    val isConnected = signal.isConnected
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CARRIER AGGREGATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isConnected && signal.isCaActive) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = when {
                            !isConnected -> "OFFLINE"
                            signal.isCaActive -> "ACTIVE"
                            else -> "INACTIVE"
                        },
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected && signal.isCaActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            if (isConnected && signal.isCaActive && signal.caDetails != "-") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = signal.caDetails,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp, color = borderColor.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isConnected) "NEIGHBOR CELLS: ${signal.neighborCells}" else "NEIGHBOR CELLS: -",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RouterTrafficCard(
    signal: OduSignalState,
    history: List<OduSignalState>,
    chartTimeRange: ChartTimeRange,
    isExpandedWidth: Boolean,
    onOpenSettings: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val ulColor = if (isDark) SignalExcellent else SignalExcellentLight
    val isConnected = signal.isConnected
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Minimalist Gateway Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "THROUGHPUT & GATEWAY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onOpenSettings),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !isConnected -> MaterialTheme.colorScheme.error
                                        signal.isRouterLoggedIn -> ulColor
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                !isConnected -> "OFFLINE"
                                signal.isRouterLoggedIn -> "ADMIN"
                                else -> "GUEST RF"
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp, color = borderColor.copy(alpha = 0.5f))

            // Flat 2-Column Hero Speed Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = "↓ DOWNLOAD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isConnected) formatSpeed(signal.downloadSpeedBps) else "-- Mbps",
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(borderColor.copy(alpha = 0.5f))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "↑ UPLOAD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isConnected) formatSpeed(signal.uploadSpeedBps) else "-- Mbps",
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) ulColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = borderColor.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            ThroughputTrendChart(
                history = history,
                chartTimeRange = chartTimeRange,
                isConnected = isConnected
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 1.dp, color = borderColor.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Flat Bento Grid for Device Telemetry (3 columns on phone & tablet, compact monospace)
            val metrics = listOf(
                "WAN IP" to (if (isConnected) (signal.wanIp ?: "-") else "-"),
                "SIM NO" to (if (isConnected) (signal.simPhoneNumber ?: "-") else "-"),
                "CPU TEMP" to (if (isConnected) (signal.cpuTemp ?: "Normal") else "-"),
                "TODAY" to (if (isConnected && signal.dayRxBytes > 0) formatBytes(signal.dayRxBytes) else "-"),
                "MONTH" to (if (isConnected && signal.monthRxBytes > 0) formatBytes(signal.monthRxBytes) else "-"),
                "UPTIME" to (if (isConnected) (signal.deviceUptimeSeconds?.let { com.example.odumonitor.util.FrequencyConverter.formatUptime(it) } ?: "-") else "-")
            )

            val cols = if (isExpandedWidth) 3 else 2
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                metrics.chunked(cols).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { (lbl, value) ->
                            RouterMetricItem(
                                label = lbl,
                                value = value,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThroughputTrendChart(
    history: List<OduSignalState>,
    chartTimeRange: ChartTimeRange,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val dlColor = MaterialTheme.colorScheme.primary
    val ulColor = if (isDark) SignalExcellent else SignalExcellentLight
    val guideColor = MaterialTheme.colorScheme.outlineVariant

    val dlPoints = if (isConnected) history.map { TimedMetricPoint(it.lastUpdated, it.downloadSpeedBps.toFloat()) } else emptyList()
    val ulPoints = if (isConnected) history.map { TimedMetricPoint(it.lastUpdated, it.uploadSpeedBps.toFloat()) } else emptyList()

    val avgDl = if (dlPoints.isNotEmpty()) dlPoints.map { it.value.toDouble() }.average().toLong() else 0L
    val actualMax = maxOf(
        dlPoints.maxOfOrNull { it.value } ?: 0f,
        ulPoints.maxOfOrNull { it.value } ?: 0f
    )

    // Ratcheted Y-Axis Ceiling: starts at 5 Mbps (625,000 Bytes/s) minimum and only expands when a new peak exceeds it
    val minCeilingBps = 625_000f // 5.0 Mbps
    var lockedCeilingBps by remember(chartTimeRange) { mutableFloatStateOf(minCeilingBps) }
    if (actualMax > lockedCeilingBps) {
        lockedCeilingBps = actualMax * 1.12f
    }
    val maxVal = lockedCeilingBps.coerceAtLeast(minCeilingBps)

    val windowEndMs = dlPoints.lastOrNull()?.timestampMs ?: System.currentTimeMillis()
    val windowDurationMs = chartTimeRange.durationMillis.coerceAtLeast(1000L)
    val windowStartMs = windowEndMs - windowDurationMs
    val maxGapMs = maxOf((windowDurationMs / 60L) * 4L, 15_000L)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "-${chartTimeRange.shortLabel.uppercase()}",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "SKALA 0..${formatSpeed(maxVal.toLong())}",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "0s",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(vertical = 4.dp)
        ) {
            if (!isConnected || history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (!isConnected) "OFFLINE — TRAFIK DIJEDA" else "MENGUMPULKAN SAMPEL...",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val gridColor = guideColor.copy(alpha = 0.35f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, 0f),
                        end = Offset(w, 0f),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, h * 0.5f),
                        end = Offset(w, h * 0.5f),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, h),
                        end = Offset(w, h),
                        strokeWidth = 1f
                    )

                    fun toTimedOffsets(pts: List<TimedMetricPoint>): List<Pair<Long, Offset>> {
                        return pts.map { pt ->
                            val timeFraction = ((pt.timestampMs - windowStartMs).toFloat() / windowDurationMs.toFloat()).coerceIn(0f, 1f)
                            val x = timeFraction * w
                            val norm = (pt.value / maxVal).coerceIn(0f, 1f)
                            val y = h - (norm * (h - 4.dp.toPx())) - 2.dp.toPx()
                            Pair(pt.timestampMs, Offset(x, y))
                        }
                    }

                    fun drawLinearSeries(mapped: List<Pair<Long, Offset>>, color: Color) {
                        if (mapped.isEmpty()) return
                        if (mapped.size == 1) {
                            drawCircle(color = color, radius = 3.dp.toPx(), center = mapped.first().second)
                            return
                        }

                        val linePath = Path()
                        val fillPath = Path()
                        var segStartX = mapped.first().second.x

                        linePath.moveTo(mapped.first().second.x, mapped.first().second.y)
                        fillPath.moveTo(mapped.first().second.x, h)
                        fillPath.lineTo(mapped.first().second.x, mapped.first().second.y)

                        for (i in 1 until mapped.size) {
                            val prev = mapped[i - 1]
                            val curr = mapped[i]
                            if (curr.first - prev.first > maxGapMs) {
                                fillPath.lineTo(prev.second.x, h)
                                fillPath.lineTo(segStartX, h)
                                fillPath.close()

                                segStartX = curr.second.x
                                linePath.moveTo(curr.second.x, curr.second.y)
                                fillPath.moveTo(curr.second.x, h)
                                fillPath.lineTo(curr.second.x, curr.second.y)
                            } else {
                                linePath.lineTo(curr.second.x, curr.second.y)
                                fillPath.lineTo(curr.second.x, curr.second.y)
                            }
                        }

                        fillPath.lineTo(mapped.last().second.x, h)
                        fillPath.lineTo(segStartX, h)
                        fillPath.close()

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.16f), color.copy(alpha = 0.0f))
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = color,
                            style = Stroke(
                                width = 1.75.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        drawCircle(
                            color = color,
                            radius = 3.dp.toPx(),
                            center = mapped.last().second
                        )
                    }

                    drawLinearSeries(toTimedOffsets(dlPoints), dlColor)
                    drawLinearSeries(toTimedOffsets(ulPoints), ulColor)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRAFFIC (${chartTimeRange.shortLabel.uppercase()})",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "AVG ${if (isConnected) formatSpeed(avgDl) else "--"} • PEAK ${if (isConnected) formatSpeed(actualMax.toLong()) else "--"}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RouterMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioControlScreen(
    signalState: OduSignalState,
    horizontalPad: androidx.compose.ui.unit.Dp,
    isExecutingRadioCommand: Boolean,
    radioCommandMessage: String?,
    custom4gBands: Set<Int> = setOf(1, 3, 8, 40),
    custom5gBands: Set<Int> = setOf(1, 3, 28, 40, 78),
    onUpdateCustom4gBands: (Set<Int>) -> Unit = {},
    onUpdateCustom5gBands: (Set<Int>) -> Unit = {},
    onSetNetworkSelect: (String) -> Unit,
    onSet4gBandLock: (String, String) -> Unit,
    onSet5gBandLock: (String, String) -> Unit,
    onLock4gCell: (Int, Int) -> Unit,
    onUnlock4gCell: () -> Unit,
    onLock5gCell: (Int, Int, Int) -> Unit,
    onUnlock5gCell: () -> Unit,
    onClearRadioMessage: () -> Unit,
    onGoToRouterLogin: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val warningColor = if (isDark) SignalFair else SignalFairLight
    var pendingConfirmation by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    if (pendingConfirmation != null) {
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            title = {
                Text(
                    text = "Konfirmasi Pengaturan Radio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = pendingConfirmation!!.first,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val action = pendingConfirmation!!.second
                        pendingConfirmation = null
                        action()
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Terapkan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingConfirmation = null },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPad, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!radioCommandMessage.isNullOrBlank()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isExecutingRadioCommand) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isExecutingRadioCommand) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = radioCommandMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isExecutingRadioCommand) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                    if (!isExecutingRadioCommand) {
                        IconButton(
                            onClick = onClearRadioMessage,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup Pesan",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!signalState.isConnected || !signalState.isRouterLoggedIn) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (!signalState.isConnected) Icons.Default.WifiOff else Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (!signalState.isConnected) {
                                "ODU Terputus (Tidak Terjangkau)"
                            } else {
                                "Perlu Akses Admin Router"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (!signalState.isConnected) {
                            "Perangkat belum terhubung ke ZTE MC7500. Sambungkan Wi-Fi/LAN ke router ODU terlebih dahulu sebelum mengubah pengaturan radio dan band."
                        } else {
                            "Fitur kendali radio membutuhkan sesi login admin router aktif. Silakan masuk ke Pengaturan Router untuk menguji login."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = onGoToRouterLogin,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Buka Pengaturan Login Router")
                    }
                }
            }
        } else {
            // Card 1: Mode Jaringan (Bearer)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. MODE JARINGAN (BEARER)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mode aktif: ${com.example.odumonitor.util.FrequencyConverter.formatBearerModeName(signalState.netSelect)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val bearerModes = listOf(
                        Triple("Auto (5G/4G/3G)", "WL_AND_5G", "Beralih ke mode otomatis (5G + 4G + 3G)?"),
                        Triple("5G NSA + 4G LTE", "LTE_AND_5G", "Beralih ke mode 5G NSA + 4G LTE?"),
                        Triple("5G SA Saja", "Only_5G", "Beralih ke 5G SA Saja? Pastikan kartu SIM & BTS mendukung 5G Standalone."),
                        Triple("4G LTE Saja", "Only_LTE", "Beralih ke 4G LTE Saja? Modem tidak akan menggunakan sinyal 5G.")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bearerModes.chunked(2).forEach { rowModes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowModes.forEach { (label, modeCode, promptText) ->
                                    val isCurrent = signalState.netSelect == modeCode
                                    FilterChip(
                                        selected = isCurrent,
                                        onClick = {
                                            pendingConfirmation = Pair(promptText) {
                                                onSetNetworkSelect(modeCode)
                                            }
                                        },
                                        enabled = !isExecutingRadioCommand,
                                        label = {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: Kunci Band 4G LTE
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. KUNCI BAND 4G LTE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val active4g = signalState.lteBandLock?.toLongOrNull()?.let {
                        com.example.odumonitor.util.FrequencyConverter.parse4gActiveBands(it)
                    } ?: emptyList()
                    val active4gLabel = if (active4g.isEmpty() || active4g.size >= 10) {
                        "Semua Band (Auto)"
                    } else {
                        "B" + active4g.joinToString(", B")
                    }
                    Text(
                        text = "Status Kunci: $active4gLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Profil Operator Indonesia:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val operatorProfiles = listOf(
                        Triple("Telkomsel (B1,3,8,40)", listOf(1, 3, 8, 40), "Telkomsel (B1, B3, B8, B40)"),
                        Triple("XL Axiata (B1,3,8)", listOf(1, 3, 8), "XL Axiata (B1, B3, B8)"),
                        Triple("Indosat (B1,3,8)", listOf(1, 3, 8), "Indosat (B1, B3, B8)"),
                        Triple("Smartfren (B28,40)", listOf(28, 40), "Smartfren (B28, B40)")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        operatorProfiles.chunked(2).forEach { rowOps ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowOps.forEach { (btnText, bands, desc) ->
                                    val mask = com.example.odumonitor.util.FrequencyConverter.calculate4gBandMask(bands).toString()
                                    OutlinedButton(
                                        onClick = {
                                            pendingConfirmation = Pair("Kunci band 4G ke profil $desc? Modem akan memutus dan menyambung ulang.") {
                                                onSet4gBandLock(mask, desc)
                                            }
                                        },
                                        enabled = !isExecutingRadioCommand,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = btnText,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Pilihan Band Tunggal 4G:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val single4gBands = listOf(
                        Pair("Band 1", 1),
                        Pair("Band 3", 3),
                        Pair("Band 8", 8),
                        Pair("Band 40", 40),
                        Pair("Band 28", 28)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        single4gBands.chunked(3).forEach { rowBands ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowBands.forEach { (lbl, bandNum) ->
                                    val mask = (1L shl (bandNum - 1)).toString()
                                    FilledTonalButton(
                                        onClick = {
                                            pendingConfirmation = Pair("Kunci modem hanya ke 4G Band $bandNum?") {
                                                onSet4gBandLock(mask, "Band $bandNum")
                                            }
                                        },
                                        enabled = !isExecutingRadioCommand,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(text = lbl, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Kombinasi Band Kustom 4G (Tersimpan Otomatis):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val custom4gCandidates = listOf(1, 3, 5, 8, 28, 40)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        custom4gCandidates.chunked(3).forEach { rowCandidates ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowCandidates.forEach { bNum ->
                                    val isSelected = custom4gBands.contains(bNum)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val updated = if (isSelected && custom4gBands.size > 1) {
                                                custom4gBands - bNum
                                            } else {
                                                custom4gBands + bNum
                                            }
                                            onUpdateCustom4gBands(updated)
                                        },
                                        label = { Text("B$bNum", fontSize = 12.sp) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 44.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val sortedCustom4g = custom4gBands.sorted()
                    val custom4gDesc = "Kustom (" + sortedCustom4g.joinToString(", ") { "B$it" } + ")"
                    val custom4gMask = com.example.odumonitor.util.FrequencyConverter.calculate4gBandMask(sortedCustom4g).toString()

                    Button(
                        onClick = {
                            pendingConfirmation = Pair("Kunci kombinasi band 4G ke $custom4gDesc?") {
                                onSet4gBandLock(custom4gMask, custom4gDesc)
                            }
                        },
                        enabled = !isExecutingRadioCommand && sortedCustom4g.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Terapkan $custom4gDesc", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            pendingConfirmation = Pair("Buka kunci dan aktifkan semua band 4G (Auto)?") {
                                onSet4gBandLock(com.example.odumonitor.util.FrequencyConverter.FULL_4G_MASK.toString(), "Auto / Semua")
                            }
                        },
                        enabled = !isExecutingRadioCommand,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Buka Semua Band 4G (Auto)", fontSize = 12.sp)
                    }
                }
            }

            // Card 3: Kunci Band 5G NR (SA)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. KUNCI BAND 5G NR (SA)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pilih band 5G Standalone (n40 Telkomsel/Smartfren, n1, n3, n78):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val single5gBands = listOf("40", "1", "3", "78")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        single5gBands.chunked(2).forEach { rowBands ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowBands.forEach { b ->
                                    FilledTonalButton(
                                        onClick = {
                                            pendingConfirmation = Pair("Kunci frekuensi 5G SA ke band n$b?") {
                                                onSet5gBandLock(b, "n$b")
                                            }
                                        },
                                        enabled = !isExecutingRadioCommand,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(text = "Band n$b", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Kombinasi Band Kustom 5G (Tersimpan Otomatis):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val custom5gCandidates = listOf(1, 3, 28, 40, 78)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        custom5gCandidates.chunked(3).forEach { rowCandidates ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowCandidates.forEach { bNum ->
                                    val isSelected = custom5gBands.contains(bNum)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val updated = if (isSelected && custom5gBands.size > 1) {
                                                custom5gBands - bNum
                                            } else {
                                                custom5gBands + bNum
                                            }
                                            onUpdateCustom5gBands(updated)
                                        },
                                        label = { Text("n$bNum", fontSize = 12.sp) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 44.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val sortedCustom5g = custom5gBands.sorted()
                    val custom5gDesc = "Kustom (" + sortedCustom5g.joinToString(", ") { "n$it" } + ")"
                    val custom5gPayload = sortedCustom5g.joinToString(",")

                    Button(
                        onClick = {
                            pendingConfirmation = Pair("Kunci kombinasi band 5G ke $custom5gDesc?") {
                                onSet5gBandLock(custom5gPayload, custom5gDesc)
                            }
                        },
                        enabled = !isExecutingRadioCommand && sortedCustom5g.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Terapkan $custom5gDesc", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            pendingConfirmation = Pair("Buka kunci dan aktifkan semua band 5G NR (Auto)?") {
                                val full = com.example.odumonitor.util.FrequencyConverter.FULL_5G_BANDS.joinToString(",")
                                onSet5gBandLock(full, "Auto / Semua")
                            }
                        },
                        enabled = !isExecutingRadioCommand,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Buka Semua Band 5G (Auto)", fontSize = 12.sp)
                    }
                }
            }

            // Card 4: Cell Lock (PCI & Channel)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. KUNCI PEMANCAR SEL (CELL LOCK)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kunci antena ke menara BTS aktif untuk mencegah perpindahan pemancar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val lock4gRaw = signalState.lockLteCell
                    val is4gCellLocked = !lock4gRaw.isNullOrBlank() && lock4gRaw != "0,0"
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sel 4G LTE",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (is4gCellLocked) "Terkunci ($lock4gRaw)" else "Bebas (Auto)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (is4gCellLocked) warningColor else MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val canLock4g = signalState.ltePci > 0 && signalState.lteEarfcn != null
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val pci = signalState.ltePci
                                        val earfcn = signalState.lteEarfcn ?: 0
                                        pendingConfirmation = Pair("Kunci permanen ke pemancar 4G PCI $pci (EARFCN $earfcn)?") {
                                            onLock4gCell(pci, earfcn)
                                        }
                                    },
                                    enabled = !isExecutingRadioCommand && canLock4g,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (canLock4g) "Kunci PCI ${signalState.ltePci}" else "Sel 4G Kosong",
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        pendingConfirmation = Pair("Buka kunci sel 4G agar modem dapat memilih BTS bebas secara otomatis?") {
                                            onUnlock4gCell()
                                        }
                                    },
                                    enabled = !isExecutingRadioCommand && is4gCellLocked,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(text = "Buka Kunci", fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val lock5gRaw = signalState.lockNrCell
                    val is5gCellLocked = !lock5gRaw.isNullOrBlank() && lock5gRaw != "0,0,0"
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sel 5G NR",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (is5gCellLocked) "Terkunci ($lock5gRaw)" else "Bebas (Auto)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (is5gCellLocked) warningColor else MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val canLock5g = signalState.nrPci > 0 && signalState.nrArfcn != null
                            val nrBandNumber = signalState.nrBand.replace("n", "").trim().toIntOrNull() ?: 40
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val pci = signalState.nrPci
                                        val arfcn = signalState.nrArfcn ?: 0
                                        pendingConfirmation = Pair("Kunci permanen ke pemancar 5G PCI $pci (ARFCN $arfcn, n$nrBandNumber)?") {
                                            onLock5gCell(pci, arfcn, nrBandNumber)
                                        }
                                    },
                                    enabled = !isExecutingRadioCommand && canLock5g,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (canLock5g) "Kunci PCI ${signalState.nrPci}" else "Sel 5G Kosong",
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        pendingConfirmation = Pair("Buka kunci sel 5G agar modem dapat memilih BTS bebas secara otomatis?") {
                                            onUnlock5gCell()
                                        }
                                    },
                                    enabled = !isExecutingRadioCommand && is5gCellLocked,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(text = "Buka Kunci", fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "* Catatan: Jika modem kehilangan sinyal setelah mengunci sel, tekan tombol 'Buka Kunci' lalu ubah mode jaringan atau tunggu modem sinkronisasi ulang.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    selectedSubTab: Int,
    horizontalPad: androidx.compose.ui.unit.Dp,
    onSubTabSelected: (Int) -> Unit,
    config: WidgetConfig,
    routerCreds: RouterCredentials,
    notifConfig: NotificationConfig,
    historyRetention: HistoryRetention = HistoryRetention.TWENTY_FOUR_HOURS,
    isTestingLogin: Boolean,
    loginStatusMessage: String?,
    onConfigChanged: (WidgetConfig) -> Unit,
    onRouterCredentialsChanged: (RouterCredentials) -> Unit,
    onTestLogin: (RouterCredentials) -> Unit,
    onNotificationConfigChanged: (NotificationConfig) -> Unit,
    onHistoryRetentionChanged: (HistoryRetention) -> Unit = {},
    onClearHistory: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val okColor = if (isDark) SignalExcellent else SignalExcellentLight

    var hostInput by remember(routerCreds) { mutableStateOf(routerCreds.host) }
    var usernameInput by remember(routerCreds) { mutableStateOf(routerCreds.username) }
    var passwordInput by remember(routerCreds) { mutableStateOf(routerCreds.password) }
    var isLoginEnabled by remember(routerCreds) { mutableStateOf(routerCreds.isLoginEnabled) }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPad, vertical = 12.dp)
    ) {
        val tabTitles = listOf("Router", "Widget", "Notifikasi")
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            tabTitles.forEachIndexed { idx, title ->
                SegmentedButton(
                    selected = selectedSubTab == idx,
                    onClick = { onSubTabSelected(idx) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = idx,
                        count = tabTitles.size
                    )
                ) {
                    Text(text = title, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (selectedSubTab) {
                    0 -> {
                        Text(
                            text = "Kredensial & Autentikasi Router",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Login memungkinkan pengambilan kuota, kecepatan real-time, dan status WAN",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aktifkan Login Otomatis",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = isLoginEnabled,
                                onCheckedChange = {
                                    isLoginEnabled = it
                                    onRouterCredentialsChanged(
                                        routerCreds.copy(
                                            host = hostInput,
                                            username = usernameInput,
                                            password = passwordInput,
                                            isLoginEnabled = it
                                        )
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = { hostInput = it },
                            label = { Text("IP Host / URL Router") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password Web GUI") },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(
                                    onClick = { showPassword = !showPassword },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Tampilkan Password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val newCreds = RouterCredentials(
                                    host = hostInput,
                                    username = usernameInput,
                                    password = passwordInput,
                                    isLoginEnabled = isLoginEnabled
                                )
                                onRouterCredentialsChanged(newCreds)
                                onTestLogin(newCreds)
                            },
                            enabled = !isTestingLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isTestingLogin) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mengautentikasi...", fontWeight = FontWeight.Bold)
                            } else {
                                Text("Simpan & Uji Login", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (loginStatusMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val isSuccess = loginStatusMessage.startsWith("Berhasil")
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSuccess) okColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = loginStatusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSuccess) okColor else MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    1 -> {
                        Text(
                            text = "Pengaturan Homescreen Widget",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pilih metrik yang ingin ditampilkan pada widget 2x2 dan 4x2",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        WidgetToggleItem(
                            label = "Tampilkan Sinyal 5G",
                            checked = config.show5g,
                            onCheckedChange = { onConfigChanged(config.copy(show5g = it)) }
                        )
                        WidgetToggleItem(
                            label = "Tampilkan Sinyal 4G LTE",
                            checked = config.show4g,
                            onCheckedChange = { onConfigChanged(config.copy(show4g = it)) }
                        )
                        WidgetToggleItem(
                            label = "Status Provider & Koneksi",
                            checked = config.showProviderStatus,
                            onCheckedChange = { onConfigChanged(config.copy(showProviderStatus = it)) }
                        )
                        WidgetToggleItem(
                            label = "ID Pemancar (PCI & Cell ID)",
                            checked = config.showPciTower,
                            onCheckedChange = { onConfigChanged(config.copy(showPciTower = it)) }
                        )
                        WidgetToggleItem(
                            label = "Informasi Band Frekuensi",
                            checked = config.showBandInfo,
                            onCheckedChange = { onConfigChanged(config.copy(showBandInfo = it)) }
                        )
                        WidgetToggleItem(
                            label = "Kejernihan Sinyal (SINR & RSRQ)",
                            checked = config.showSinrRsrq,
                            onCheckedChange = { onConfigChanged(config.copy(showSinrRsrq = it)) }
                        )
                        WidgetToggleItem(
                            label = "Waktu Update Terakhir",
                            checked = config.showTimestamp,
                            onCheckedChange = { onConfigChanged(config.copy(showTimestamp = it)) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Interval Sinkronisasi Latar Belakang",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val intervals = listOf(
                            15 to "15 Menit",
                            30 to "30 Menit",
                            60 to "1 Jam",
                            180 to "3 Jam",
                            360 to "6 Jam",
                            -1 to "Manual"
                        )

                        intervals.forEach { (min, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { onConfigChanged(config.copy(updateIntervalMinutes = min)) }
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                RadioButton(
                                    selected = config.updateIntervalMinutes == min,
                                    onClick = { onConfigChanged(config.copy(updateIntervalMinutes = min)) }
                                )
                            }
                        }
                    }

                    2 -> {
                        Text(
                            text = "Layanan & Peringatan Latar Belakang",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Deteksi perubahan jaringan real-time dan kirim notifikasi saat aplikasi ditutup",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        WidgetToggleItem(
                            label = "Layanan Pemantauan Aktif (Foreground Service)",
                            checked = notifConfig.isServiceEnabled,
                            onCheckedChange = { onNotificationConfigChanged(notifConfig.copy(isServiceEnabled = it)) }
                        )
                        WidgetToggleItem(
                            label = "Peringatan Perubahan Band (4G / 5G)",
                            checked = notifConfig.notifyBandChange,
                            onCheckedChange = { onNotificationConfigChanged(notifConfig.copy(notifyBandChange = it)) }
                        )
                        WidgetToggleItem(
                            label = "Peringatan Perubahan Alamat WAN IP",
                            checked = notifConfig.notifyWanIpChange,
                            onCheckedChange = { onNotificationConfigChanged(notifConfig.copy(notifyWanIpChange = it)) }
                        )
                        WidgetToggleItem(
                            label = "Peringatan ODU Terputus / Offline",
                            checked = notifConfig.notifyOduOffline,
                            onCheckedChange = { onNotificationConfigChanged(notifConfig.copy(notifyOduOffline = it)) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Retensi Riwayat Grafik (Database SQLite)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Menentukan berapa lama sampel grafik disimpan untuk rentang 1m hingga 24j",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        HistoryRetention.entries.forEach { retentionOption ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { onHistoryRetentionChanged(retentionOption) }
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = retentionOption.label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                RadioButton(
                                    selected = historyRetention == retentionOption,
                                    onClick = { onHistoryRetentionChanged(retentionOption) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onClearHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bersihkan Semua Riwayat Grafik", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "ZTE ODU Monitor v${com.example.odumonitor.BuildConfig.VERSION_NAME} (Build ${com.example.odumonitor.BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun WidgetToggleItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
