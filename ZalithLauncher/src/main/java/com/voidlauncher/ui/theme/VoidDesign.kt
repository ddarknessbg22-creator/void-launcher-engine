package com.voidlauncher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** VOID 2.0 — pure AMOLED monochrome design system. */
object VoidDesign {
    val Canvas = Color(0xFF000000)
    val Ink = Color(0xFFFFFFFF)
    val InkSoft = Color(0xFFE6E6E6)
    val Muted = Color(0xFF8C8C8C)
    val Faint = Color(0xFF555555)
    val Line = Color(0xFF202020)
    val Panel = Color(0xFF0A0A0A)
    val PanelRaised = Color(0xFF111111)
    val Danger = Color(0xFFFF5C5C)

    val Display = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        letterSpacing = (-2).sp,
        color = Ink,
    )
    val Headline = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        letterSpacing = (-0.8).sp,
        color = Ink,
    )
    val Title = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = Ink,
    )
    val Body = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        color = InkSoft,
    )
    val Caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = Muted,
    )
    val Label = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 1.sp,
        color = Muted,
    )
}
