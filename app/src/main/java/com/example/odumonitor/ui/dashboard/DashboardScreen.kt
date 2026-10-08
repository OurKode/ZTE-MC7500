package com.example.odumonitor.ui.dashboard

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Widgets
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
import com.example.odumonitor.data.local.WidgetConfig
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

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
    onWidgetConfigChanged: (WidgetConfig) -> Unit
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

                // 5G NR Telemetry & Real-Time Trend Card
                SignalTechCard(
                    title = "5G NR",
                    band = uiState.signalState.nrBand,
                    rsrp = uiState.signalState.nrRsrp,
                    sinr = uiState.signalState.nrSinr,
                    rsrq = uiState.signalState.nrRsrq,
                    pci = uiState.signalState.nrPci,
                    cellId = uiState.signalState.nrCellId,
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

    // Modal Bottom Sheet for Widget Configuration
    if (showWidgetSheet) {
        WidgetConfigBottomSheet(
            config = uiState.widgetConfig,
            onConfigChanged = onWidgetConfigChanged,
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
                    Text(
                        text = if (band.isNotBlank() && band != "-") "Band $band" else "Siaga",
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

            // Secondary Cell Telemetry (PCI, Cell ID)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID Pemancar (PCI): $pci",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Sektor Sel: ${if (cellId > 0) cellId else "-"}",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigBottomSheet(
    config: WidgetConfig,
    onConfigChanged: (WidgetConfig) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BorderSubtle)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
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
                        color = if (config.updateIntervalMinutes == min) AccentPrimary else TextPrimary,
                        fontWeight = if (config.updateIntervalMinutes == min) FontWeight.Bold else FontWeight.Normal
                    )
                    RadioButton(
                        selected = config.updateIntervalMinutes == min,
                        onClick = { onConfigChanged(config.copy(updateIntervalMinutes = min)) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = AccentPrimary,
                            unselectedColor = TextMuted
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
