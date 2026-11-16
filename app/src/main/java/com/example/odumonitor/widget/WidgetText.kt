package com.example.odumonitor.widget

import android.content.Context
import com.example.odumonitor.R
import com.example.odumonitor.data.local.WidgetConfig
import com.example.odumonitor.data.model.OduSignalState

/**
 * All widget copy, resolved before composition.
 *
 * Glance 1.0.0 composes a widget without Compose UI's `LocalContext`, so calling
 * `stringResource()` inside a widget composable throws and the launcher falls back to
 * its error layout. Every string is looked up here instead, where a real Context
 * exists, and the composables only place what they are handed.
 */
data class WidgetText(
    val status: String,
    val offlineHeadline: String,
    val offlineDetail: String,
    val fiveG: WidgetSectionText?,
    val fourG: WidgetSectionText?
)

data class WidgetSectionText(
    /** "5G NR", used where the band sits on its own line. */
    val title: String,
    /** "5G n78", used where the band shares the label line. */
    val bandedTitle: String,
    /** "n78", or null when the band is unknown or hidden. */
    val band: String?,
    val value: String,
    val snr: String?,
    val rsrq: String?,
    val pci: String?
)

internal fun widgetText(
    context: Context,
    signal: OduSignalState,
    config: WidgetConfig,
    detailed: Boolean
): WidgetText {
    val offline = !signal.isConnected
    return WidgetText(
        status = if (offline) {
            context.getString(R.string.widget_odu_offline)
        } else {
            if (detailed) "${signal.connectionType}  ${signal.provider}" else signal.connectionType
        },
        offlineHeadline = context.getString(
            if (detailed) R.string.widget_link_lost else R.string.widget_odu_unreachable
        ),
        offlineDetail = if (detailed) {
            signal.errorMessage ?: context.getString(R.string.widget_link_lost_detail)
        } else {
            context.getString(R.string.widget_check_link)
        },
        fiveG = if (!config.show5g) {
            null
        } else {
            section(
                context = context,
                config = config,
                titleRes = R.string.widget_5g,
                bandRes = R.string.widget_5g_band,
                band = signal.nrBand,
                rsrp = signal.nrRsrp,
                sinr = signal.nrSinr,
                rsrq = signal.nrRsrq,
                pci = signal.nrPci
            )
        },
        fourG = if (!config.show4g) {
            null
        } else {
            section(
                context = context,
                config = config,
                titleRes = R.string.widget_4g,
                bandRes = R.string.widget_4g_band,
                band = signal.lteBand,
                rsrp = signal.lteRsrp,
                sinr = signal.lteSinr,
                rsrq = signal.lteRsrq,
                pci = signal.ltePci
            )
        }
    )
}

private fun section(
    context: Context,
    config: WidgetConfig,
    titleRes: Int,
    bandRes: Int,
    band: String,
    rsrp: Int,
    sinr: Float,
    rsrq: Int,
    pci: Int
): WidgetSectionText {
    val hasSignal = rsrp != 0
    val standby = context.getString(R.string.widget_standby)
    val showBand = config.showBandInfo && band != "-"

    return WidgetSectionText(
        title = context.getString(titleRes),
        bandedTitle = if (showBand) context.getString(bandRes, band) else context.getString(titleRes),
        band = if (showBand) band else null,
        value = if (hasSignal) "$rsrp dBm" else "-- dBm",
        snr = when {
            !config.showSinrRsrq -> null
            hasSignal -> context.getString(R.string.widget_snr, sinr.toString())
            else -> standby
        },
        rsrq = when {
            !config.showSinrRsrq -> null
            hasSignal -> context.getString(R.string.widget_rsrq, rsrq.toString())
            else -> standby
        },
        pci = when {
            !config.showPciTower -> null
            else -> context.getString(R.string.widget_pci, if (pci > 0) pci.toString() else "-")
        }
    )
}
