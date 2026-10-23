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
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
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
        val signalState = repository.fetchCurrentSignalOnce()
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
        OduCompactWidget().update(context, glanceId)
    }
}

@Composable
fun CompactWidgetContent(signal: OduSignalState, config: WidgetConfig, launchIntent: android.content.Intent) {
    val statusBgColor = if (signal.isConnected) {
        GlanceTheme.colors.primaryContainer
    } else {
        GlanceTheme.colors.errorContainer
    }
    val statusTextColor = if (signal.isConnected) {
        GlanceTheme.colors.onPrimaryContainer
    } else {
        GlanceTheme.colors.onErrorContainer
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(10.dp)
            .clickable(actionStartActivity(launchIntent))
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .background(statusBgColor)
                        .cornerRadius(6.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (signal.isConnected) signal.connectionType else "ODU TERPUTUS",
                        style = TextStyle(
                            color = statusTextColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Box(
                    modifier = GlanceModifier
                        .cornerRadius(6.dp)
                        .padding(6.dp)
                        .clickable(actionRunCallback<RefreshCompactWidgetAction>())
                ) {
                    Text(
                        text = "↻",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            if (!signal.isConnected) {
                Text(
                    text = "ODU tidak terjangkau",
                    style = TextStyle(
                        color = GlanceTheme.colors.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Periksa koneksi Wi-Fi/LAN router",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (config.show5g) {
                        val has5g = signal.nrRsrp != 0
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = if (config.showBandInfo && signal.nrBand != "-") "5G (${signal.nrBand})" else "5G NR",
                                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (has5g) "${signal.nrRsrp} dBm" else "-- dBm",
                                style = TextStyle(
                                    color = if (has5g) ColorProvider(
                                        day = getRsrpColor(signal.nrRsrp, isDark = false),
                                        night = getRsrpColor(signal.nrRsrp, isDark = true)
                                    ) else GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (config.showSinrRsrq) {
                                Text(
                                    text = if (has5g) "SNR: ${signal.nrSinr} dB" else "Siaga",
                                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 9.sp)
                                )
                            }
                        }
                    }

                    if (config.show5g && config.show4g) {
                        Spacer(modifier = GlanceModifier.width(6.dp))
                    }

                    if (config.show4g) {
                        val has4g = signal.lteRsrp != 0
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = if (config.showBandInfo && signal.lteBand != "-") "4G (${signal.lteBand})" else "4G LTE",
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (has4g) "${signal.lteRsrp} dBm" else "-- dBm",
                                style = TextStyle(
                                    color = if (has4g) ColorProvider(
                                        day = getRsrpColor(signal.lteRsrp, isDark = false),
                                        night = getRsrpColor(signal.lteRsrp, isDark = true)
                                    ) else GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (config.showSinrRsrq) {
                                Text(
                                    text = if (has4g) "SNR: ${signal.lteSinr} dB" else "Siaga",
                                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 9.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
