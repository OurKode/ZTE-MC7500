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
        val signalState = repository.fetchCurrentSignalOnce()
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
        OduDetailedWidget().update(context, glanceId)
    }
}

@Composable
fun DetailedWidgetContent(signal: OduSignalState, config: WidgetConfig, launchIntent: android.content.Intent) {
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
            .padding(12.dp)
            .clickable(actionStartActivity(launchIntent))
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (config.showProviderStatus) {
                    Box(
                        modifier = GlanceModifier
                            .background(statusBgColor)
                            .cornerRadius(6.dp)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (signal.isConnected) "${signal.connectionType} • ${signal.provider}" else "ODU TERPUTUS (OFFLINE)",
                            style = TextStyle(
                                color = statusTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                if (config.showTimestamp) {
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(signal.lastUpdated)),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                Box(
                    modifier = GlanceModifier
                        .cornerRadius(6.dp)
                        .padding(6.dp)
                        .clickable(actionRunCallback<RefreshDetailedWidgetAction>())
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

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (!signal.isConnected) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .background(GlanceTheme.colors.errorContainer)
                        .cornerRadius(12.dp)
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = "Koneksi ke ODU Terputus",
                            style = TextStyle(
                                color = GlanceTheme.colors.onErrorContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = signal.errorMessage ?: "Pastikan perangkat terhubung ke jaringan Wi-Fi/LAN router ODU.",
                            style = TextStyle(
                                color = GlanceTheme.colors.onErrorContainer,
                                fontSize = 10.sp
                            ),
                            maxLines = 2
                        )
                    }
                }
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                ) {
                    if (config.show5g) {
                        val has5g = signal.nrRsrp != 0
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight()
                                .background(GlanceTheme.colors.surfaceVariant)
                                .cornerRadius(12.dp)
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "5G NR",
                                        style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = GlanceModifier.defaultWeight())
                                    if (config.showBandInfo && signal.nrBand != "-") {
                                        Text(
                                            text = signal.nrBand,
                                            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    text = if (has5g) "${signal.nrRsrp} dBm" else "-- dBm",
                                    style = TextStyle(
                                        color = if (has5g) ColorProvider(
                                            day = getRsrpColor(signal.nrRsrp, isDark = false),
                                            night = getRsrpColor(signal.nrRsrp, isDark = true)
                                        ) else GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (config.showSinrRsrq) {
                                    Text(
                                        text = if (has5g) "SNR: ${signal.nrSinr} dB" else "SNR: --",
                                        style = TextStyle(
                                            color = if (has5g) ColorProvider(
                                                day = getSinrColor(signal.nrSinr, isDark = false),
                                                night = getSinrColor(signal.nrSinr, isDark = true)
                                            ) else GlanceTheme.colors.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = if (has5g) "RSRQ: ${signal.nrRsrq} dB" else "RSRQ: --",
                                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
                                    )
                                }
                                if (config.showPciTower) {
                                    Text(
                                        text = "PCI: ${if (signal.nrPci > 0) signal.nrPci else "-"}",
                                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 9.sp)
                                    )
                                }
                            }
                        }
                    }

                    if (config.show5g && config.show4g) {
                        Spacer(modifier = GlanceModifier.width(8.dp))
                    }

                    if (config.show4g) {
                        val has4g = signal.lteRsrp != 0
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight()
                                .background(GlanceTheme.colors.surfaceVariant)
                                .cornerRadius(12.dp)
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "4G LTE",
                                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = GlanceModifier.defaultWeight())
                                    if (config.showBandInfo && signal.lteBand != "-") {
                                        Text(
                                            text = signal.lteBand,
                                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    text = if (has4g) "${signal.lteRsrp} dBm" else "-- dBm",
                                    style = TextStyle(
                                        color = if (has4g) ColorProvider(
                                            day = getRsrpColor(signal.lteRsrp, isDark = false),
                                            night = getRsrpColor(signal.lteRsrp, isDark = true)
                                        ) else GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (config.showSinrRsrq) {
                                    Text(
                                        text = if (has4g) "SNR: ${signal.lteSinr} dB" else "SNR: --",
                                        style = TextStyle(
                                            color = if (has4g) ColorProvider(
                                                day = getSinrColor(signal.lteSinr, isDark = false),
                                                night = getSinrColor(signal.lteSinr, isDark = true)
                                            ) else GlanceTheme.colors.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = if (has4g) "RSRQ: ${signal.lteRsrq} dB" else "RSRQ: --",
                                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
                                    )
                                }
                                if (config.showPciTower) {
                                    Text(
                                        text = "PCI: ${if (signal.ltePci > 0) signal.ltePci else "-"}",
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
}
