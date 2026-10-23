package com.example.odumonitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Material 3 Fallback Light Palette (Telecom Cobalt & Slate)
val MdLightPrimary = Color(0xFF006399)
val MdLightOnPrimary = Color(0xFFFFFFFF)
val MdLightPrimaryContainer = Color(0xFFCDE5FF)
val MdLightOnPrimaryContainer = Color(0xFF001D32)
val MdLightSecondary = Color(0xFF51606F)
val MdLightOnSecondary = Color(0xFFFFFFFF)
val MdLightSecondaryContainer = Color(0xFFD4E4F6)
val MdLightOnSecondaryContainer = Color(0xFF0D1D2A)
val MdLightTertiary = Color(0xFF67587A)
val MdLightOnTertiary = Color(0xFFFFFFFF)
val MdLightTertiaryContainer = Color(0xFFEDDCFF)
val MdLightOnTertiaryContainer = Color(0xFF221533)
val MdLightBackground = Color(0xFFF8F9FF)
val MdLightOnBackground = Color(0xFF181C20)
val MdLightSurface = Color(0xFFF8F9FF)
val MdLightOnSurface = Color(0xFF181C20)
val MdLightSurfaceVariant = Color(0xFFDEE3EB)
val MdLightOnSurfaceVariant = Color(0xFF42474E)
val MdLightOutline = Color(0xFF72777F)
val MdLightOutlineVariant = Color(0xFFC2C7CF)

// Material 3 Fallback Dark Palette (Telecom Sky & Deep Slate)
val MdDarkPrimary = Color(0xFF94CCFF)
val MdDarkOnPrimary = Color(0xFF003352)
val MdDarkPrimaryContainer = Color(0xFF004A75)
val MdDarkOnPrimaryContainer = Color(0xFFCDE5FF)
val MdDarkSecondary = Color(0xFFB8C8DA)
val MdDarkOnSecondary = Color(0xFF233240)
val MdDarkSecondaryContainer = Color(0xFF394857)
val MdDarkOnSecondaryContainer = Color(0xFFD4E4F6)
val MdDarkTertiary = Color(0xFFD2BFE7)
val MdDarkOnTertiary = Color(0xFF382A49)
val MdDarkTertiaryContainer = Color(0xFF4F4061)
val MdDarkOnTertiaryContainer = Color(0xFFEDDCFF)
val MdDarkBackground = Color(0xFF101418)
val MdDarkOnBackground = Color(0xFFE0E2E8)
val MdDarkSurface = Color(0xFF101418)
val MdDarkOnSurface = Color(0xFFE0E2E8)
val MdDarkSurfaceVariant = Color(0xFF42474E)
val MdDarkOnSurfaceVariant = Color(0xFFC2C7CF)
val MdDarkOutline = Color(0xFF8C9199)
val MdDarkOutlineVariant = Color(0xFF42474E)

// Modern Telecom Instrument Palette (Legacy & Dark Reference)
val BgBase = Color(0xFF0A0D12)            // Deep slate canvas
val SurfaceCard = Color(0xFF131720)       // Primary flat card surface
val SurfaceCardSubtle = Color(0xFF1A202C) // Secondary chip / control surface
val BorderSubtle = Color(0xFF262D3D)      // Refined hairline border
val BorderHairline = Color(0x338B949E)

// Text Hierarchy
val TextPrimary = Color(0xFFF1F5F9)       // Slate-50 high contrast text
val TextSecondary = Color(0xFF94A3B8)     // Slate-400 secondary text
val TextMuted = Color(0xFF64748B)         // Slate-500 muted text

// Purposeful Telecom Accent (Precision Sky / Cobalt)
val AccentPrimary = Color(0xFF38BDF8)     // Sky-400 for focused interactions

// Strict Signal Quality Colors (Dark Mode / Default)
val SignalExcellent = Color(0xFF10B981)   // Emerald Green (Dark mode)
val SignalFair = Color(0xFFF59E0B)        // Amber Yellow (Dark mode)
val SignalPoor = Color(0xFFEF4444)        // Coral Red (Dark mode)

// High-Contrast Light Mode Signal Quality Colors (WCAG AA compliant on light surfaces)
val SignalExcellentLight = Color(0xFF047857) // Emerald-700
val SignalFairLight = Color(0xFFB45309)      // Amber-700
val SignalPoorLight = Color(0xFFB91C1C)      // Red-700

// Backward compatibility aliases for Glance widgets & legacy references
val VantablackBg = BgBase
val SurfaceDarkShell = SurfaceCard
val SurfaceDarkCore = SurfaceCardSubtle
val HairlineBorder = BorderHairline
val AccentCyan = AccentPrimary
val AccentPurple = Color(0xFF818CF8)

fun getRsrpColor(rsrp: Int, isDark: Boolean = true): Color = when {
    rsrp >= -80 -> if (isDark) SignalExcellent else SignalExcellentLight
    rsrp >= -100 -> if (isDark) SignalFair else SignalFairLight
    else -> if (isDark) SignalPoor else SignalPoorLight
}

fun getSinrColor(sinr: Float, isDark: Boolean = true): Color = when {
    sinr >= 20f -> if (isDark) SignalExcellent else SignalExcellentLight
    sinr >= 10f -> if (isDark) SignalFair else SignalFairLight
    else -> if (isDark) SignalPoor else SignalPoorLight
}

fun getRsrqColor(rsrq: Int, isDark: Boolean = true): Color = when {
    rsrq >= -10 -> if (isDark) SignalExcellent else SignalExcellentLight
    rsrq >= -15 -> if (isDark) SignalFair else SignalFairLight
    else -> if (isDark) SignalPoor else SignalPoorLight
}

@Composable
fun adaptiveRsrpColor(rsrp: Int): Color = getRsrpColor(rsrp, isSystemInDarkTheme())

@Composable
fun adaptiveSinrColor(sinr: Float): Color = getSinrColor(sinr, isSystemInDarkTheme())

@Composable
fun adaptiveRsrqColor(rsrq: Int): Color = getRsrqColor(rsrq, isSystemInDarkTheme())
