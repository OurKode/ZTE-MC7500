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
import java.text.SimpleDateFormat
import java.util.*

class OduDetailedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OduDetailedWidget()
}

class OduDetailedWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = OduRepository()
        val signalState = repository.fetchCurrentSignalOnce()

        provideContent {
            GlanceTheme {
                DetailedWidgetContent(signalState)
            }
        }
    }
}

@Composable
fun DetailedWidgetContent(signal: OduSignalState) {
    // Doppelrand Outer Shell
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(SurfaceDarkShell))
            .cornerRadius(22.dp)
            .padding(4.dp)
    ) {
        // Doppelrand Inner Core
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(SurfaceDarkCore))
                .cornerRadius(18.dp)
                .padding(14.dp)
        ) {
            // Row 1: Header Bar & Status Badges
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection Status Badge
                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(if (signal.isConnected) SignalExcellent.copy(alpha = 0.15f) else SignalPoor.copy(alpha = 0.15f)))
                        .cornerRadius(8.dp)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (signal.isConnected) "${signal.connectionType} • ${signal.provider}" else "ODU DISCONNECTED",
                        style = TextStyle(
                            color = ColorProvider(if (signal.isConnected) SignalExcellent else SignalPoor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(signal.lastUpdated)),
                    style = TextStyle(color = ColorProvider(TextMuted), fontSize = 10.sp)
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Row 2: 5G & 4G Dual Telemetry Bento Cards (Expanding to fill height)
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight()
            ) {
                // 5G NR Card
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .background(ColorProvider(SurfaceDarkShell))
                        .cornerRadius(14.dp)
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SINYAL 5G",
                                style = TextStyle(color = ColorProvider(AccentCyan), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = signal.nrBand,
                                style = TextStyle(color = ColorProvider(AccentCyan), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Text(
                            text = "Kekuatan: ${signal.nrRsrp} dBm",
                            style = TextStyle(color = ColorProvider(getRsrpColor(signal.nrRsrp)), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "Kejernihan: ${signal.nrSinr} dB",
                            style = TextStyle(color = ColorProvider(getSinrColor(signal.nrSinr)), fontSize = 12.sp)
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "Kualitas Penerimaan: ${signal.nrRsrq} dB",
                            style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 10.sp)
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "ID Pemancar (PCI): ${signal.nrPci}",
                            style = TextStyle(color = ColorProvider(TextMuted), fontSize = 10.sp)
                        )
                        Text(
                            text = "ID Sektor Sel: ${if (signal.nrCellId > 0) signal.nrCellId else "-"}",
                            style = TextStyle(color = ColorProvider(TextMuted), fontSize = 9.sp)
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // 4G LTE Card
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .background(ColorProvider(SurfaceDarkShell))
                        .cornerRadius(14.dp)
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SINYAL 4G LTE",
                                style = TextStyle(color = ColorProvider(AccentPurple), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = signal.lteBand,
                                style = TextStyle(color = ColorProvider(AccentPurple), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Text(
                            text = "Kekuatan: ${signal.lteRsrp} dBm",
                            style = TextStyle(color = ColorProvider(getRsrpColor(signal.lteRsrp)), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "Kejernihan: ${signal.lteSinr} dB",
                            style = TextStyle(color = ColorProvider(getSinrColor(signal.lteSinr)), fontSize = 12.sp)
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "Kualitas Penerimaan: ${signal.lteRsrq} dB",
                            style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 10.sp)
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "ID Pemancar (PCI): ${signal.ltePci}",
                            style = TextStyle(color = ColorProvider(TextMuted), fontSize = 10.sp)
                        )
                        Text(
                            text = "ID Sektor Sel: ${if (signal.lteCellId > 0) signal.lteCellId else "-"}",
                            style = TextStyle(color = ColorProvider(TextMuted), fontSize = 9.sp)
                        )
                    }
                }
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
