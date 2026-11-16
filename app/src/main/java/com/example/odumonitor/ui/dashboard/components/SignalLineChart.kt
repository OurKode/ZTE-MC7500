package com.example.odumonitor.ui.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.odumonitor.R
import com.example.odumonitor.data.local.AppearanceConfig
import com.example.odumonitor.data.local.ChartTimeRange
import com.example.odumonitor.data.local.RetroSkin
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.ui.common.ChartMetric
import com.example.odumonitor.ui.common.TimedMetricPoint
import com.example.odumonitor.ui.theme.LocalRetroSkin
import com.example.odumonitor.ui.theme.PlateLabel
import com.example.odumonitor.ui.theme.UnitLabel
import com.example.odumonitor.ui.theme.getRsrqColor
import com.example.odumonitor.ui.theme.getRsrpColor
import com.example.odumonitor.ui.theme.getSinrColor
import java.util.Locale

@Composable
fun SignalLineChart(
    history: List<OduSignalState>,
    isConnected: Boolean,
    metric: ChartMetric,
    chartTimeRange: ChartTimeRange = ChartTimeRange.FIVE_MINUTES,
    is5g: Boolean
) {
    val isDark = LocalRetroSkin.current == RetroSkin.CHARCOAL
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
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
                .height(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (!isConnected) {
                    stringResource(R.string.chart_paused_signal)
                } else {
                    stringResource(R.string.chart_collecting)
                },
                style = UnitLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        return
    }

    val minVal = timedPoints.minOf { it.value }
    val maxVal = timedPoints.maxOf { it.value }
    val avgVal = timedPoints.map { it.value.toDouble() }.average().toFloat()
    val currentVal = timedPoints.last().value

    // Dynamic Y-Axis scale directly reflecting min and peak/max in the current time window.
    val yMax = maxVal
    val yMin = if (minVal == maxVal) minVal - 1f else minVal
    val totalYSpan = (yMax - yMin).coerceAtLeast(0.5f)

    val chartHeightDp = 76.dp

    val metricColor = when (metric) {
        ChartMetric.RSRQ -> getRsrqColor(currentVal.toInt(), isDark)
        ChartMetric.RSRP -> getRsrpColor(currentVal.toInt(), isDark)
        ChartMetric.SINR -> getSinrColor(currentVal, isDark)
    }

    val windowEndMs = timedPoints.last().timestampMs
    val windowDurationMs = chartTimeRange.durationMillis.coerceAtLeast(1000L)
    val windowStartMs = windowEndMs - windowDurationMs
    val maxGapMs = maxOf((windowDurationMs / 60L) * 4L, 15_000L)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "-${chartTimeRange.shortLabel.uppercase()}",
                style = UnitLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
            val scaleMinStr = if (metric == ChartMetric.SINR) String.format(Locale.US, "%.1f", yMin) else yMin.toInt().toString()
            val scaleMaxStr = if (metric == ChartMetric.SINR) String.format(Locale.US, "%.1f", yMax) else yMax.toInt().toString()
            Text(
                text = stringResource(R.string.chart_scale, "$scaleMinStr ${metric.unit}", scaleMaxStr),
                style = UnitLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
            Text(
                text = "0s",
                style = UnitLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeightDp)
                .padding(vertical = 4.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                drawLine(gridColor, Offset(0f, 0f), Offset(width, 0f), 1f)
                drawLine(gridColor, Offset(0f, height / 2f), Offset(width, height / 2f), 1f)
                drawLine(gridColor, Offset(0f, height), Offset(width, height), 1f)

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
                            metricColor.copy(alpha = 0.14f),
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
                style = PlateLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.chart_min_avg_max,
                    minVal.toInt().toString(),
                    avgFormatted,
                    "${maxVal.toInt()} ${metric.unit}"
                ),
                style = UnitLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
