package com.example.odumonitor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.odumonitor.R

/**
 * Barlow: a low-contrast grotesque derived from 1930s California license-plate
 * lettering. It was drawn for legibility at a distance on small stamped plates,
 * which is exactly the job of labels on a field instrument.
 *
 * Numbers use tabular figures everywhere (tnum), so a value ticking from 17.8 to
 * 18.1 does not shift every glyph beside it.
 */
val InstrumentFamily = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold)
)

private val tnum = "tnum"

private fun base(
    size: Int,
    weight: FontWeight,
    lineHeight: Int,
    tracking: Float = 0f,
    tabular: Boolean = false
) = TextStyle(
    fontFamily = InstrumentFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = if (tabular) tnum else null,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

/** Hero reading, the number you walked over to the meter to look at. */
val HeroNumeral = base(46, FontWeight.Bold, 52, tabular = true)

/** Secondary reading, same anatomy one rank down. */
val SubNumeral = base(26, FontWeight.SemiBold, 30, tabular = true)

/** Micro-label above a plate. Uppercase by content, not by transform. */
val PlateLabel = base(10, FontWeight.SemiBold, 12, tracking = 0.9f)

/** Value inside the metric grid. */
val GridValue = base(13, FontWeight.SemiBold, 16, tabular = true)

/** Unit or qualifier riding beside a number. */
val UnitLabel = base(9, FontWeight.Medium, 11, tracking = 0.4f)

val RetroTypography = Typography(
    displayLarge = HeroNumeral,
    displayMedium = base(40, FontWeight.Bold, 44, tabular = true),
    displaySmall = SubNumeral,
    headlineLarge = base(26, FontWeight.SemiBold, 30),
    headlineMedium = base(20, FontWeight.SemiBold, 24),
    headlineSmall = base(17, FontWeight.SemiBold, 21),
    titleLarge = base(16, FontWeight.SemiBold, 20),
    titleMedium = base(13, FontWeight.SemiBold, 17, tabular = true),
    titleSmall = base(12, FontWeight.Medium, 15),
    bodyLarge = base(14, FontWeight.Normal, 19),
    bodyMedium = base(13, FontWeight.Normal, 18),
    bodySmall = base(12, FontWeight.Normal, 16),
    labelLarge = base(12, FontWeight.SemiBold, 15, tracking = 0.5f),
    labelMedium = PlateLabel,
    labelSmall = UnitLabel
)
