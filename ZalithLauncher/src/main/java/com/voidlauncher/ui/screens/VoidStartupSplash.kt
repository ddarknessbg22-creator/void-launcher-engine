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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.voidlauncher.ui.theme.VoidDesign

/** Lightweight first frame while VOID initialises. No work is done here. */
@Composable
fun VoidStartupSplash(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "void-startup")
    val phase = pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "void-startup-pulse",
    ).value

    Box(
        modifier.fillMaxSize().background(VoidDesign.Canvas),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VOID", style = VoidDesign.Display)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    val factor = when (index) { 0 -> 0.3f; 1 -> 0.7f; else -> 1f }
                    val v = (phase * factor).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .size(5.dp)
                            .scale(0.8f + v * 0.4f)
                            .alpha(0.35f + v * 0.65f)
                            .background(VoidDesign.Ink),
                    )
                }
            }
        }
    }
}
