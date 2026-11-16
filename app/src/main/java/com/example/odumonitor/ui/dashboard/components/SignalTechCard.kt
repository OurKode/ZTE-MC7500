package com.example.odumonitor.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.odumonitor.R
import com.example.odumonitor.data.local.AppearanceConfig
import com.example.odumonitor.data.local.ChartTimeRange
import com.example.odumonitor.data.local.RetroSkin
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.ui.common.ChartMetric
import com.example.odumonitor.ui.theme.*

/**
 * One radio technology, one plate. The active metric is underlined rather than
 * filled, because the trace below belongs to it and a filled block would read as
 * a pressed button instead of a selected source.
 */
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
    is5g: Boolean,
    appearanceConfig: AppearanceConfig = AppearanceConfig()
) {
    val hasValidSignal = isConnected && rsrp != 0
    val night = LocalRetroSkin.current == RetroSkin.CHARCOAL
    val isChartVisible = if (is5g) appearanceConfig.show5gChart else appearanceConfig.show4gChart

    InstrumentPlate(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = PlateLabel,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val cleanBand = when {
                    band.startsWith("LTE BAND ", ignoreCase = true) -> "B" + band.substringAfter("LTE BAND ", "").trim()
                    band.startsWith("BAND ", ignoreCase = true) -> "B" + band.substringAfter("BAND ", "").trim()
                    else -> band
                }
                val freqPart = dlFreqMhz?.let { "  ${it.toInt()} MHz" } ?: ""
                val displayBand = when {
                    !isConnected -> stringResource(R.string.traffic_state_offline)
                    cleanBand.isNotBlank() && cleanBand != "-" -> {
                        val prefix = if (cleanBand.startsWith("B", true) || cleanBand.startsWith("n", true)) "" else "B"
                        "$prefix$cleanBand$freqPart"
                    }
                    else -> stringResource(R.string.tech_no_signal)
                }

                Text(
                    text = displayBand,
                    style = UnitLabel,
                    color = if (isConnected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(10.dp))
            InstrumentDivider()
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricColumnItem(
                    label = stringResource(R.string.tech_rsrp),
                    valueText = if (hasValidSignal) "$rsrp" else "--",
                    unitText = stringResource(R.string.tech_dbm),
                    valueColor = if (hasValidSignal) getRsrpColor(rsrp, night) else MaterialTheme.colorScheme.onSurfaceVariant,
                    isSelected = selectedMetric == ChartMetric.RSRP,
                    onClick = { onMetricSelected(ChartMetric.RSRP) },
                    modifier = Modifier.weight(1f)
                )

                MetricColumnItem(
                    label = stringResource(R.string.tech_sinr),
                    valueText = if (hasValidSignal) "$sinr" else "--",
                    unitText = stringResource(R.string.tech_db),
                    valueColor = if (hasValidSignal) getSinrColor(sinr, night) else MaterialTheme.colorScheme.onSurfaceVariant,
                    isSelected = selectedMetric == ChartMetric.SINR,
                    onClick = { onMetricSelected(ChartMetric.SINR) },
                    modifier = Modifier.weight(1f)
                )

                MetricColumnItem(
                    label = stringResource(R.string.tech_rsrq),
                    valueText = if (hasValidSignal) "$rsrq" else "--",
                    unitText = stringResource(R.string.tech_db),
                    valueColor = if (hasValidSignal) getRsrqColor(rsrq, night) else MaterialTheme.colorScheme.onSurfaceVariant,
                    isSelected = selectedMetric == ChartMetric.RSRQ,
                    onClick = { onMetricSelected(ChartMetric.RSRQ) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (isChartVisible) {
                Spacer(Modifier.height(4.dp))
                InstrumentDivider()
                Spacer(Modifier.height(10.dp))

                SignalLineChart(
                    history = history,
                    isConnected = isConnected,
                    metric = selectedMetric,
                    chartTimeRange = chartTimeRange,
                    is5g = is5g
                )
            }

            Spacer(Modifier.height(10.dp))
            InstrumentDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val chanPrefix = stringResource(if (is5g) R.string.tech_arfcn else R.string.tech_earfcn)
                val chanText = if (isConnected && earfcnOrArfcn != null) "$chanPrefix $earfcnOrArfcn" else "$chanPrefix -"
                val bwText = if (isConnected && bandwidth != null) "  $bandwidth" else ""
                Text(
                    text = "$chanText$bwText",
                    style = UnitLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isConnected) {
                        val p = stringResource(R.string.tech_pci)
                        val c = stringResource(R.string.tech_cell_id)
                        "$p ${if (pci > 0) pci else "-"}  $c ${if (cellId > 0) cellId else "-"}"
                    } else {
                        "${stringResource(R.string.tech_pci)} -  ${stringResource(R.string.tech_cell_id)} -"
                    },
                    style = UnitLabel,
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
    Box(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(MaterialTheme.shapes.small)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = UnitLabel,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                if (unitText.isNotBlank()) {
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = unitText,
                        style = UnitLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            if (isSelected) {
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
