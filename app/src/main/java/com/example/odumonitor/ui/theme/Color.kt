package com.example.odumonitor.ui.theme

import androidx.compose.ui.graphics.Color

// Modern Telecom Instrument Palette (Deep Slate / Graphite)
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

// Strict Signal Quality Colors (Strict threshold tuning - Emerald / Amber / Coral)
val SignalExcellent = Color(0xFF10B981)   // Emerald Green
val SignalFair = Color(0xFFF59E0B)        // Amber Yellow
val SignalPoor = Color(0xFFEF4444)        // Coral Red

// Backward compatibility aliases for Glance widgets & legacy references
val VantablackBg = BgBase
val SurfaceDarkShell = SurfaceCard
val SurfaceDarkCore = SurfaceCardSubtle
val HairlineBorder = BorderHairline
val AccentCyan = AccentPrimary
val AccentPurple = Color(0xFF818CF8)

fun getRsrpColor(rsrp: Int): Color = when {
    rsrp >= -80 -> SignalExcellent
    rsrp >= -100 -> SignalFair
    else -> SignalPoor
}

fun getSinrColor(sinr: Float): Color = when {
    sinr >= 20f -> SignalExcellent
    sinr >= 10f -> SignalFair
    else -> SignalPoor
}

fun getRsrqColor(rsrq: Int): Color = when {
    rsrq >= -10 -> SignalExcellent
    rsrq >= -15 -> SignalFair
    else -> SignalPoor
}
