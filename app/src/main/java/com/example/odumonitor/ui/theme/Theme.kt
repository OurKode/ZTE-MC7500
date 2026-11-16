package com.example.odumonitor.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.odumonitor.data.local.AppearanceConfig
import com.example.odumonitor.data.local.RetroSkin

/**
 * Cream: warm paper, ink, one burnt-orange accent.
 * Every M3 role is mapped deliberately so screens written against
 * MaterialTheme.colorScheme inherit the instrument look for free.
 */
private val CreamScheme = lightColorScheme(
    primary = CreamAccent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = CreamAccentContainer,
    onPrimaryContainer = Color(0xFF3A1206),
    secondary = CreamInk2,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2DACA),
    onSecondaryContainer = CreamInk,
    tertiary = Color(0xFF3F5D52),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3E3DA),
    onTertiaryContainer = Color(0xFF10241C),
    background = CreamShell,
    onBackground = CreamInk,
    surface = CreamPlate,
    onSurface = CreamInk,
    surfaceVariant = CreamInset,
    onSurfaceVariant = CreamInk2,
    surfaceContainerLowest = Color(0xFFFBF8F1),
    surfaceContainerLow = Color(0xFFF5F0E6),
    surfaceContainer = Color(0xFFEFE9DC),
    surfaceContainerHigh = Color(0xFFE4DCCC),
    surfaceContainerHighest = Color(0xFFDCD3C2),
    outline = CreamHairline,
    outlineVariant = CreamHairlineSoft,
    error = SignalPoorLight,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF5D6D3),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = CreamInk,
    inverseOnSurface = CreamPlate
)

/**
 * Charcoal: the same meter at night. Warm blacks, never grey, because a cold
 * grey night mode next to a cream day mode reads as two different products.
 */
private val CharcoalScheme = darkColorScheme(
    primary = CharcoalAccent,
    onPrimary = Color(0xFF2A1408),
    primaryContainer = CharcoalAccentContainer,
    onPrimaryContainer = Color(0xFFF7D8C6),
    secondary = CharcoalInk2,
    onSecondary = Color(0xFF1A1610),
    secondaryContainer = Color(0xFF332D24),
    onSecondaryContainer = Color(0xFFE7DFD1),
    tertiary = Color(0xFF8FA79A),
    onTertiary = Color(0xFF12251C),
    tertiaryContainer = Color(0xFF2A4136),
    onTertiaryContainer = Color(0xFFD3E3DA),
    background = CharcoalShell,
    onBackground = CharcoalInk,
    surface = CharcoalPlate,
    onSurface = CharcoalInk,
    surfaceVariant = CharcoalInset,
    onSurfaceVariant = CharcoalInk2,
    surfaceContainerLowest = Color(0xFF0E0C0A),
    surfaceContainerLow = Color(0xFF1A1712),
    surfaceContainer = Color(0xFF221E18),
    surfaceContainerHigh = Color(0xFF2A251E),
    surfaceContainerHighest = Color(0xFF332D24),
    outline = CharcoalHairline,
    outlineVariant = CharcoalHairlineSoft,
    error = SignalPoor,
    onError = Color(0xFF2A0A08),
    errorContainer = Color(0xFF4A1512),
    onErrorContainer = Color(0xFFF7D5D2),
    inverseSurface = CharcoalInk,
    inverseOnSurface = CharcoalShell
)

/**
 * Instrument corners are machined, not soft. Nothing here is a pill: a stamped
 * plate has a 3dp break at the edge and a dialog is a slightly larger panel,
 * not a bubble.
 */
private val InstrumentShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(3.dp),
    large = RoundedCornerShape(3.dp),
    extraLarge = RoundedCornerShape(3.dp)
)

val LocalRetroSkin = staticCompositionLocalOf { RetroSkin.CREAM }
val LocalMotionScale = staticCompositionLocalOf { 1f }

@Composable
fun OduMonitorTheme(
    appearanceConfig: AppearanceConfig = AppearanceConfig(),
    darkTheme: Boolean = appearanceConfig.skin == RetroSkin.CHARCOAL,
    content: @Composable () -> Unit
) {
    val motionScale = rememberMotionScale()
    val colorScheme = if (darkTheme) CharcoalScheme else CreamScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalRetroSkin provides appearanceConfig.skin,
        LocalMotionScale provides motionScale
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RetroTypography,
            shapes = InstrumentShapes,
            content = content
        )
    }
}
