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
import com.example.odumonitor.worker.WidgetUpdateManager

class OduCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OduCompactWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val config = runCatching { WidgetPreferences(context).getWidgetConfig() }
            .getOrDefault(WidgetConfig())
        WidgetUpdateManager.scheduleWidgetUpdates(context, config.updateIntervalMinutes)
    }
}

class OduCompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val config = runCatching { WidgetPreferences(context).getWidgetConfig() }
            .getOrDefault(WidgetConfig())
        val signal = loadWidgetSignal(context)
        val text = widgetText(context, signal, config, detailed = false)
        val launchIntent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            GlanceTheme {
                CompactWidgetContent(signal, text, launchIntent)
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
fun CompactWidgetContent(signal: OduSignalState, text: WidgetText, launchIntent: android.content.Intent) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetPalette.shell)
            .cornerRadius(3.dp)
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
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Box(
                    modifier = GlanceModifier
                        .cornerRadius(2.dp)
                        .padding(6.dp)
                        .clickable(actionRunCallback<RefreshCompactWidgetAction>())
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

            Spacer(modifier = GlanceModifier.height(6.dp))

            if (!signal.isConnected) {
                Text(
                    text = text.offlineHeadline,
                    style = TextStyle(
                        color = WidgetPalette.bad,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = text.offlineDetail,
                    style = TextStyle(color = WidgetPalette.ink2, fontSize = 10.sp),
                    maxLines = 2
                )
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    text.fiveG?.let { fiveG ->
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = fiveG.bandedTitle,
                                style = TextStyle(color = WidgetPalette.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = fiveG.value,
                                style = TextStyle(
                                    color = if (signal.nrRsrp != 0) ColorProvider(
                                        day = getRsrpColor(signal.nrRsrp, isDark = false),
                                        night = getRsrpColor(signal.nrRsrp, isDark = true)
                                    ) else WidgetPalette.ink2,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            fiveG.snr?.let { snr ->
                                Text(
                                    text = snr,
                                    style = TextStyle(color = WidgetPalette.ink2, fontSize = 9.sp)
                                )
                            }
                        }
                    }

                    if (text.fiveG != null && text.fourG != null) {
                        Spacer(modifier = GlanceModifier.width(6.dp))
                    }

                    text.fourG?.let { fourG ->
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = fourG.bandedTitle,
                                style = TextStyle(color = WidgetPalette.ink2, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = fourG.value,
                                style = TextStyle(
                                    color = if (signal.lteRsrp != 0) ColorProvider(
                                        day = getRsrpColor(signal.lteRsrp, isDark = false),
                                        night = getRsrpColor(signal.lteRsrp, isDark = true)
                                    ) else WidgetPalette.ink2,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            fourG.snr?.let { snr ->
                                Text(
                                    text = snr,
                                    style = TextStyle(color = WidgetPalette.ink2, fontSize = 9.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
