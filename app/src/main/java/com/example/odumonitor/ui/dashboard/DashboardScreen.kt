package com.example.odumonitor.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
        bitsPerSec >= 1_000.0 -> String.format(Locale.US, "%.1f Kbps", bitsPerSec / 1_000.0)
        else -> String.format(Locale.US, "%.0f bps", bitsPerSec)
    }
}

enum class ChartMetric(val label: String, val unit: String) {
    RSRQ("RSRQ", "dB"),
    RSRP("RSRP", "dBm"),
    SINR("SINR", "dB")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onPollingIntervalSelected: (Long) -> Unit,
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
    var showWidgetSheet by remember { mutableStateOf(false) }
    var selected5gMetric by remember { mutableStateOf(ChartMetric.RSRQ) }
    var selected4gMetric by remember { mutableStateOf(ChartMetric.RSRQ) }

    Scaffold(
        containerColor = BgBase
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgBase)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Precision Telecom Header
            HeaderSection(
                isRefreshing = uiState.isRefreshing,
                pollingInterval = uiState.pollingIntervalMs,
                onRefresh = onRefresh,
                onPollingIntervalSelected = onPollingIntervalSelected,
                onOpenWidgetSettings = { showWidgetSheet = true }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Telemetry Cards
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Connection & Tower Status Banner
                ConnectionStatusBanner(
                    signal = uiState.signalState,
                    errorMessage = uiState.signalState.errorMessage
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Router System & Live Bandwidth Telemetry Card
                RouterTrafficCard(
                    signal = uiState.signalState,
                    history = uiState.signalHistory,
                    onOpenSettings = { showWidgetSheet = true }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 5G NR Telemetry & Real-Time Trend Card
                SignalTechCard(
                    title = "5G NR",
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
                    selectedMetric = selected5gMetric,
                    onMetricSelected = { selected5gMetric = it },
                    is5g = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4G LTE Telemetry & Real-Time Trend Card
                SignalTechCard(
                    title = "4G LTE",
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
                    selectedMetric = selected4gMetric,
                    onMetricSelected = { selected4gMetric = it },
                    is5g = false
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Carrier Aggregation & Tower Matrix Card
                CarrierAggregationCard(signal = uiState.signalState)

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Bottom Sheet for Widget, Router & Notification Configuration
    if (showWidgetSheet) {
        SettingsBottomSheet(
            config = uiState.widgetConfig,
            routerCreds = uiState.routerCredentials,
            notifConfig = uiState.notificationConfig,
            signalState = uiState.signalState,
            isTestingLogin = uiState.isTestingLogin,
            loginStatusMessage = uiState.loginStatusMessage,
            isExecutingRadioCommand = uiState.isExecutingRadioCommand,
            radioCommandMessage = uiState.radioCommandMessage,
            onConfigChanged = onWidgetConfigChanged,
            onRouterCredentialsChanged = onRouterCredentialsChanged,
            onTestLogin = onTestRouterLogin,
            onNotificationConfigChanged = onNotificationConfigChanged,
            onSetNetworkSelect = onSetNetworkSelect,
            onSet4gBandLock = onSet4gBandLock,
            onSet5gBandLock = onSet5gBandLock,
            onLock4gCell = onLock4gCell,
            onUnlock4gCell = onUnlock4gCell,
            onLock5gCell = onLock5gCell,
            onUnlock5gCell = onUnlock5gCell,
            onClearRadioMessage = onClearRadioMessage,
            onDismiss = { showWidgetSheet = false }
        )
    }
}

@Composable
fun HeaderSection(
    isRefreshing: Boolean,
    pollingInterval: Long,
    onRefresh: () -> Unit,
    onPollingIntervalSelected: (Long) -> Unit,
    onOpenWidgetSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Utility Tag
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Router,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
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
                text = "Telemetri Sinyal & Monitor Antena",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Actions: Interval Selector & Quick Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Polling interval selector chips
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(1000L to "1s", 3000L to "3s", 5000L to "5s").forEach { (ms, label) ->
                    val isSelected = pollingInterval == ms
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) AccentPrimary.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { onPollingIntervalSelected(ms) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AccentPrimary else TextSecondary
                        )
                    }
                }
            }

            // Refresh Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .clickable { onRefresh() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Muat Ulang",
                    tint = if (isRefreshing) AccentPrimary else TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Widget Settings Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .clickable { onOpenWidgetSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = "Pengaturan Widget",
                    tint = TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
fun ConnectionStatusBanner(
    signal: OduSignalState,
    errorMessage: String?
) {
    val isOk = signal.isConnected && errorMessage == null
    val statusColor = if (isOk) SignalExcellent else SignalPoor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Solid Indicator Dot (no decorative pulse)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isOk) signal.provider else "Terputus dari ODU",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "192.168.254.1",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isOk) "${signal.connectionType} • ${signal.signalBar}/5 Bar Sinyal" else (errorMessage ?: "Periksa koneksi Wi-Fi/LAN ke ODU"),
                    fontSize = 12.sp,
                    color = if (isOk) TextSecondary else SignalPoor
                )
            }

