package com.example.odumonitor.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.odumonitor.R
import com.example.odumonitor.data.local.RetroSkin
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.ui.theme.*

@Composable
fun CarrierAggregationCard(signal: OduSignalState) {
    val night = LocalRetroSkin.current == RetroSkin.CHARCOAL
    val activeColor = signalColor(0, night)
    val isConnected = signal.isConnected
    val isActive = isConnected && signal.isCaActive

    InstrumentPlate(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ca_title),
                    style = PlateLabel,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SignalLed(level = when {
                        !isConnected -> 2
                        isActive -> 0
                        else -> 1
                    }, size = 6.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            !isConnected -> stringResource(R.string.traffic_state_offline)
                            isActive -> stringResource(R.string.ca_active)
                            else -> stringResource(R.string.ca_inactive)
                        },
                        style = UnitLabel,
                        color = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isActive && signal.caDetails != "-") {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = signal.caDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(8.dp))
            InstrumentDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ca_neighbor_cells),
                    style = UnitLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isConnected) "${signal.neighborCells}" else "-",
                    style = GridValue,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
