package com.voidlauncher.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.voidlauncher.ui.theme.VoidDesign

/**
 * VOID game-start overlay. Three restrained pulses make it obvious the app is alive.
 * Removed by VMActivity the moment the engine reports its first rendered frame.
 */
@Composable
fun VoidGameLoading(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "void-loading")
    val phase = pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "void-pulse",
    ).value

    Box(
        modifier
            .fillMaxSize()
            .background(VoidDesign.Canvas),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VOID", style = VoidDesign.Display)
            Spacer(Modifier.height(6.dp))
            Text("STARTING MINECRAFT", style = VoidDesign.Label)
            Spacer(Modifier.height(28.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { index ->
                    val delayFactor = when (index) { 0 -> 0.25f; 1 -> 0.65f; else -> 1f }
                    val value = (phase * delayFactor).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .size(6.dp)
                            .scale(0.8f + value * 0.5f)
                            .alpha(0.35f + value * 0.65f)
                            .background(if (index == 1) VoidDesign.Ink else VoidDesign.Muted),
                    )
                }
            }
            Spacer(Modifier.width(1.dp))
        }
    }
}
