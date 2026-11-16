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
import com.example.odumonitor.ui.theme.getRsrpColor
import com.example.odumonitor.ui.theme.getSinrColor
import com.example.odumonitor.worker.WidgetUpdateManager
import java.text.SimpleDateFormat
import java.util.*

class OduDetailedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OduDetailedWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val config = runCatching { WidgetPreferences(context).getWidgetConfig() }
            .getOrDefault(WidgetConfig())
        WidgetUpdateManager.scheduleWidgetUpdates(context, config.updateIntervalMinutes)
    }
}

class OduDetailedWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val config = runCatching { WidgetPreferences(context).getWidgetConfig() }
            .getOrDefault(WidgetConfig())
        val signal = loadWidgetSignal(context)
        val text = widgetText(context, signal, config, detailed = true)
        val launchIntent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            GlanceTheme {
                DetailedWidgetContent(signal, config, text, launchIntent)
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
fun DetailedWidgetContent(
    signal: OduSignalState,
    config: WidgetConfig,
    text: WidgetText,
    launchIntent: android.content.Intent
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetPalette.shell)
            .cornerRadius(3.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = GlanceModifier
                                .width(6.dp)
                                .height(6.dp)
                                .background(if (signal.isConnected) WidgetPalette.good else WidgetPalette.bad)
                                .cornerRadius(3.dp)
                        ) {}
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = text.status,
                            style = TextStyle(
                                color = if (signal.isConnected) WidgetPalette.ink else WidgetPalette.bad,
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
                        style = TextStyle(color = WidgetPalette.ink2, fontSize = 10.sp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                Box(
                    modifier = GlanceModifier
                        .cornerRadius(2.dp)
                        .padding(6.dp)
                        .clickable(actionRunCallback<RefreshDetailedWidgetAction>())
                ) {
                    Text(
                        text = "\u21bb",
                        style = TextStyle(
                            color = WidgetPalette.accent,
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
                        .background(WidgetPalette.inset)
                        .cornerRadius(3.dp)
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = text.offlineHeadline,
                            style = TextStyle(
                                color = WidgetPalette.bad,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = text.offlineDetail,
                            style = TextStyle(
                                color = WidgetPalette.ink2,
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
                    text.fiveG?.let { fiveG ->
                        SignalPlate(
                            modifier = GlanceModifier.defaultWeight(),
                            section = fiveG,
                            rsrp = signal.nrRsrp,
                            sinr = signal.nrSinr,
                            labelColor = WidgetPalette.accent
                        )
                    }

                    if (text.fiveG != null && text.fourG != null) {
                        Spacer(modifier = GlanceModifier.width(8.dp))
                    }

                    text.fourG?.let { fourG ->
                        SignalPlate(
                            modifier = GlanceModifier.defaultWeight(),
                            section = fourG,
                            rsrp = signal.lteRsrp,
                            sinr = signal.lteSinr,
                            labelColor = WidgetPalette.ink2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalPlate(
    modifier: GlanceModifier,
    section: WidgetSectionText,
    rsrp: Int,
    sinr: Float,
    labelColor: androidx.glance.unit.ColorProvider
) {
    val hasSignal = rsrp != 0
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(WidgetPalette.plate)
            .cornerRadius(3.dp)
            .padding(8.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = section.title,
                    style = TextStyle(color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                section.band?.let { band ->
                    Text(
                        text = band,
                        style = TextStyle(color = WidgetPalette.ink2, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = section.value,
                style = TextStyle(
                    color = if (hasSignal) ColorProvider(
                        day = getRsrpColor(rsrp, isDark = false),
                        night = getRsrpColor(rsrp, isDark = true)
                    ) else WidgetPalette.ink2,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            section.snr?.let { snr ->
                Text(
                    text = snr,
                    style = TextStyle(
                        color = if (hasSignal) ColorProvider(
                            day = getSinrColor(sinr, isDark = false),
                            night = getSinrColor(sinr, isDark = true)
                        ) else WidgetPalette.ink2,
                        fontSize = 11.sp
                    )
                )
            }
            section.rsrq?.let { rsrq ->
                Text(
                    text = rsrq,
                    style = TextStyle(color = WidgetPalette.ink2, fontSize = 10.sp)
                )
            }
            section.pci?.let { pci ->
                Text(
                    text = pci,
                    style = TextStyle(color = WidgetPalette.ink2, fontSize = 9.sp)
                )
            }
        }
    }
}
