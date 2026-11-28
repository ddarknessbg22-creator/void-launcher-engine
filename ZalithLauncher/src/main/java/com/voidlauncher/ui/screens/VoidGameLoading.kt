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

/** VOID game-start overlay. Static by design: no idle animation competes with the game. */
@Composable
fun VoidGameLoading(modifier: Modifier = Modifier) {
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
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(if (index == 1) VoidDesign.Ink else VoidDesign.Muted),
                    )
                }
            }
        }
    }
}
