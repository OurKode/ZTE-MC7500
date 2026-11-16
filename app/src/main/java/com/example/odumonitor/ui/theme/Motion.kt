package com.example.odumonitor.ui.theme

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * One curve for the whole application. A meter's needle does not ease four
 * different ways depending on which plate it is drawn on, and neither does
 * this interface.
 *
 * Everything here is state change only. Nothing loops, because the dashboard
 * already re-reads the router every three seconds and an ambient animation on
 * top of that is a battery tax with no information in it.
 */
object RetroMotion {
    /** Fast out, settled in. Mechanical, no bounce, no overshoot. */
    val Easing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    const val ENTER_MS = 220
    const val EXIT_MS = 160
    const val SHEET_MS = 240
    const val VALUE_MS = 140
    const val STAGGER_MS = 40
    const val STAGGER_MAX = 5
    const val CHART_REVEAL_MS = 600
    const val ALERT_PULSE_MS = 600

    fun enter(scale: Float) = tween<Float>(durationMillis = (ENTER_MS * scale).toInt(), easing = Easing)
    fun exit(scale: Float) = tween<Float>(durationMillis = (EXIT_MS * scale).toInt(), easing = Easing)
    fun sheet(scale: Float) = tween<Float>(durationMillis = (SHEET_MS * scale).toInt(), easing = Easing)

    /** Generic so the same value-change curve can drive Dp, Color and Float alike. */
    fun <T> value(scale: Float) = tween<T>(durationMillis = (VALUE_MS * scale).toInt(), easing = Easing)
}

/**
 * 0f when the user or the system asked for stillness, 1f otherwise.
 * Android exposes this as the animator duration scale, which is what the
 * "Remove animations" accessibility toggle writes.
 */
@Composable
fun rememberMotionScale(): Float {
    val context = LocalContext.current
    return remember(context) {
        val systemScale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        val batterySaver = runCatching {
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode
        }.getOrDefault(false)
        if (systemScale == 0f || batterySaver) 0f else 1f
    }
}
