package com.voidlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MonoScheme = darkColorScheme(
    primary = White,
    onPrimary = Amoled,
    primaryContainer = Surface3,
    onPrimaryContainer = White,
    secondary = Grey100,
    onSecondary = Amoled,
    secondaryContainer = Surface2,
    onSecondaryContainer = Grey100,
    tertiary = White,
    onTertiary = Amoled,
    background = Amoled,
    onBackground = White,
    surface = Amoled,
    onSurface = White,
    surfaceVariant = Surface2,
    onSurfaceVariant = Grey400,
    surfaceTint = Amoled,
    outline = Hairline,
    outlineVariant = Hairline,
    error = Danger,
    onError = Amoled,
)

@Composable
fun VoidTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MonoScheme, typography = VoidTypography, content = content)
}
