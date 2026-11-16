package com.example.odumonitor.widget

import androidx.glance.color.ColorProvider
import com.example.odumonitor.ui.theme.CharcoalAccent
import com.example.odumonitor.ui.theme.CharcoalHairline
import com.example.odumonitor.ui.theme.CharcoalInk
import com.example.odumonitor.ui.theme.CharcoalInk2
import com.example.odumonitor.ui.theme.CharcoalInset
import com.example.odumonitor.ui.theme.CharcoalPlate
import com.example.odumonitor.ui.theme.CharcoalShell
import com.example.odumonitor.ui.theme.CreamAccent
import com.example.odumonitor.ui.theme.CreamHairline
import com.example.odumonitor.ui.theme.CreamInk
import com.example.odumonitor.ui.theme.CreamInk2
import com.example.odumonitor.ui.theme.CreamInset
import com.example.odumonitor.ui.theme.CreamPlate
import com.example.odumonitor.ui.theme.CreamShell
import com.example.odumonitor.ui.theme.SignalExcellent
import com.example.odumonitor.ui.theme.SignalExcellentLight
import com.example.odumonitor.ui.theme.SignalPoor
import com.example.odumonitor.ui.theme.SignalPoorLight

/**
 * The same two skins as the app, expressed as Glance colour pairs. Glance has no
 * animation and no custom fonts, so the identity here is carried entirely by the
 * palette, the 3dp corners and the flat plates with no elevation.
 */
object WidgetPalette {
    val shell = ColorProvider(day = CreamShell, night = CharcoalShell)
    val plate = ColorProvider(day = CreamPlate, night = CharcoalPlate)
    val inset = ColorProvider(day = CreamInset, night = CharcoalInset)
    val ink = ColorProvider(day = CreamInk, night = CharcoalInk)
    val ink2 = ColorProvider(day = CreamInk2, night = CharcoalInk2)
    val hairline = ColorProvider(day = CreamHairline, night = CharcoalHairline)
    val accent = ColorProvider(day = CreamAccent, night = CharcoalAccent)
    val good = ColorProvider(day = SignalExcellentLight, night = SignalExcellent)
    val bad = ColorProvider(day = SignalPoorLight, night = SignalPoor)
}
