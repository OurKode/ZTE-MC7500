package com.example.odumonitor.ui.dashboard.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.odumonitor.R
import com.example.odumonitor.data.local.AppearanceConfig
import com.example.odumonitor.data.local.ChartTimeRange
import com.example.odumonitor.data.local.RetroSkin
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.ui.common.formatBytes
import com.example.odumonitor.ui.common.formatSpeed
import com.example.odumonitor.ui.theme.*
import com.example.odumonitor.util.FrequencyConverter

/**
 * The main plate: two hero readouts in segment windows, a trend trace under them,
 * and a silk-screened grid of device counters.
 *
 * The hero numbers are drawn seven-segment (see SevenSegmentReadout) because this
 * is the value you read from across the room. Everything else is ordinary type,
 * because a wall of glowing segments is unreadable and this is still a tool.
 */
@Composable
fun RouterTrafficCard(
    signal: OduSignalState,
    history: List<OduSignalState>,
    chartTimeRange: ChartTimeRange,
    isExpandedWidth: Boolean,
    onOpenSettings: () -> Unit,
    appearanceConfig: AppearanceConfig = AppearanceConfig()
) {
    val night = LocalRetroSkin.current == RetroSkin.CHARCOAL
    val motion = LocalMotionScale.current
    val upColor = signalColor(0, night)
    val isConnected = signal.isConnected

    InstrumentPlate(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.traffic_title),
                    style = PlateLabel,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onOpenSettings)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SignalLed(
                        level = when {
                            !isConnected -> 2
                            signal.isRouterLoggedIn -> 0
                            else -> 1
                        },
                        size = 6.dp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            !isConnected -> stringResource(R.string.traffic_state_offline)
                            signal.isRouterLoggedIn -> stringResource(R.string.traffic_state_admin)
                            else -> stringResource(R.string.traffic_state_guest)
                        },
                        style = UnitLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            InstrumentDivider()
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                HeroReadout(
                    label = stringResource(R.string.traffic_download),
                    value = if (isConnected) formatSpeed(signal.downloadSpeedBps) else null,
                    color = MaterialTheme.colorScheme.onSurface,
                    motionScale = motion,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                HeroReadout(
                    label = stringResource(R.string.traffic_upload),
                    value = if (isConnected) formatSpeed(signal.uploadSpeedBps) else null,
                    color = upColor,
                    motionScale = motion,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }

            if (appearanceConfig.showTrafficChart) {
                Spacer(Modifier.height(10.dp))
                InstrumentDivider()
                Spacer(Modifier.height(10.dp))
                ThroughputTrendChart(
                    history = history,
                    chartTimeRange = chartTimeRange,
                    isConnected = isConnected
                )
            }

            Spacer(Modifier.height(10.dp))
            InstrumentDivider()
            Spacer(Modifier.height(10.dp))

            // Daily packet errors and drops are exposed by the router and shown
            // nowhere else, so they are flagged the moment they are non-zero.
            val errPackets = signal.dayRxErrorPackets + signal.dayTxErrorPackets
            val dropPackets = signal.dayRxDropPackets + signal.dayTxDropPackets
            val hasPacketTrouble = errPackets > 0L || dropPackets > 0L

            val metrics = listOf(
                Triple(stringResource(R.string.metric_wan_ip), if (isConnected) (signal.wanIp ?: "-") else "-", false),
                Triple(stringResource(R.string.metric_sim_no), if (isConnected) (signal.simPhoneNumber ?: "-") else "-", false),
                Triple(
                    stringResource(R.string.metric_cpu_temp),
                    if (isConnected) {
                        signal.cpuTemp?.let { stringResource(R.string.unit_celsius, it) } ?: "-"
                    } else "-",
                    false
                ),
                Triple(stringResource(R.string.metric_today), if (isConnected && signal.dayRxBytes > 0) formatBytes(signal.dayRxBytes) else "-", false),
                Triple(stringResource(R.string.metric_month), if (isConnected && signal.monthRxBytes > 0) formatBytes(signal.monthRxBytes) else "-", false),
                Triple(stringResource(R.string.metric_uptime), if (isConnected) (signal.deviceUptimeSeconds?.let { FrequencyConverter.formatUptime(it) } ?: "-") else "-", false),
                Triple(stringResource(R.string.metric_session), if (isConnected) FrequencyConverter.formatUptime(signal.connectionDurationSeconds) else "-", false),
                Triple(stringResource(R.string.metric_err_drop), if (isConnected) "$errPackets/$dropPackets" else "-", hasPacketTrouble)
            )

            val cols = if (isExpandedWidth) 4 else 2
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                metrics.chunked(cols).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { (lbl, value, isAlert) ->
                            RouterMetricItem(
                                label = lbl,
                                value = value,
                                modifier = Modifier.weight(1f),
                                valueColor = if (isAlert) MaterialTheme.colorScheme.error else null
                            )
                        }
                        repeat(cols - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Splits "17.8 Mbps" into the numeral and its unit so the numeral can be drawn
 * as segments and the unit can stay as type on the plate.
 */
private fun splitReading(reading: String): Pair<String, String> {
    val cut = reading.lastIndexOf(' ')
    return if (cut <= 0) reading to "" else reading.substring(0, cut) to reading.substring(cut + 1)
}

@Composable
private fun HeroReadout(
    label: String,
    value: String?,
    color: Color,
    motionScale: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(vertical = 4.dp)) {
        PlateLabelText(text = label)
        Spacer(Modifier.height(6.dp))
        if (value == null) {
            Text(
                text = "--",
                style = HeroNumeral,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val (numeral, unit) = splitReading(value)
            Row(verticalAlignment = Alignment.Bottom) {
                Crossfade(
                    targetState = numeral,
                    animationSpec = RetroMotion.value(motionScale),
                    label = "hero-numeral"
                ) { shown ->
                    SevenSegmentReadout(
                        text = shown,
                        color = color,
                        digitHeight = 34.dp
                    )
                }
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = unit,
                        style = UnitLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RouterMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    Box(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = UnitLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = GridValue,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
