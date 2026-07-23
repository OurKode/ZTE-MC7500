package com.example.odumonitor.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.odumonitor.data.local.HistoryRetention
import com.example.odumonitor.data.local.WidgetConfig
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onPollingIntervalSelected: (Long) -> Unit,
    onTabSelected: (Int) -> Unit,
    onHistoryRetentionSelected: (HistoryRetention) -> Unit,
    onClearHistory: () -> Unit,
    onWidgetConfigChanged: (WidgetConfig) -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                activeTab = uiState.activeTab,
                onTabSelected = onTabSelected
            )
        },
        containerColor = VantablackBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(VantablackBg)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Global Header
            HeaderSection(
                onRefresh = onRefresh,
                isRefreshing = uiState.isRefreshing
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState.activeTab) {
                0 -> LiveMonitoringContent(
                    uiState = uiState,
                    onPollingIntervalSelected = onPollingIntervalSelected
                )
                1 -> HistoryContent(
                    historyList = uiState.historyList,
                    currentRetention = uiState.historyRetention,
                    onRetentionSelected = onHistoryRetentionSelected,
                    onClearHistory = onClearHistory
                )
                2 -> WidgetSettingsContent(
                    config = uiState.widgetConfig,
                    onConfigChanged = onWidgetConfigChanged
                )
            }
        }
    }
}

