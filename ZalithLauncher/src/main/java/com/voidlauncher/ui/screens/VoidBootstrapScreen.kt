package com.voidlauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.components.InstallableItem
import com.voidlauncher.ui.components.VoidPrimaryButton
import com.voidlauncher.ui.theme.VoidDesign

/**
 * First-run bootstrap: one button installs every missing engine component.
 * Returning users never see this screen because all tasks are already FINISHED.
 */
@Composable
fun VoidBootstrapScreen(
    items: List<InstallableItem>,
    states: Map<String, InstallableItem.State>,
    busy: Boolean,
    onInstall: () -> Unit,
) {
    val done = states.values.count { it == InstallableItem.State.FINISHED }
    val percent = if (items.isEmpty()) 1f else done.toFloat() / items.size

    Box(Modifier.fillMaxSize().background(VoidDesign.Canvas)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
        ) {
            Text("VOID", style = VoidDesign.Display)
            Spacer(Modifier.height(6.dp))
            Text("ONE-TIME SETUP", style = VoidDesign.Label)
            Spacer(Modifier.height(28.dp))
            Text("Preparing Minecraft", style = VoidDesign.Headline)
            Spacer(Modifier.height(8.dp))
            Text(
                "VOID needs a Java runtime and graphics libraries to run real Minecraft. This happens once.",
                style = VoidDesign.Body,
            )
            Spacer(Modifier.height(22.dp))

            LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = VoidDesign.Ink,
                trackColor = VoidDesign.Line,
            )
            Spacer(Modifier.height(6.dp))
            Text("$done of ${items.size} components ready", style = VoidDesign.Caption)
            Spacer(Modifier.height(18.dp))

            items.forEach { item ->
                val state = states[item.name] ?: InstallableItem.State.NOT_STARTED
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        item.name,
                        style = VoidDesign.Body,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        when (state) {
                            InstallableItem.State.FINISHED -> "READY"
                            InstallableItem.State.RUNNING -> "INSTALLING"
                            InstallableItem.State.FAILED -> "RETRY"
                            else -> "NEEDED"
                        },
                        style = VoidDesign.Label,
                    )
                }
                Spacer(Modifier.height(1.dp).background(VoidDesign.Line))
            }

            Spacer(Modifier.height(28.dp))
            VoidPrimaryButton(
                text = if (busy) "Installing" else "Install everything",
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            )
        }
    }
}
