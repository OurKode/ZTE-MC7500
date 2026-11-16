package com.example.odumonitor.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.odumonitor.R
import com.example.odumonitor.data.local.ChartTimeRange
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The top plate: who we are talking to, how often, and over what window.
 * When the link is down this same plate becomes the alarm, because a meter does
 * not grow a second face when it fails, it lights the one it has.
 */
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
    val isOk = signal.isConnected && signal.errorMessage == null
    val stamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(signal.lastUpdated))

    if (!isOk) {
        InstrumentPlate(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
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
                        SignalLed(level = 2, size = 7.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.status_link_lost),
                            style = PlateLabel,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Text(
                        text = "$routerHost  $stamp",
                        style = UnitLabel,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = signal.errorMessage
                        ?: stringResource(R.string.status_link_lost_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                )

                Spacer(Modifier.height(10.dp))

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
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = MaterialTheme.shapes.small,
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
                    ) {
                        Text(
                            text = if (isRefreshing) stringResource(R.string.action_contacting)
                            else stringResource(R.string.action_retry),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenRouterSettings,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(stringResource(R.string.action_router_ip), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    } else {
        val nextInterval = when (pollingInterval) {
            1000L -> 3000L
            3000L -> 5000L
            else -> 1000L
        }
        val intervalLabel = "${pollingInterval / 1000}s"

        InstrumentPlate(modifier = Modifier.fillMaxWidth()) {
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
                        SignalLed(level = 0)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = signal.provider,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        InstrumentTag(text = "${signal.connectionType}  ${signal.signalBar}/5")
                    }

                    Spacer(Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onPollingIntervalSelected(nextInterval) }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.status_refresh, intervalLabel),
                            style = PlateLabel,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                InstrumentDivider()
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartTimeRange.entries.forEach { rangeOption ->
                        val isSelected = chartTimeRange == rangeOption
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onChartTimeRangeSelected(rangeOption) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rangeOption.shortLabel.uppercase(),
                                style = if (isSelected) {
                                    MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    UnitLabel.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth(0.55f)
                                        .height(2.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
