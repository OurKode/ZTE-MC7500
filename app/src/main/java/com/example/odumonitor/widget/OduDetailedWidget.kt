package com.example.odumonitor.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.odumonitor.MainActivity
import com.example.odumonitor.data.local.WidgetConfig
import com.example.odumonitor.data.local.WidgetPreferences
import com.example.odumonitor.data.model.OduSignalState
import com.example.odumonitor.data.repository.OduRepository
import com.example.odumonitor.ui.theme.*
import com.example.odumonitor.worker.WidgetUpdateManager
import java.text.SimpleDateFormat
import java.util.*

class OduDetailedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OduDetailedWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val config = WidgetPreferences(context).getWidgetConfig()
        WidgetUpdateManager.scheduleWidgetUpdates(context, config.updateIntervalMinutes)
    }
}

class OduDetailedWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = OduRepository(context)
        // Cache-first reading to prevent rapid-fire network requests during widget updates
        val signalState = repository.getLastCachedSignal() ?: repository.fetchCurrentSignalOnce()
        val config = WidgetPreferences(context).getWidgetConfig()
        val launchIntent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            GlanceTheme {
                DetailedWidgetContent(signalState, config, launchIntent)
            }
        }
    }
}

class RefreshDetailedWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repository = OduRepository(context)
        repository.fetchCurrentSignalOnce()
        OduDetailedWidget().update(context, glanceId)
    }
}

@Composable
fun DetailedWidgetContent(signal: OduSignalState, config: WidgetConfig, launchIntent: android.content.Intent) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(SurfaceCard))
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(launchIntent))
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            // Header Bar & Status
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (config.showProviderStatus) {
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(if (signal.isConnected) SignalExcellent.copy(alpha = 0.15f) else SignalPoor.copy(alpha = 0.15f)))
                            .cornerRadius(6.dp)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
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
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Timestamp
                if (config.showTimestamp) {
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(signal.lastUpdated)),
                        style = TextStyle(color = ColorProvider(TextMuted), fontSize = 10.sp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Tap to refresh button
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(6.dp)
                        .padding(4.dp)
                        .clickable(actionRunCallback<RefreshDetailedWidgetAction>())
                ) {
                    Text(
                        text = "↻",
                        style = TextStyle(
                            color = ColorProvider(AccentPrimary),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Row 2: 5G & 4G Dual Telemetry
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight()
            ) {
                // 5G NR Cell
                if (config.show5g) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(ColorProvider(SurfaceCardSubtle))
                            .cornerRadius(10.dp)
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "5G NR",
                                    style = TextStyle(color = ColorProvider(AccentPrimary), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = GlanceModifier.defaultWeight())
                                if (config.showBandInfo && signal.nrBand != "-") {
                                    Text(
                                        text = signal.nrBand,
                                        style = TextStyle(color = ColorProvider(AccentPrimary), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = "${signal.nrRsrp} dBm",
                                style = TextStyle(color = ColorProvider(getRsrpColor(signal.nrRsrp)), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            )
                            if (config.showSinrRsrq) {
                                Text(
                                    text = "SNR: ${signal.nrSinr} dB",
                                    style = TextStyle(color = ColorProvider(getSinrColor(signal.nrSinr)), fontSize = 11.sp)
                                )
                                Text(
                                    text = "RSRQ: ${signal.nrRsrq} dB",
                                    style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 10.sp)
                                )
                            }
                            if (config.showPciTower) {
                                Text(
                                    text = "PCI: ${signal.nrPci}",
                                    style = TextStyle(color = ColorProvider(TextMuted), fontSize = 9.sp)
                                )
                            }
                        }
                    }
                }

                if (config.show5g && config.show4g) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                }

                // 4G LTE Cell
                if (config.show4g) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(ColorProvider(SurfaceCardSubtle))
                            .cornerRadius(10.dp)
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "4G LTE",
                                    style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = GlanceModifier.defaultWeight())
                                if (config.showBandInfo && signal.lteBand != "-") {
                                    Text(
                                        text = signal.lteBand,
                                        style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = "${signal.lteRsrp} dBm",
                                style = TextStyle(color = ColorProvider(getRsrpColor(signal.lteRsrp)), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            )
                            if (config.showSinrRsrq) {
                                Text(
                                    text = "SNR: ${signal.lteSinr} dB",
                                    style = TextStyle(color = ColorProvider(getSinrColor(signal.lteSinr)), fontSize = 11.sp)
                                )
                                Text(
                                    text = "RSRQ: ${signal.lteRsrq} dB",
                                    style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 10.sp)
                                )
                            }
                            if (config.showPciTower) {
                                Text(
                                    text = "PCI: ${signal.ltePci}",
                                    style = TextStyle(color = ColorProvider(TextMuted), fontSize = 9.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
