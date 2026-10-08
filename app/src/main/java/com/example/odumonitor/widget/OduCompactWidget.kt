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

class OduCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OduCompactWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val config = WidgetPreferences(context).getWidgetConfig()
        WidgetUpdateManager.scheduleWidgetUpdates(context, config.updateIntervalMinutes)
    }
}

class OduCompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = OduRepository(context)
        // Read cached signal first to avoid double-request lockups, fallback to network
        val signalState = repository.getLastCachedSignal() ?: repository.fetchCurrentSignalOnce()
        val config = WidgetPreferences(context).getWidgetConfig()
        val launchIntent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            GlanceTheme {
                CompactWidgetContent(signalState, config, launchIntent)
            }
        }
    }
}

class RefreshCompactWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repository = OduRepository(context)
        repository.fetchCurrentSignalOnce()
        OduCompactWidget().update(context, glanceId)
    }
}

@Composable
fun CompactWidgetContent(signal: OduSignalState, config: WidgetConfig, launchIntent: android.content.Intent) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(SurfaceCard))
            .cornerRadius(14.dp)
            .padding(10.dp)
            .clickable(actionStartActivity(launchIntent))
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header Bar
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection Status Chip
                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(if (signal.isConnected) SignalExcellent.copy(alpha = 0.15f) else SignalPoor.copy(alpha = 0.15f)))
                        .cornerRadius(6.dp)
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

                // Tap to refresh button
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(6.dp)
                        .padding(4.dp)
                        .clickable(actionRunCallback<RefreshCompactWidgetAction>())
                ) {
                    Text(
                        text = "↻",
                        style = TextStyle(
                            color = ColorProvider(AccentPrimary),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Dual Column 5G & 4G Quick Telemetry
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 5G Cell
                if (config.show5g) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = if (config.showBandInfo && signal.nrBand != "-") "5G (${signal.nrBand})" else "5G NR",
                            style = TextStyle(color = ColorProvider(AccentPrimary), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${signal.nrRsrp} dBm",
                            style = TextStyle(
                                color = ColorProvider(getRsrpColor(signal.nrRsrp)),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (config.showSinrRsrq) {
                            Text(
                                text = "SNR: ${signal.nrSinr} dB",
                                style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 9.sp)
                            )
                        }
                    }
                }

                if (config.show5g && config.show4g) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                }

                // 4G Cell
                if (config.show4g) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = if (config.showBandInfo && signal.lteBand != "-") "4G (${signal.lteBand})" else "4G LTE",
                            style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${signal.lteRsrp} dBm",
                            style = TextStyle(
                                color = ColorProvider(getRsrpColor(signal.lteRsrp)),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (config.showSinrRsrq) {
                            Text(
                                text = "SNR: ${signal.lteSinr} dB",
                                style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 9.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
