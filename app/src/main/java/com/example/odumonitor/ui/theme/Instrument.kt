package com.example.odumonitor.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.odumonitor.data.local.RetroSkin

/**
 * The embossed edge of a stamped plate: a lit hairline along the top, a recessed
 * one along the bottom. This replaces elevation entirely, because a bench
 * instrument has no floating cards, it has plates screwed flat to a body.
 */
fun Modifier.plateBevel(corner: Dp = 3.dp, night: Boolean = false): Modifier = drawBehind {
    val highlight = if (night) Color(0x14FFFFFF) else Color(0x8AFFFFFF)
    val shadow = if (night) Color(0x59000000) else Color(0x1A000000)
    val inset = corner.toPx() + 1.dp.toPx()
    val stroke = 1.dp.toPx()
    drawLine(
        color = highlight,
        start = Offset(inset, stroke / 2f),
        end = Offset(size.width - inset, stroke / 2f),
        strokeWidth = stroke
    )
    drawLine(
        color = shadow,
        start = Offset(inset, size.height - stroke / 2f),
        end = Offset(size.width - inset, size.height - stroke / 2f),
        strokeWidth = stroke
    )
}

/**
 * A plate: flat face, hairline edge, bevel. No shadow, no gradient, no glass.
 */
@Composable
fun InstrumentPlate(
    modifier: Modifier = Modifier,
    corner: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(corner)
    val night = LocalRetroSkin.current == RetroSkin.CHARCOAL
    Box(
        modifier = modifier
            .clip(shape)
            .background(color, shape)
            .border(1.dp, borderColor, shape)
            .plateBevel(corner, night)
    ) {
        content()
    }
}

/** Uppercase micro-label silk-screened above a value. */
@Composable
fun PlateLabelText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        modifier = modifier,
        style = PlateLabel,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun InstrumentDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

/** The three-state indicator, as an LED rather than a coloured word. */
@Composable
fun SignalLed(level: Int, modifier: Modifier = Modifier, size: Dp = 7.dp) {
    val night = LocalRetroSkin.current == RetroSkin.CHARCOAL
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(signalColor(level, night))
    )
}

/**
 * Real seven-segment geometry, drawn rather than typed.
 *
 * A segmented font would need a glyph table nobody can verify at runtime and
 * fails silently into tofu on a missing character. Drawing the segments means
 * the digits are always correct, always the same weight, and scale to any size.
 *
 * Only these characters are defined: 0-9 . : -  space. Callers pass the numeral
 * alone and set the unit with normal type, which is also how a real meter is
 * silkscreened: digits in the window, unit on the plate.
 */
@Composable
fun SevenSegmentReadout(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    digitHeight: Dp = 34.dp,
    showGhostSegments: Boolean = true
) {
    val night = LocalRetroSkin.current == RetroSkin.CHARCOAL
    // An unlit segment is a hole in the light, not a grey tile. On the cream skin
    // anything above 4 percent reads as a box outline around the digit, which is
    // what made "1" look like "|1" on a real screen.
    val ghost = color.copy(alpha = if (night) 0.06f else 0.035f)
    val width = digitHeight * (text.length.coerceAtLeast(1) * 0.68f)

    androidx.compose.foundation.Canvas(modifier = modifier.width(width).height(digitHeight)) {
        val h = size.height
        val cell = size.width / text.length.coerceAtLeast(1)
        val digitW = cell * 0.72f
        val thickness = (h * 0.11f).coerceAtLeast(1.4.dp.toPx())
        val pad = (cell - digitW) / 2f

        text.forEachIndexed { index, ch ->
            val ox = index * cell + pad
            when (ch) {
                '.' -> drawCircle(
                    color = color,
                    radius = thickness * 0.62f,
                    center = Offset(ox + digitW * 0.85f, h - thickness * 0.6f)
                )
                ':' -> {
                    drawCircle(color, thickness * 0.55f, Offset(ox + digitW * 0.5f, h * 0.34f))
                    drawCircle(color, thickness * 0.55f, Offset(ox + digitW * 0.5f, h * 0.68f))
                }
                '-' -> drawSegment(ox, h, digitW, thickness, SEG_G, color)
                ' ' -> Unit
                else -> {
                    val mask = digitMask(ch) ?: return@forEachIndexed
                    for (bit in 0 until 7) {
                        val on = mask and (1 shl bit) != 0
                        if (on) drawSegment(ox, h, digitW, thickness, bit, color)
                        else if (showGhostSegments) drawSegment(ox, h, digitW, thickness, bit, ghost)
                    }
                }
            }
        }
    }
}

// Segment indices following the usual labelling: A top, B top-right, C bottom-right,
// D bottom, E bottom-left, F top-left, G middle.
private const val SEG_A = 0
private const val SEG_B = 1
private const val SEG_C = 2
private const val SEG_D = 3
private const val SEG_E = 4
private const val SEG_F = 5
private const val SEG_G = 6

