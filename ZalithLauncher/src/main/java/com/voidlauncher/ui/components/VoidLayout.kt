package com.voidlauncher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** Universal layout frame: phone-width content stays readable on tablets and large screens. */
@Composable
fun VoidContentFrame(
    modifier: Modifier = Modifier,
    content: @Composable Modifier.() -> Unit,
) {
    val widthDp = LocalConfiguration.current.screenWidthDp.dp
    val maxWidth = when {
        widthDp >= 1200.dp -> 900.dp
        widthDp >= 840.dp -> 760.dp
        else -> widthDp
    }
    Box(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
        ) {
            content(Modifier)
        }
    }
}

@Composable
fun isWideLayout(): Boolean = LocalConfiguration.current.screenWidthDp.dp >= 840.dp

@Composable
fun isExtraWideLayout(): Boolean = LocalConfiguration.current.screenWidthDp.dp >= 1200.dp
