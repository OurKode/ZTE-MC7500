package com.example.odumonitor.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.data.repository.OduRepository
import com.example.odumonitor.ui.theme.*
import kotlinx.coroutines.runBlocking

class OduCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OduCompactWidget()
}

class OduCompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = OduRepository()
        val signalState = repository.fetchCurrentSignalOnce()

        provideContent {
            GlanceTheme {
                CompactWidgetContent(signalState)
            }
        }
    }
}

@Composable
fun CompactWidgetContent(signal: OduSignalState) {
    // Doppelrand Outer Shell
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(SurfaceDarkShell))
            .cornerRadius(18.dp)
            .padding(3.dp)
    ) {
        // Doppelrand Inner Core
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(SurfaceDarkCore))
                .cornerRadius(15.dp)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header Status Bar
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(if (signal.isConnected) SignalExcellent.copy(alpha = 0.15f) else SignalPoor.copy(alpha = 0.15f)))
                        .cornerRadius(8.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (signal.isConnected) signal.connectionType else "OFFLINE",
                        style = TextStyle(
                            color = ColorProvider(if (signal.isConnected) SignalExcellent else SignalPoor),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Text(
                    text = if (signal.isConnected) signal.provider else "ZTE",
                    style = TextStyle(
                        color = ColorProvider(TextMuted),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Dual Column 5G & 4G Quick Telemetry Metrics
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 5G Cell
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "5G (${signal.nrBand})",
                        style = TextStyle(color = ColorProvider(AccentCyan), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${signal.nrRsrp} dBm",
                        style = TextStyle(
                            color = ColorProvider(getRsrpColor(signal.nrRsrp)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "SNR ${signal.nrSinr} dB",
                        style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 9.sp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(4.dp))

                // 4G Cell
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "4G (${signal.lteBand})",
                        style = TextStyle(color = ColorProvider(AccentPurple), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${signal.lteRsrp} dBm",
                        style = TextStyle(
                            color = ColorProvider(getRsrpColor(signal.lteRsrp)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "SNR ${signal.lteSinr} dB",
                        style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 9.sp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Footer Quick Info Bar (Tower PCI)
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID Pemancar (PCI): 5G ${signal.nrPci} • 4G ${signal.ltePci}",
                    style = TextStyle(color = ColorProvider(TextMuted), fontSize = 9.sp)
                )
            }
        }
    }
}

private fun getRsrpColor(rsrp: Int) = when {
    rsrp >= -80 -> SignalExcellent
    rsrp >= -100 -> SignalFair
    else -> SignalPoor
}

private fun getSinrColor(sinr: Float) = when {
    sinr >= 20f -> SignalExcellent
    sinr >= 10f -> SignalFair
    else -> SignalPoor
}