private fun digitMask(ch: Char): Int? = when (ch) {
    '0' -> 0b0111111
    '1' -> 0b0000110
    '2' -> 0b1011011
    '3' -> 0b1001111
    '4' -> 0b1100110
    '5' -> 0b1101101
    '6' -> 0b1111101
    '7' -> 0b0000111
    '8' -> 0b1111111
    '9' -> 0b1101111
    else -> null
}

private fun DrawScope.drawSegment(
    x: Float,
    h: Float,
    w: Float,
    thickness: Float,
    bit: Int,
    color: Color
) {
    val cap = StrokeCap.Round
    val inset = thickness * 0.55f
    when (bit) {
        SEG_A -> drawLine(color, Offset(x + inset, thickness / 2f), Offset(x + w - inset, thickness / 2f), thickness, cap)
        SEG_D -> drawLine(color, Offset(x + inset, h - thickness / 2f), Offset(x + w - inset, h - thickness / 2f), thickness, cap)
        SEG_G -> drawLine(color, Offset(x + inset, h / 2f), Offset(x + w - inset, h / 2f), thickness, cap)
        SEG_F -> drawLine(color, Offset(x + thickness / 2f, inset), Offset(x + thickness / 2f, h / 2f - inset), thickness, cap)
        SEG_B -> drawLine(color, Offset(x + w - thickness / 2f, inset), Offset(x + w - thickness / 2f, h / 2f - inset), thickness, cap)
        SEG_E -> drawLine(color, Offset(x + thickness / 2f, h / 2f + inset), Offset(x + thickness / 2f, h - inset), thickness, cap)
        SEG_C -> drawLine(color, Offset(x + w - thickness / 2f, h / 2f + inset), Offset(x + w - thickness / 2f, h - inset), thickness, cap)
    }
}

/**
 * Wordmark: three ascending bars plus the model name. The bars are the app's
 * core datum drawn as a mark, not a decorative glyph.
 */
@Composable
fun InstrumentWordmark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    barHeight: Dp = 18.dp
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.width(barHeight * 1.15f).height(barHeight)
        ) {
            val gap = size.width * 0.16f
            val barW = (size.width - gap * 2) / 3f
            val heights = listOf(0.45f, 0.7f, 1f)
            heights.forEachIndexed { i, frac ->
                val bh = size.height * frac
                drawRect(
                    color = color,
                    topLeft = Offset(i * (barW + gap), size.height - bh),
                    size = Size(barW, bh)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "ZTE MC7500",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.6.sp
        )
    }
}

/** Scrollable-free tag row used for band chips. */
@Composable
fun InstrumentTag(text: String, modifier: Modifier = Modifier, selected: Boolean = false) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape
            )
            .height(22.dp)
            // Hugs its text. A full-width child here used to stretch the tag
            // across the plate and push the refresh control off its own edge.
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = UnitLabel,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * A rocker switch, not a Material toggle. Square track, square thumb, 3dp
 * corners, and the same motion curve as everything else. Material's switch is
 * two perfect pills with a floating shadow, which is the one shape this
 * interface is built to avoid.
 */
@Composable
fun InstrumentToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val motion = LocalMotionScale.current
    val trackW = 42.dp
    val trackH = 24.dp
    val inset = 3.dp
    val thumb = trackH - inset * 2
    val travel = trackW - inset * 2 - thumb

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) travel else 0.dp,
        animationSpec = RetroMotion.value(motion),
        label = "toggle-thumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = RetroMotion.value(motion),
        label = "toggle-track"
    )

    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .size(trackW, trackH)
            .clip(shape)
            .background(trackColor, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(inset)
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(thumb)
                .clip(RoundedCornerShape(2.dp))
                .background(if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface)
        )
    }
}

/**
 * The tab selector as a switch bank: flat segments in one plate, hairline
 * dividers, and a lit rule over the selected one. No pill, no sliding thumb.
 */
@Composable
fun InstrumentTabStrip(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    InstrumentPlate(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            items.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 44.dp)
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.uppercase(),
                        style = UnitLabel,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Underline, matching the chart-range selector on the dashboard.
                    if (selected) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                if (index < items.lastIndex) {
                    Box(
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }
    }
}

/** Orange silk-screened group caption, matching the plate labels on the dashboard. */
@Composable
fun InstrumentSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = PlateLabel,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Selectable band/mode chip. Material's FilterChip is a pill with a leading
 * tick; a band selector is a row of stamped keys that either are or are not
 * pressed, so the selected one simply takes the accent fill.
 */
@Composable
fun InstrumentChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(3.dp)
    val fill = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        selected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        selected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(fill, shape)
            .border(
                1.dp,
                if (selected && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = UnitLabel,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