            if (isOk) {
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(signal.lastUpdated)),
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun SignalTechCard(
    title: String,
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
    selectedMetric: ChartMetric,
    onMetricSelected: (ChartMetric) -> Unit,
    is5g: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            // Card Header: Tech & Band Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceCardSubtle)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    val cleanBand = when {
                        band.startsWith("LTE BAND ", ignoreCase = true) -> "B" + band.substringAfter("LTE BAND ", "").trim()
                        band.startsWith("BAND ", ignoreCase = true) -> "B" + band.substringAfter("BAND ", "").trim()
                        else -> band
                    }
                    val freqPart = dlFreqMhz?.let { " • ${it} MHz" } ?: ""
                    val displayBand = if (cleanBand.isNotBlank() && cleanBand != "-") {
                        val prefix = if (cleanBand.startsWith("B", true) || cleanBand.startsWith("n", true)) "" else "Band "
                        "$prefix$cleanBand$freqPart"
                    } else "Siaga"
                    Text(
                        text = displayBand,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // High-Glanceability Primary Numeric Metrics (Value-First)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumnItem(
                    label = "Kekuatan (RSRP)",
                    valueText = "$rsrp dBm",
                    valueColor = getRsrpColor(rsrp),
                    modifier = Modifier.weight(1f)
                )
                MetricColumnItem(
                    label = "Kejernihan (SINR)",
                    valueText = "$sinr dB",
                    valueColor = getSinrColor(sinr),
                    modifier = Modifier.weight(1f)
                )
                MetricColumnItem(
                    label = "Kualitas (RSRQ)",
                    valueText = "$rsrq dB",
                    valueColor = getRsrqColor(rsrq),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Chart Header: Metric Switcher Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tren Kestabilan Real-Time",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )

                // Metric Selector Pills
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChartMetric.values().forEach { metric ->
                        val isSelected = selectedMetric == metric
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AccentPrimary.copy(alpha = 0.16f) else SurfaceCardSubtle)
                                .border(
                                    1.dp,
                                    if (isSelected) AccentPrimary.copy(alpha = 0.8f) else BorderSubtle,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onMetricSelected(metric) }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = metric.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AccentPrimary else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time Line Graph Canvas
            SignalLineChart(
                history = history,
                metric = selectedMetric,
                is5g = is5g
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Cell Telemetry (Channel, Bandwidth, PCI, Cell ID)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val chanPrefix = if (is5g) "ARFCN" else "EARFCN"
                val chanText = earfcnOrArfcn?.let { "$chanPrefix $it" } ?: "$chanPrefix -"
                val bwText = bandwidth?.let { " • $it" } ?: ""
                Text(
                    text = "$chanText$bwText",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "PCI: $pci  •  Sel: ${if (cellId > 0) cellId else "-"}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun MetricColumnItem(
    label: String,
    valueText: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = valueText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun SignalLineChart(
    history: List<OduSignalState>,
    metric: ChartMetric,
    is5g: Boolean
) {
    val dataPoints = remember(history, metric, is5g) {
        history.map { state ->
            if (is5g) {
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
        }
    }

    if (dataPoints.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCardSubtle.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Mengumpulkan telemetri...",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
        return
    }

    val minVal = dataPoints.minOrNull() ?: 0f
    val maxVal = dataPoints.maxOrNull() ?: 0f
    val currentVal = dataPoints.last()

    val metricColor = when (metric) {
        ChartMetric.RSRQ -> getRsrqColor(currentVal.toInt())
        ChartMetric.RSRP -> getRsrpColor(currentVal.toInt())
        ChartMetric.SINR -> getSinrColor(currentVal)
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCardSubtle.copy(alpha = 0.35f))
                .border(1.dp, BorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val range = (maxVal - minVal).coerceAtLeast(1.5f)
                val yMin = minVal - (range * 0.15f)
                val yMax = maxVal + (range * 0.15f)
                val totalYSpan = (yMax - yMin).coerceAtLeast(1f)

                // 3 Subtle horizontal grid lines
                val gridAlpha = 0.18f
                drawLine(
                    color = BorderSubtle.copy(alpha = gridAlpha),
                    start = Offset(0f, 0f),
                    end = Offset(width, 0f),
                    strokeWidth = 1f
                )
                drawLine(
                    color = BorderSubtle.copy(alpha = gridAlpha),
                    start = Offset(0f, height / 2),
                    end = Offset(width, height / 2),
                    strokeWidth = 1f
                )
                drawLine(
                    color = BorderSubtle.copy(alpha = gridAlpha),
                    start = Offset(0f, height),
                    end = Offset(width, height),
                    strokeWidth = 1f
                )

                val points = dataPoints.mapIndexed { index, value ->
                    val x = if (dataPoints.size > 1) {
                        (index.toFloat() / (dataPoints.size - 1)) * width
                    } else {
                        width
                    }
                    val normalizedY = ((value - yMin) / totalYSpan).coerceIn(0f, 1f)
                    val y = height - (normalizedY * height)
                    Offset(x, y)
                }

                if (points.size == 1) {
                    val p = points.first()
                    drawCircle(color = metricColor, radius = 4.dp.toPx(), center = p)
                    return@Canvas
                }

                // Construct path for the line
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }

                // Area fill path underneath line
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(points.last().x, height)
                    lineTo(points.first().x, height)
                    close()
                }

                // Draw gradient area
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            metricColor.copy(alpha = 0.22f),
                            metricColor.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw stroke
                drawPath(
                    path = strokePath,
                    color = metricColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Highlight latest point with a precise dot
                val latestPoint = points.last()
                drawCircle(
                    color = metricColor,
                    radius = 4.dp.toPx(),
                    center = latestPoint
                )
                drawCircle(
                    color = BgBase,
                    radius = 2.dp.toPx(),
                    center = latestPoint
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Axis Bounds & Sample Counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min: ${minVal.toInt()} ${metric.unit}",
                fontSize = 10.sp,
                color = TextMuted
            )
            Text(
                text = "${dataPoints.size} sampel terbaru",
                fontSize = 10.sp,
                color = TextMuted
            )
            Text(
                text = "Max: ${maxVal.toInt()} ${metric.unit}",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun CarrierAggregationCard(signal: OduSignalState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Penggabungan Frekuensi (CA)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (signal.isCaActive) SignalExcellent.copy(alpha = 0.15f) else SurfaceCardSubtle)
                        .border(
                            1.dp,
                            if (signal.isCaActive) SignalExcellent.copy(alpha = 0.5f) else BorderSubtle,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (signal.isCaActive) "AKTIF (Koneksi Ganda)" else "Tidak Aktif",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (signal.isCaActive) SignalExcellent else TextMuted
                    )
                }
            }

            if (signal.isCaActive && signal.caDetails != "-") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = signal.caDetails,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Pemancar Sekitar: ${signal.neighborCells}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun RouterTrafficCard(
    signal: OduSignalState,
    history: List<OduSignalState>,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Device & Auth Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = "Router",
                        tint = AccentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ZTE MC7500 GATEWAY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (signal.isRouterLoggedIn) SignalExcellent.copy(alpha = 0.12f) else SurfaceCardSubtle)
                        .border(
                            1.dp,
                            if (signal.isRouterLoggedIn) SignalExcellent.copy(alpha = 0.3f) else BorderSubtle,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (signal.isRouterLoggedIn) SignalExcellent else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (signal.isRouterLoggedIn) "Admin Aktif" else "Tamu (RF)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (signal.isRouterLoggedIn) SignalExcellent else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Speed Rates Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCardSubtle)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UNDUH REAL-TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "↓ ${formatSpeed(signal.downloadSpeedBps)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(BorderSubtle)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "UNGGAH REAL-TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "↑ ${formatSpeed(signal.uploadSpeedBps)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SignalExcellent
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dual-Line Real-Time Throughput Trend Chart
            ThroughputTrendChart(history = history)

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-metrics Grid (Identitas SIM/IP, Statistik Kuota, Kondisi Gateway)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RouterMetricItem(
                    label = "WAN IP",
                    value = signal.wanIp ?: "-",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                RouterMetricItem(
                    label = "NO. TELEPON SIM",
                    value = signal.simPhoneNumber ?: "-",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RouterMetricItem(
                    label = "TRAFIK BULAN INI",
                    value = if (signal.monthRxBytes > 0) formatBytes(signal.monthRxBytes) else "-",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                RouterMetricItem(
                    label = "TRAFIK HARI INI",
                    value = if (signal.dayRxBytes > 0) formatBytes(signal.dayRxBytes) else "-",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RouterMetricItem(
                    label = "SUHU CPU",
                    value = signal.cpuTemp ?: "Normal",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                RouterMetricItem(
                    label = "UPTIME GATEWAY",
                    value = signal.deviceUptimeSeconds?.let { com.example.odumonitor.util.FrequencyConverter.formatUptime(it) } ?: "-",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ThroughputTrendChart(
    history: List<OduSignalState>,
    modifier: Modifier = Modifier
) {
    val dlPoints = history.map { it.downloadSpeedBps.toFloat() }
    val ulPoints = history.map { it.uploadSpeedBps.toFloat() }

    val actualMax = maxOf(
        dlPoints.maxOrNull() ?: 0f,
        ulPoints.maxOrNull() ?: 0f
    )
    val maxVal = actualMax.coerceAtLeast(1024f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tren Throughput Real-Time",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentPrimary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Unduh",
                    fontSize = 10.sp,
                    color = AccentPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SignalExcellent)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Unggah",
                    fontSize = 10.sp,
                    color = SignalExcellent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCardSubtle)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (history.size < 2) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mengumpulkan telemetri throughput...",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val pointCount = dlPoints.size
                    val stepX = if (pointCount > 1) w / (pointCount - 1) else w

                    // Guide lines
                    drawLine(
                        color = BorderSubtle.copy(alpha = 0.35f),
                        start = Offset(0f, h * 0.25f),
                        end = Offset(w, h * 0.25f),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = BorderSubtle.copy(alpha = 0.35f),
                        start = Offset(0f, h * 0.75f),
                        end = Offset(w, h * 0.75f),
                        strokeWidth = 1.dp.toPx()
                    )

                    fun toOffsets(pts: List<Float>): List<Offset> {
                        return pts.mapIndexed { idx, v ->
                            val norm = (v / maxVal).coerceIn(0f, 1f)
                            val x = idx * stepX
                            val y = h - (norm * (h - 8.dp.toPx())) - 4.dp.toPx()
                            Offset(x, y)
                        }
                    }

                    fun drawCurve(offsets: List<Offset>, color: Color) {
                        if (offsets.size < 2) return
                        val linePath = Path()
                        val fillPath = Path()

                        linePath.moveTo(offsets[0].x, offsets[0].y)
                        fillPath.moveTo(offsets[0].x, h)
                        fillPath.lineTo(offsets[0].x, offsets[0].y)

                        for (i in 0 until offsets.size - 1) {
                            val cur = offsets[i]
                            val nxt = offsets[i + 1]
                            val cpx = (cur.x + nxt.x) / 2f
                            linePath.cubicTo(cpx, cur.y, cpx, nxt.y, nxt.x, nxt.y)
                            fillPath.cubicTo(cpx, cur.y, cpx, nxt.y, nxt.x, nxt.y)
                        }

                        fillPath.lineTo(offsets.last().x, h)
                        fillPath.close()

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.01f))
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = color,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        drawCircle(
                            color = color,
                            radius = 3.dp.toPx(),
                            center = offsets.last()
                        )
                    }

                    drawCurve(toOffsets(dlPoints), AccentPrimary)
                    drawCurve(toOffsets(ulPoints), SignalExcellent)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min: 0 Mbps",
                fontSize = 10.sp,
                color = TextMuted
            )
            Text(
                text = "${history.size} sampel terbaru",
                fontSize = 10.sp,
                color = TextMuted
            )
            Text(
                text = "Puncak: ${formatSpeed(actualMax.toLong())}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCardSubtle)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    config: WidgetConfig,
    routerCreds: RouterCredentials,
    notifConfig: NotificationConfig,
    signalState: OduSignalState,
    isTestingLogin: Boolean,
    loginStatusMessage: String?,
    isExecutingRadioCommand: Boolean,
    radioCommandMessage: String?,
    onConfigChanged: (WidgetConfig) -> Unit,
    onRouterCredentialsChanged: (RouterCredentials) -> Unit,
    onTestLogin: (RouterCredentials) -> Unit,
    onNotificationConfigChanged: (NotificationConfig) -> Unit,
    onSetNetworkSelect: (String) -> Unit = {},
    onSet4gBandLock: (String, String) -> Unit = { _, _ -> },
    onSet5gBandLock: (String, String) -> Unit = { _, _ -> },
    onLock4gCell: (Int, Int) -> Unit = { _, _ -> },
    onUnlock4gCell: () -> Unit = {},
    onLock5gCell: (Int, Int, Int) -> Unit = { _, _, _ -> },
    onUnlock5gCell: () -> Unit = {},
    onClearRadioMessage: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Router tab
    var hostInput by remember(routerCreds) { mutableStateOf(routerCreds.host) }
    var usernameInput by remember(routerCreds) { mutableStateOf(routerCreds.username) }
    var passwordInput by remember(routerCreds) { mutableStateOf(routerCreds.password) }
    var isLoginEnabled by remember(routerCreds) { mutableStateOf(routerCreds.isLoginEnabled) }
    var showPassword by remember { mutableStateOf(false) }
    var pendingConfirmation by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    if (pendingConfirmation != null) {
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            containerColor = SurfaceCard,
            title = {
                Text(
                    text = "Konfirmasi Pengaturan Radio",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = pendingConfirmation!!.first,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val action = pendingConfirmation!!.second
                        pendingConfirmation = null
                        action()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("Terapkan", color = BgBase, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmation = null }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        contentColor = TextPrimary,
        tonalElevation = 8.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = TextMuted
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Segment Tab Selector (4 Clean Anti-slop Pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCardSubtle)
                    .padding(4.dp)
            ) {
                val tabTitles = listOf("Widget", "Router", "Notif", "Radio & Band")
                tabTitles.forEachIndexed { idx, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == idx) SurfaceCard else Color.Transparent)
                            .clickable { selectedTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == idx) AccentPrimary else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when (selectedTab) {
                0 -> {
                    // Widget Settings Section
                    Text(
                        text = "Pengaturan Homescreen Widget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Pilih metrik yang ingin ditampilkan pada widget 2x2 dan 4x2",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

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

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Interval Sinkronisasi Latar Belakang",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
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
                                .clickable { onConfigChanged(config.copy(updateIntervalMinutes = min)) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            RadioButton(
                                selected = config.updateIntervalMinutes == min,
                                onClick = { onConfigChanged(config.copy(updateIntervalMinutes = min)) },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentPrimary)
                            )
                        }
                    }
                }
                1 -> {
                    // Router Authentication Section
                    Text(
                        text = "Kredensial & Autentikasi Router",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Login memungkinkan pengambilan kuota, kecepatan real-time, dan status WAN",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aktifkan Login Otomatis",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
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
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BgBase,
                                checkedTrackColor = AccentPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceCardSubtle
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        label = { Text("IP Host / URL Router", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password Web GUI", color = TextSecondary) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Password",
                                    tint = TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentPrimary
                        ),
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
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = BgBase
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTestingLogin) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = BgBase,
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceCardSubtle)
                                .border(
                                    1.dp,
                                    if (loginStatusMessage.startsWith("Berhasil")) SignalExcellent.copy(alpha = 0.5f) else SignalPoor.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = loginStatusMessage,
                                fontSize = 12.sp,
                                color = if (loginStatusMessage.startsWith("Berhasil")) SignalExcellent else SignalPoor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                2 -> {
                    // Notification & Alert Settings Section
                    Text(
                        text = "Layanan & Peringatan Latar Belakang",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Deteksi perubahan jaringan real-time dan kirim notifikasi saat aplikasi ditutup",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    WidgetToggleItem(
                        label = "Layanan Pemantauan Aktif (Foreground Service)",
                        checked = notifConfig.isServiceEnabled,
                        onCheckedChange = { onNotificationConfigChanged(notifConfig.copy(isServiceEnabled = it)) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    WidgetToggleItem(
                        label = "Peringatan Perubahan Band (4G / 5G)",
                        checked = notifConfig.notifyBandChange,
                        onCheckedChange = { onNotificationConfigChanged(notifConfig.copy(notifyBandChange = it)) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    WidgetToggleItem(
                        label = "Peringatan Perubahan Alamat WAN IP",
                        checked = notifConfig.notifyWanIpChange,
                        onCheckedChange = { onNotificationConfigChanged(notifConfig.copy(notifyWanIpChange = it)) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    WidgetToggleItem(
                        label = "Peringatan ODU Terputus / Offline",
                        checked = notifConfig.notifyOduOffline,
                        onCheckedChange = { onNotificationConfigChanged(notifConfig.copy(notifyOduOffline = it)) }
                    )
                }
                3 -> {
                    // Radio & Band Management Section
                    Text(
                        text = "Alat Kendali Radio & Band",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Kelola mode bearer, kunci band frekuensi, dan kunci pemancar sel ODU",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Feedback Banner if command executed
                    if (!radioCommandMessage.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isExecutingRadioCommand) AccentPrimary.copy(alpha = 0.12f) else SurfaceCardSubtle)
                                .border(1.dp, if (isExecutingRadioCommand) AccentPrimary.copy(alpha = 0.4f) else BorderSubtle, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isExecutingRadioCommand) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = AccentPrimary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Text(
                                        text = radioCommandMessage,
                                        fontSize = 12.sp,
                                        color = if (isExecutingRadioCommand) AccentPrimary else TextPrimary
                                    )
                                }
                                if (!isExecutingRadioCommand) {
                                    IconButton(
                                        onClick = onClearRadioMessage,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Tutup",
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (!signalState.isRouterLoggedIn) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E2616))
                                .border(1.dp, Color(0xFF8C6D1F), RoundedCornerShape(8.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Perlu Akses Admin Router",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Fitur kendali radio membutuhkan sesi login admin router aktif. Silakan masukkan kredensial di tab 'Router' dan klik 'Simpan & Uji Login'.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        // Section 1: Network Mode (Bearer)
                        Text(
                            text = "1. MODE JARINGAN (BEARER)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mode aktif: ${com.example.odumonitor.util.FrequencyConverter.formatBearerModeName(signalState.netSelect)}",
                            fontSize = 12.sp,
                            color = AccentPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val bearerModes = listOf(
                            Triple("Auto (5G/4G/3G)", "WL_AND_5G", "Beralih ke mode otomatis (5G + 4G + 3G)?"),
                            Triple("5G NSA + 4G LTE", "LTE_AND_5G", "Beralih ke mode 5G NSA + 4G LTE?"),
                            Triple("5G SA Saja", "Only_5G", "Beralih ke 5G SA Saja? Pastikan kartu SIM & BTS mendukung 5G Standalone."),
                            Triple("4G LTE Saja", "Only_LTE", "Beralih ke 4G LTE Saja? Modem tidak akan menggunakan sinyal 5G.")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            bearerModes.chunked(2).forEach { rowModes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowModes.forEach { (label, modeCode, promptText) ->
                                        val isCurrent = signalState.netSelect == modeCode
                                        OutlinedButton(
                                            onClick = {
                                                pendingConfirmation = Pair(promptText) {
                                                    onSetNetworkSelect(modeCode)
                                                }
                                            },
                                            enabled = !isExecutingRadioCommand,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isCurrent) AccentPrimary.copy(alpha = 0.15f) else SurfaceCardSubtle,
                                                contentColor = if (isCurrent) AccentPrimary else TextPrimary
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isCurrent) AccentPrimary else BorderSubtle
                                            )
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Section 2: 4G Band Locking
                        Text(
                            text = "2. KUNCI BAND 4G LTE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp
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
                            fontSize = 12.sp,
                            color = AccentPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Profil Operator Indonesia:",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val operatorProfiles = listOf(
                            Triple("Telkomsel (B1,3,8,40)", listOf(1, 3, 8, 40), "Telkomsel (B1, B3, B8, B40)"),
                            Triple("XL Axiata (B1,3,8)", listOf(1, 3, 8), "XL Axiata (B1, B3, B8)"),
                            Triple("Indosat (B1,3,8)", listOf(1, 3, 8), "Indosat (B1, B3, B8)"),
                            Triple("Smartfren (B28,40)", listOf(28, 40), "Smartfren (B28, B40)")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = SurfaceCardSubtle,
                                                contentColor = TextPrimary
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                        ) {
                                            Text(text = btnText, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pilihan Band Tunggal 4G:",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val single4gBands = listOf(
                            Pair("B1", 1),
                            Pair("B3", 3),
                            Pair("B8", 8),
                            Pair("B40", 40),
                            Pair("B28", 28)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            single4gBands.forEach { (lbl, bandNum) ->
                                val mask = (1L shl (bandNum - 1)).toString()
                                OutlinedButton(
                                    onClick = {
                                        pendingConfirmation = Pair("Kunci modem hanya ke 4G Band $bandNum?") {
                                            onSet4gBandLock(mask, "Band $bandNum")
                                        }
                                    },
                                    enabled = !isExecutingRadioCommand,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = SurfaceCardSubtle,
                                        contentColor = TextPrimary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                ) {
                                    Text(text = lbl, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Reset 4G Bands button
                        OutlinedButton(
                            onClick = {
                                pendingConfirmation = Pair("Buka kunci dan aktifkan semua band 4G (Auto)?") {
                                    onSet4gBandLock(com.example.odumonitor.util.FrequencyConverter.FULL_4G_MASK.toString(), "Auto / Semua")
                                }
                            },
                            enabled = !isExecutingRadioCommand,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceCardSubtle,
                                contentColor = TextSecondary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Buka Semua Band 4G (Auto)", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Section 3: 5G Band Locking
                        Text(
                            text = "3. KUNCI BAND 5G NR (SA)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pilih band 5G Standalone (n40 Telkomsel/Smartfren, n1, n3, n78):",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val single5gBands = listOf("40", "1", "3", "78")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            single5gBands.forEach { b ->
                                OutlinedButton(
                                    onClick = {
                                        pendingConfirmation = Pair("Kunci frekuensi 5G SA ke band n$b?") {
                                            onSet5gBandLock(b, "n$b")
                                        }
                                    },
                                    enabled = !isExecutingRadioCommand,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = SurfaceCardSubtle,
                                        contentColor = TextPrimary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                ) {
                                    Text(text = "n$b", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Reset 5G Bands button
                        OutlinedButton(
                            onClick = {
                                pendingConfirmation = Pair("Buka kunci dan aktifkan semua band 5G NR (Auto)?") {
                                    val full = com.example.odumonitor.util.FrequencyConverter.FULL_5G_BANDS.joinToString(",")
                                    onSet5gBandLock(full, "Auto / Semua")
                                }
                            },
                            enabled = !isExecutingRadioCommand,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceCardSubtle,
                                contentColor = TextSecondary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Buka Semua Band 5G (Auto)", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Section 4: Cell Lock (PCI & Channel)
                        Text(
                            text = "4. KUNCI PEMANCAR SEL (CELL LOCK)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kunci antena ke menara BTS aktif untuk mencegah perpindahan pemancar.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // 4G Cell Lock Box
                        val lock4gRaw = signalState.lockLteCell
                        val is4gCellLocked = !lock4gRaw.isNullOrBlank() && lock4gRaw != "0,0"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceCardSubtle)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Sel 4G LTE",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (is4gCellLocked) "Terkunci ($lock4gRaw)" else "Bebas (Auto)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (is4gCellLocked) SignalFair else AccentPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

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
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                                    ) {
                                        Text(
                                            text = if (canLock4g) "Kunci Sel 4G (PCI ${signalState.ltePci})" else "Sel 4G Tidak Tersedia",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BgBase,
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
                                        modifier = Modifier.weight(0.6f),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = SurfaceCard,
                                            contentColor = TextPrimary
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Text(text = "Buka Kunci", fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 5G Cell Lock Box
                        val lock5gRaw = signalState.lockNrCell
                        val is5gCellLocked = !lock5gRaw.isNullOrBlank() && lock5gRaw != "0,0,0"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceCardSubtle)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Sel 5G NR",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (is5gCellLocked) "Terkunci ($lock5gRaw)" else "Bebas (Auto)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (is5gCellLocked) SignalFair else AccentPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

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
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                                    ) {
                                        Text(
                                            text = if (canLock5g) "Kunci Sel 5G (PCI ${signalState.nrPci})" else "Sel 5G Tidak Tersedia",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BgBase,
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
                                        modifier = Modifier.weight(0.6f),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = SurfaceCard,
                                            contentColor = TextPrimary
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Text(text = "Buka Kunci", fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "* Catatan: Jika modem kehilangan sinyal setelah mengunci sel, tekan tombol 'Buka Kunci' lalu ubah mode jaringan atau tunggu modem sinkronisasi ulang.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BgBase,
                checkedTrackColor = AccentPrimary,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceCardSubtle
            )
        )
    }
}
