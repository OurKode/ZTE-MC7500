package com.example.odumonitor.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SignalCellularAlt
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
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onPollingIntervalSelected: (Long) -> Unit,
    onToggleDemoMode: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    val signal = uiState.signalState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VantablackBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header section
        HeaderSection(
            uiState = uiState,
            onRefresh = onRefresh,
            onToggleDemo = onToggleDemoMode
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Connection Status Banner
        ConnectionStatusBanner(signal = signal, errorMessage = signal.errorMessage)

        Spacer(modifier = Modifier.height(20.dp))

        // Refresh Interval Selector Pill
        IntervalSelectorRow(
            currentInterval = uiState.pollingIntervalMs,
            onIntervalSelected = onPollingIntervalSelected
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 5G NSA/SA Metric Gauge Card (Double Bezel Architecture)
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
                Divider(color = HairlineBorder)
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

        Spacer(modifier = Modifier.height(24.dp))

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
                Divider(color = HairlineBorder)
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

        Spacer(modifier = Modifier.height(24.dp))

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
                Divider(color = HairlineBorder)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(
                    label = "Pemancar Sekitar (Neighbors)",
                    value = signal.neighborCells,
                    valueColor = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun HeaderSection(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onToggleDemo: (Boolean) -> Unit
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

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Demo mode toggle pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (uiState.isDemoMode) AccentPurple.copy(alpha = 0.2f) else SurfaceDarkShell)
                    .border(1.dp, if (uiState.isDemoMode) AccentPurple else HairlineBorder, RoundedCornerShape(20.dp))
                    .clickable { onToggleDemo(!uiState.isDemoMode) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isDemoMode) AccentPurple else TextMuted)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.isDemoMode) "DEMO" else "LIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.isDemoMode) AccentPurple else TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Refresh Button-in-Button
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
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
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

// Helpers for Signal threshold colors and progress calculations
private fun getRsrpColor(rsrp: Int): Color = when {
    rsrp >= -80 -> SignalExcellent
    rsrp >= -100 -> SignalFair
    else -> SignalPoor
}

private fun getSinrColor(sinr: Float): Color = when {
    sinr >= 20f -> SignalExcellent
    sinr >= 10f -> SignalFair
    else -> SignalPoor
}

private fun getRsrqColor(rsrq: Int): Color = when {
    rsrq >= -10 -> SignalExcellent
    rsrq >= -15 -> SignalFair
    else -> SignalPoor
}

private fun calculateRsrpProgress(rsrp: Int): Float {
    // Map -140 dBm (0%) to -60 dBm (100%)
    return ((rsrp + 140) / 80f)
}

private fun calculateSinrProgress(sinr: Float): Float {
    // Map -10 dB (0%) to 30 dB (100%)
    return ((sinr + 10) / 40f)
}

private fun calculateRsrqProgress(rsrq: Int): Float {
    // Map -20 dB (0%) to -3 dB (100%)
    return ((rsrq + 20) / 17f)
}
