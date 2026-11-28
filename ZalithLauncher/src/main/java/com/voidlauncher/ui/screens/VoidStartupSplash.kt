package com.voidlauncher.ui.screens

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
import androidx.compose.ui.unit.dp
import com.voidlauncher.ui.theme.VoidDesign

/** Lightweight first frame while VOID initialises. Static: no work and no idle animation. */
@Composable
fun VoidStartupSplash(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().background(VoidDesign.Canvas),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VOID", style = VoidDesign.Display)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) {
                    Box(Modifier.size(5.dp).background(VoidDesign.Ink))
                }
            }
        }
    }
}
