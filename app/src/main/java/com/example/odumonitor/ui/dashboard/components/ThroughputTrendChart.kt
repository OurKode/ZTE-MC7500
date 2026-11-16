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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.odumonitor.R
import com.example.odumonitor.data.local.AppearanceConfig
import com.example.odumonitor.data.local.ChartTimeRange
import com.example.odumonitor.data.local.RetroSkin
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.ui.common.TimedMetricPoint
import com.example.odumonitor.ui.common.formatSpeed
import com.example.odumonitor.ui.theme.LocalRetroSkin
import com.example.odumonitor.ui.theme.PlateLabel
import com.example.odumonitor.ui.theme.UnitLabel
import com.example.odumonitor.ui.theme.SignalExcellent
import com.example.odumonitor.ui.theme.SignalExcellentLight

@Composable
fun ThroughputTrendChart(
    history: List<OduSignalState>,
    chartTimeRange: ChartTimeRange,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = LocalRetroSkin.current == RetroSkin.CHARCOAL
    val dlColor = MaterialTheme.colorScheme.primary
    val ulColor = if (isDark) SignalExcellent else SignalExcellentLight
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)

    val dlPoints = if (isConnected) history.map { TimedMetricPoint(it.lastUpdated, it.downloadSpeedBps.toFloat()) } else emptyList()
    val ulPoints = if (isConnected) history.map { TimedMetricPoint(it.lastUpdated, it.uploadSpeedBps.toFloat()) } else emptyList()

    val avgDl = if (dlPoints.isNotEmpty()) dlPoints.map { it.value.toDouble() }.average().toLong() else 0L
    val actualMax = maxOf(
        dlPoints.maxOfOrNull { it.value } ?: 0f,
        ulPoints.maxOfOrNull { it.value } ?: 0f
    )

    // Dynamic Y-Axis scale ceiling based strictly on peak/max traffic in the active time range.
    // If the peak is 60 Mbps, the scale ceiling is exactly 60 Mbps, and dynamically adapts on range change.
    val maxVal = if (actualMax > 0f) actualMax else 125_000f // Fallback to 1 Mbps when traffic is 0

    val chartHeightDp = 76.dp

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
                style = UnitLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
            Text(
                text = stringResource(R.string.chart_scale, "0", formatSpeed(maxVal.toLong())),
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
            if (!isConnected || history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (!isConnected) {
                            stringResource(R.string.chart_paused_traffic)
                        } else {
                            stringResource(R.string.chart_collecting_samples)
                        },
                        style = UnitLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawLine(gridColor, Offset(0f, 0f), Offset(w, 0f), 1f)
                    drawLine(gridColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), 1f)
                    drawLine(gridColor, Offset(0f, h), Offset(w, h), 1f)

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
                                colors = listOf(color.copy(alpha = 0.14f), color.copy(alpha = 0.0f))
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
                text = stringResource(R.string.chart_traffic_title, chartTimeRange.shortLabel.uppercase()),
                style = PlateLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.chart_average_peak,
                    if (isConnected) formatSpeed(avgDl) else "--",
                    if (isConnected) formatSpeed(actualMax.toLong()) else "--"
                ),
                style = UnitLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
