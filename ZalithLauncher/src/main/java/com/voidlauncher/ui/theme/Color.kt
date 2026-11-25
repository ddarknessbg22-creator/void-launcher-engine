package com.voidlauncher.ui.theme

import androidx.compose.ui.graphics.Color

/** AMOLED monochrome. Pure black canvas, white as the only accent. */
val Amoled = Color(0xFF000000)
val Surface1 = Color(0xFF080808)
val Surface2 = Color(0xFF0F0F0F)
val Surface3 = Color(0xFF161616)
val White = Color(0xFFFFFFFF)
val Grey100 = Color(0xFFE8E8E8)
val Grey400 = Color(0xFF8A8A8A)
val Grey600 = Color(0xFF5A5A5A)
val Hairline = Color(0xFF1F1F1F)
val Danger = Color(0xFFFF5A5A)
val Success = Color(0xFF6FD48C)

// Legacy aliases kept so older references still resolve during the redesign.
val VoidBg = Amoled
val VoidSecondary = Surface1
val VoidSurface = Surface2
val VoidSurfaceHigh = Surface3
val VoidPurple = White
val VoidCyan = Grey100
val VoidEmerald = White
val VoidText = White
val VoidMuted = Grey400
val VoidDanger = Danger
val VoidWarning = Grey100
val VoidGradient = listOf(White, Grey100)
