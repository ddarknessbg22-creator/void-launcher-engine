package com.voidlauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voidlauncher.model.AccountType
import com.voidlauncher.ui.UiState
import com.voidlauncher.ui.VoidViewModel
import com.voidlauncher.ui.components.VoidContentFrame
import com.voidlauncher.ui.components.VoidDivider
import com.voidlauncher.ui.components.VoidPanel
import com.voidlauncher.ui.components.VoidPill
import com.voidlauncher.ui.components.VoidPrimaryButton
import com.voidlauncher.ui.components.VoidSecondaryButton
import com.voidlauncher.ui.components.VoidSectionTitle
import com.voidlauncher.ui.theme.VoidDesign
import com.voidlauncher.util.Net

/** VOID 2.0 Home: command-center layout, no decorative cards, no gradients. */
@Composable
fun HomeScreen(state: UiState, vm: VoidViewModel, onNavigate: (String) -> Unit) {
    val account = state.accounts.firstOrNull { it.id == state.selectedAccountId }
    val installed = state.selectedVersionId?.let(state.installedIds::contains) ?: false
    val download = state.download
    val java = state.javaDownload

    VoidContentFrame {
        Column(
            Modifier
                .fillMaxSize()
                .background(VoidDesign.Canvas)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("VOID", style = VoidDesign.Display)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(VoidDesign.Ink),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    account?.username?.take(1)?.uppercase() ?: "·",
                    color = VoidDesign.Canvas,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text("BEYOND THE ORDINARY", style = VoidDesign.Label)
        Spacer(Modifier.height(26.dp))

        Text("CURRENT SESSION", style = VoidDesign.Label)
        Spacer(Modifier.height(12.dp))

        VoidPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Profile", style = VoidDesign.Caption)
                    Spacer(Modifier.height(3.dp))
                    Text(account?.username ?: "No profile", style = VoidDesign.Title)
                }
                VoidPill(if (account?.type == AccountType.OFFLINE) "Offline" else "Microsoft")
            }
            Spacer(Modifier.height(12.dp))
            VoidDivider()
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Minecraft", style = VoidDesign.Caption)
                    Spacer(Modifier.height(3.dp))
                    Text(state.selectedVersionId ?: "No version selected", style = VoidDesign.Title)
                }
                VoidPill(if (installed) "Ready" else "Not installed", active = installed)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoidSecondaryButton("Profile", { onNavigate("settings") }, Modifier.weight(1f))
                VoidSecondaryButton("Versions", { onNavigate("versions") }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(28.dp))

        PrimaryLaunch(download.active || java.active, installed, state, vm)
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                state.lastLaunchMessage.isNotBlank() -> state.lastLaunchMessage
                installed -> "Ready to start ${state.selectedVersionId}"
                else -> "Select and install a version to play"
            },
            style = VoidDesign.Caption,
            modifier = Modifier.fillMaxWidth(),
        )

        val active = if (download.active) download else if (java.active) java else null
        val finished = if (!download.active && download.error != null) download
        else if (!java.active && java.error != null) java else null

        if (active != null) {
            Spacer(Modifier.height(22.dp))
            VoidSectionTitle("Progress")
            VoidPanel {
                Text(active.stage.ifBlank { "Working" }, style = VoidDesign.Body)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { active.percent },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = VoidDesign.Ink,
                    trackColor = VoidDesign.PanelRaised,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${(active.percent * 100).toInt()}% - ${Net.formatBytes(active.bytesDone)} / ${Net.formatBytes(active.bytesTotal)}",
                    style = VoidDesign.Caption,
                )
                if (active.filesTotal > 0) {
                    Text("${active.filesDone} / ${active.filesTotal} files", style = VoidDesign.Caption)
                }
                Spacer(Modifier.height(10.dp))
                VoidSecondaryButton("Cancel", {
                    if (download.active) vm.cancelInstall() else vm.clearJavaDownload()
                })
            }
        } else if (finished != null) {
            Spacer(Modifier.height(22.dp))
            VoidSectionTitle("Progress")
            VoidPanel {
                Text(finished.stage.ifBlank { "Finished" }, style = VoidDesign.Body)
                finished.error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = VoidDesign.Danger, style = VoidDesign.Caption)
                }
                Spacer(Modifier.height(10.dp))
                VoidSecondaryButton("Dismiss", {
                    vm.clearDownload()
                    vm.clearJavaDownload()
                })
            }
        }

        Spacer(Modifier.height(28.dp))
        VoidDivider()
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric("${state.installedVersions.size}", "INSTALLED")
            Metric("${state.versions.size}", "VERSIONS")
            Metric("${state.mods.size}", "MODS")
        }
        Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrimaryLaunch(busy: Boolean, installed: Boolean, state: UiState, vm: VoidViewModel) {
    val online = false // engine state no longer gates an in-process launch
    VoidPrimaryButton(
        text = if (installed) "Launch Minecraft" else "Install & Play",
        onClick = { vm.launchGame() },
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        if (state.selectedVersionId == null) "Choose a version first"
        else "${state.activeLoader} loader · ${state.mods.size} mods ready",
        style = VoidDesign.Caption,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Metric(value: String, label: String) {
    Column {
        Text(value, style = VoidDesign.Headline)
        Spacer(Modifier.height(2.dp))
        Text(label, style = VoidDesign.Label)
    }
}
