package com.example.odumonitor.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Two skins, one instrument.
 *
 * Cream is the moulded-plastic body of a bench meter, Charcoal is the same
 * meter under a desk lamp at night. Both share the same token names so every
 * screen is written once.
 */

// Cream: shell, silk-screened plate, inset well, ink, burnt-orange accent.
val CreamShell = Color(0xFFE9E2D4)
val CreamPlate = Color(0xFFF5F0E6)
val CreamInset = Color(0xFFDCD3C2)
val CreamInk = Color(0xFF1E1B16)
val CreamInk2 = Color(0xFF5C5548)
val CreamHairline = Color(0xFFC4BAA7)
val CreamHairlineSoft = Color(0xFFD5CCBA)
val CreamAccent = Color(0xFFB8481F)
val CreamAccentContainer = Color(0xFFF3D9CC)

// Charcoal: warm near-black, never pure grey.
val CharcoalShell = Color(0xFF14120F)
val CharcoalPlate = Color(0xFF1E1B16)
val CharcoalInset = Color(0xFF2A251E)
val CharcoalInk = Color(0xFFEDE6DA)
val CharcoalInk2 = Color(0xFFA79C8A)
val CharcoalHairline = Color(0xFF3A342B)
val CharcoalHairlineSoft = Color(0xFF2F2A23)
val CharcoalAccent = Color(0xFFE8753A)
val CharcoalAccentContainer = Color(0xFF4A2412)

/**
 * Signal semantics. These are the only saturated hues allowed to carry meaning,
 * which is why the accent above is burnt orange and not amber: amber belongs to
 * "fair signal" alone.
 */
val SignalExcellent = Color(0xFF34D399)
val SignalFair = Color(0xFFF59E0B)
val SignalPoor = Color(0xFFEF4444)

val SignalExcellentLight = Color(0xFF0F7A4D)
val SignalFairLight = Color(0xFFB45309)
val SignalPoorLight = Color(0xFFB3261E)

fun signalColor(level: Int, isDark: Boolean): Color = when (level) {
    0 -> if (isDark) SignalExcellent else SignalExcellentLight
    1 -> if (isDark) SignalFair else SignalFairLight
    else -> if (isDark) SignalPoor else SignalPoorLight
}

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
