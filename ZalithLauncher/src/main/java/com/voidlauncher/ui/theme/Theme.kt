package com.voidlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VoidScheme = darkColorScheme(
    primary = VoidDesign.Ink,
    onPrimary = VoidDesign.Canvas,
    primaryContainer = VoidDesign.PanelRaised,
    onPrimaryContainer = VoidDesign.Ink,
    secondary = VoidDesign.InkSoft,
    onSecondary = VoidDesign.Canvas,
    secondaryContainer = VoidDesign.Panel,
    onSecondaryContainer = VoidDesign.InkSoft,
    tertiary = VoidDesign.Ink,
    onTertiary = VoidDesign.Canvas,
    background = VoidDesign.Canvas,
    onBackground = VoidDesign.Ink,
    surface = VoidDesign.Panel,
    onSurface = VoidDesign.Ink,
    surfaceVariant = VoidDesign.PanelRaised,
    onSurfaceVariant = VoidDesign.Muted,
    surfaceTint = VoidDesign.Canvas,
    outline = VoidDesign.Line,
    outlineVariant = VoidDesign.Line,
    error = VoidDesign.Danger,
    onError = VoidDesign.Canvas,
)

@Composable
fun VoidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoidScheme,
        typography = androidx.compose.material3.Typography(
            displayLarge = VoidDesign.Display,
            headlineLarge = VoidDesign.Headline,
            headlineMedium = VoidDesign.Headline,
            titleLarge = VoidDesign.Title,
            titleMedium = VoidDesign.Title,
            bodyLarge = VoidDesign.Body,
            bodyMedium = VoidDesign.Body,
            labelLarge = VoidDesign.Label,
            labelMedium = VoidDesign.Label,
            labelSmall = VoidDesign.Caption,
        ),
        content = content,
    )
}