@Composable
fun HeaderSection(
    onRefresh: () -> Unit,
    isRefreshing: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Router,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ZTE MC7500",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Text(
                text = "ODU Signal & Tower Telemetry",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Refresh Button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(SurfaceDarkShell)
                .border(1.dp, HairlineBorder, CircleShape)
                .clickable { onRefresh() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = if (isRefreshing) AccentCyan else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = SurfaceDarkShell,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
    ) {
        NavigationBar(
            containerColor = SurfaceDarkShell,
            contentColor = TextPrimary
        ) {
            NavigationBarItem(
                selected = activeTab == 0,
                onClick = { onTabSelected(0) },
                icon = { Icon(Icons.Default.SignalCellularAlt, contentDescription = "Monitor") },
                label = { Text("Monitor", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentCyan,
                    selectedTextColor = AccentCyan,
                    indicatorColor = AccentCyan.copy(alpha = 0.15f),
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
            NavigationBarItem(
                selected = activeTab == 1,
                onClick = { onTabSelected(1) },
                icon = { Icon(Icons.Default.History, contentDescription = "Histori") },
                label = { Text("Histori", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentCyan,
                    selectedTextColor = AccentCyan,
                    indicatorColor = AccentCyan.copy(alpha = 0.15f),
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
            NavigationBarItem(
                selected = activeTab == 2,
                onClick = { onTabSelected(2) },
                icon = { Icon(Icons.Default.Widgets, contentDescription = "Widget") },
                label = { Text("Widget", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentCyan,
                    selectedTextColor = AccentCyan,
                    indicatorColor = AccentCyan.copy(alpha = 0.15f),
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}

@Composable
fun LiveMonitoringContent(
    uiState: DashboardUiState,
    onPollingIntervalSelected: (Long) -> Unit
) {
    val scrollState = rememberScrollState()
    val signal = uiState.signalState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Connection Status Banner
        ConnectionStatusBanner(signal = signal, errorMessage = signal.errorMessage)

        Spacer(modifier = Modifier.height(16.dp))

        // Refresh Interval Selector Pill
        IntervalSelectorRow(
            currentInterval = uiState.pollingIntervalMs,
            onIntervalSelected = onPollingIntervalSelected
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 5G NSA/SA Metric Gauge Card
        MetricSectionTitle(title = "KUALITAS SINYAL 5G", band = signal.nrBand)
        Spacer(modifier = Modifier.height(8.dp))
        DoubleBezelCard {
            Column(modifier = Modifier.padding(16.dp)) {
                GaugeBarItem(
                    label = "Kekuatan Sinyal (RSRP)",
                    valueStr = "${signal.nrRsrp} dBm",
                    progress = calculateRsrpProgress(signal.nrRsrp),
                    color = getRsrpColor(signal.nrRsrp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                GaugeBarItem(
                    label = "Kejernihan & Bebas Gangguan (SINR)",
                    valueStr = "${signal.nrSinr} dB",
                    progress = calculateSinrProgress(signal.nrSinr),
                    color = getSinrColor(signal.nrSinr)
                )
                Spacer(modifier = Modifier.height(14.dp))
                GaugeBarItem(
                    label = "Kualitas Penerimaan (RSRQ)",
                    valueStr = "${signal.nrRsrq} dB",
                    progress = calculateRsrqProgress(signal.nrRsrq),
                    color = getRsrqColor(signal.nrRsrq)
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = HairlineBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoPill(label = "ID Pemancar 5G (PCI)", value = "${signal.nrPci}")
                    InfoPill(label = "ID Sektor Sel", value = if (signal.nrCellId > 0) "${signal.nrCellId}" else "-")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4G LTE Metric Gauge Card
        MetricSectionTitle(title = "KUALITAS SINYAL 4G LTE", band = signal.lteBand)
        Spacer(modifier = Modifier.height(8.dp))
        DoubleBezelCard {
            Column(modifier = Modifier.padding(16.dp)) {
                GaugeBarItem(
                    label = "Kekuatan Sinyal (RSRP)",
                    valueStr = "${signal.lteRsrp} dBm",
                    progress = calculateRsrpProgress(signal.lteRsrp),
                    color = getRsrpColor(signal.lteRsrp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                GaugeBarItem(
                    label = "Kejernihan & Bebas Gangguan (SINR)",
                    valueStr = "${signal.lteSinr} dB",
                    progress = calculateSinrProgress(signal.lteSinr),
                    color = getSinrColor(signal.lteSinr)
                )
                Spacer(modifier = Modifier.height(14.dp))
                GaugeBarItem(
                    label = "Kualitas Penerimaan (RSRQ)",
                    valueStr = "${signal.lteRsrq} dB",
                    progress = calculateRsrqProgress(signal.lteRsrq),
                    color = getRsrqColor(signal.lteRsrq)
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = HairlineBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoPill(label = "ID Pemancar 4G (PCI)", value = "${signal.ltePci}")
                    InfoPill(label = "ID Sektor Sel", value = if (signal.lteCellId > 0) "${signal.lteCellId}" else "-")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Network Details & Carrier Aggregation
        Text(
            text = "PENGGABUNGAN FREKUENSI & PEMANCAR TERDEKAT",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        DoubleBezelCard {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(
                    label = "Multi-Frekuensi (Carrier Aggregation)",
                    value = if (signal.isCaActive) "AKTIF (Koneksi Ganda)" else "Tidak Aktif",
                    valueColor = if (signal.isCaActive) SignalExcellent else TextSecondary
                )
                if (signal.isCaActive && signal.caDetails != "-") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = signal.caDetails,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = HairlineBorder)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(
                    label = "Pemancar Sekitar (Neighbors)",
                    value = signal.neighborCells,
                    valueColor = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun HistoryContent(
    historyList: List<OduSignalState>,
    currentRetention: HistoryRetention,
    onRetentionSelected: (HistoryRetention) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "ATURAN HAPUS HISTORI OTOMATIS",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Retention selection pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HistoryRetention.values().forEach { retention ->
                val isSelected = currentRetention == retention
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentCyan.copy(alpha = 0.2f) else SurfaceDarkShell)
                        .border(1.dp, if (isSelected) AccentCyan else HairlineBorder, RoundedCornerShape(12.dp))
                        .clickable { onRetentionSelected(retention) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = retention.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) AccentCyan else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Stats & Clear button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOG HISTORI (${historyList.size} Rekaman)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.5.sp
            )

            Button(
                onClick = onClearHistory,
                colors = ButtonDefaults.buttonColors(containerColor = SignalPoor.copy(alpha = 0.15f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = SignalPoor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Hapus Sekarang",
                    color = SignalPoor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDarkShell),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada rekaman histori sinyal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList) { item ->
                    HistoryItemCard(signal = item)
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(signal: OduSignalState) {
    DoubleBezelCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(Date(signal.lastUpdated)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = signal.connectionType,
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "5G: ${signal.nrRsrp} dBm (${signal.nrSinr} dB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = getRsrpColor(signal.nrRsrp)
                    )
                    Text(
                        text = "4G: ${signal.lteRsrp} dBm (${signal.lteSinr} dB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = getRsrpColor(signal.lteRsrp)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "PCI: 5G ${signal.nrPci} | 4G ${signal.ltePci}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun WidgetSettingsContent(
    config: WidgetConfig,
    onConfigChanged: (WidgetConfig) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "KUSTOMISASI INFORMASI WIDGET",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        DoubleBezelCard {
            Column(modifier = Modifier.padding(16.dp)) {
                WidgetToggleRow(
                    label = "Tampilkan Sinyal 5G",
                    checked = config.show5g,
                    onCheckedChange = { onConfigChanged(config.copy(show5g = it)) }
                )
                HorizontalDivider(color = HairlineBorder, modifier = Modifier.padding(vertical = 8.dp))
                
                WidgetToggleRow(
                    label = "Tampilkan Sinyal 4G LTE",
                    checked = config.show4g,
                    onCheckedChange = { onConfigChanged(config.copy(show4g = it)) }
                )
                HorizontalDivider(color = HairlineBorder, modifier = Modifier.padding(vertical = 8.dp))

                WidgetToggleRow(
                    label = "Status Provider & Koneksi",
                    checked = config.showProviderStatus,
                    onCheckedChange = { onConfigChanged(config.copy(showProviderStatus = it)) }
                )
                HorizontalDivider(color = HairlineBorder, modifier = Modifier.padding(vertical = 8.dp))

                WidgetToggleRow(
                    label = "ID Pemancar (PCI & Cell ID)",
                    checked = config.showPciTower,
                    onCheckedChange = { onConfigChanged(config.copy(showPciTower = it)) }
                )
                HorizontalDivider(color = HairlineBorder, modifier = Modifier.padding(vertical = 8.dp))

                WidgetToggleRow(
                    label = "Informasi Band Frekuensi",
                    checked = config.showBandInfo,
                    onCheckedChange = { onConfigChanged(config.copy(showBandInfo = it)) }
                )
                HorizontalDivider(color = HairlineBorder, modifier = Modifier.padding(vertical = 8.dp))

                WidgetToggleRow(
                    label = "Kejernihan Sinyal (SINR & RSRQ)",
                    checked = config.showSinrRsrq,
                    onCheckedChange = { onConfigChanged(config.copy(showSinrRsrq = it)) }
                )
                HorizontalDivider(color = HairlineBorder, modifier = Modifier.padding(vertical = 8.dp))

                WidgetToggleRow(
                    label = "Waktu Update Terakhir",
                    checked = config.showTimestamp,
                    onCheckedChange = { onConfigChanged(config.copy(showTimestamp = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "INTERVAL UPDATE BACKGROUND WIDGET",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        DoubleBezelCard {
            Column(modifier = Modifier.padding(16.dp)) {
                val intervals = listOf(
                    15 to "15 Menit",
                    30 to "30 Menit",
                    60 to "1 Jam",
                    180 to "3 Jam",
                    360 to "6 Jam",
                    -1 to "Manual"
                )

                intervals.forEachIndexed { index, (min, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfigChanged(config.copy(updateIntervalMinutes = min)) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        RadioButton(
                            selected = config.updateIntervalMinutes == min,
                            onClick = { onConfigChanged(config.copy(updateIntervalMinutes = min)) },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentCyan)
                        )
                    }
                    if (index < intervals.size - 1) {
                        HorizontalDivider(color = HairlineBorder)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun WidgetToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VantablackBg,
                checkedTrackColor = AccentCyan,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceDarkShell
            )
        )
    }
}

@Composable
fun ConnectionStatusBanner(signal: OduSignalState, errorMessage: String?) {
    val isOk = signal.isConnected && errorMessage == null
    val bgColor = if (isOk) SignalExcellent.copy(alpha = 0.1f) else SignalPoor.copy(alpha = 0.1f)
    val borderColor = if (isOk) SignalExcellent.copy(alpha = 0.4f) else SignalPoor.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isOk) SignalExcellent.copy(alpha = 0.2f) else SignalPoor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isOk) Icons.Default.SignalCellularAlt else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isOk) SignalExcellent else SignalPoor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOk) signal.connectionType else "ODU Disconnected",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isOk) signal.provider else (errorMessage ?: "Check local connection to 192.168.254.1"),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOk) TextSecondary else SignalPoor
                )
            }

            if (isOk) {
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(signal.lastUpdated)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun IntervalSelectorRow(currentInterval: Long, onIntervalSelected: (Long) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "POLLING INTERVAL",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.5.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(1000L to "1s", 3000L to "3s", 5000L to "5s").forEach { (ms, label) ->
                val isSelected = currentInterval == ms
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentCyan.copy(alpha = 0.2f) else SurfaceDarkShell)
                        .border(1.dp, if (isSelected) AccentCyan else HairlineBorder, RoundedCornerShape(12.dp))
                        .clickable { onIntervalSelected(ms) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) AccentCyan else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun MetricSectionTitle(title: String, band: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.5.sp
        )
        Text(
            text = band,
            style = MaterialTheme.typography.labelSmall,
            color = AccentCyan,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DoubleBezelCard(content: @Composable () -> Unit) {
    // Outer Shell
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDarkShell)
            .border(1.dp, HairlineBorder, RoundedCornerShape(24.dp))
            .padding(4.dp)
    ) {
        // Inner Core
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDarkCore)
        ) {
            content()
        }
    }
}

@Composable
fun GaugeBarItem(label: String, valueStr: String, progress: Float, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 500),
        label = "color"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = valueStr,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = animatedColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(SurfaceDarkShell)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(CircleShape)
                    .background(animatedColor)
            )
        }
    }
}

@Composable
fun InfoPill(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

private fun calculateRsrpProgress(rsrp: Int): Float = ((rsrp + 140) / 80f)

private fun calculateSinrProgress(sinr: Float): Float = ((sinr + 10) / 40f)

private fun calculateRsrqProgress(rsrq: Int): Float = ((rsrq + 20) / 17f)
