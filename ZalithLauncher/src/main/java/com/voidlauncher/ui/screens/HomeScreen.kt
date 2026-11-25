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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voidlauncher.model.AccountType
import com.voidlauncher.ui.UiState
import com.voidlauncher.ui.VoidViewModel
import com.voidlauncher.ui.components.Avatar
import com.voidlauncher.ui.components.DividerLine
import com.voidlauncher.ui.components.GhostButton
import com.voidlauncher.ui.components.MonoCard
import com.voidlauncher.ui.components.MonoTextField
import com.voidlauncher.ui.components.Pill
import com.voidlauncher.ui.components.PrimaryButton
import com.voidlauncher.ui.components.SectionLabel
import com.voidlauncher.ui.theme.Danger
import com.voidlauncher.ui.theme.Grey100
import com.voidlauncher.ui.theme.Grey400
import com.voidlauncher.ui.theme.Surface3
import com.voidlauncher.ui.theme.White
import com.voidlauncher.util.Net
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun HomeScreen(
    state: UiState,
    vm: VoidViewModel,
    onNavigate: (String) -> Unit,
) {
    val account = state.accounts.firstOrNull { it.id == state.selectedAccountId }
    val installed = state.selectedVersionId?.let { state.installedIds.contains(it) } ?: false
    val engine = state.engines.firstOrNull { it.installed }
    var creating by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(com.voidlauncher.ui.theme.Amoled)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(10.dp))

        // Brand: pure type, no badge, no gradient.
        Text(
            "VOID",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold, letterSpacing = (-2).sp, color = White),
        )
        Text("Beyond the ordinary", style = MaterialTheme.typography.labelSmall,
            color = Grey400, letterSpacing = 2.sp)
        Spacer(Modifier.height(26.dp))

        // ---- Account ----
        SectionLabel("Profile", action = {
            TextButton(onClick = { onNavigate("settings") }) {
                Text("Manage", color = Grey100, style = MaterialTheme.typography.labelSmall)
            }
        })
        if (account == null) {
            MonoCard {
                if (!creating) {
                    Text("No profile yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Create an offline profile — no sign-in required.", color = Grey400,
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    GhostButton("Create offline profile", { creating = true }, Modifier.fillMaxWidth())
                } else {
                    MonoTextField(draft, { draft = it }, "Username")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton("Cancel", { creating = false; draft = "" })
                        PrimaryButton("Create", {
                            vm.createOffline(draft); draft = ""; creating = false
                        }, Modifier.weight(1f))
                    }
                }
            }
        } else {
            MonoCard(onClick = { onNavigate("settings") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(account.username.take(1))
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account.username, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (account.type == AccountType.OFFLINE) "Offline profile"
                            else "Microsoft account",
                            style = MaterialTheme.typography.labelSmall, color = Grey400,
                        )
                    }
                    Pill(if (account.type == AccountType.OFFLINE) "LOCAL" else "MS", filled = true)
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // ---- Version ----
        SectionLabel("Version", action = {
            TextButton(onClick = { onNavigate("versions") }) {
                Text("Browse", color = Grey100, style = MaterialTheme.typography.labelSmall)
            }
        })
        MonoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        state.selectedVersionId ?: "Not selected",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        when {
                            state.selectedVersionId == null -> "Latest: ${state.latestRelease}"
                            installed -> "Game files verified on this device"
                            state.advanced.autoInstallGame -> "Not installed — installs automatically on launch"
                            else -> "Not installed — install it from Versions"
                        },
                        style = MaterialTheme.typography.labelSmall, color = Grey400,
                    )
                }
                if (installed) {
                    Box(
                        Modifier.size(26.dp).background(Surface3, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("✓", color = White) }
                }
            }

            val dl = state.download
            if (dl.active || dl.error != null) {
                Spacer(Modifier.height(12.dp))
                DividerLine()
                Spacer(Modifier.height(12.dp))
                Row {
                    Text(dl.stage.ifBlank { "Installing" }, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    Text("${(dl.percent * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { dl.percent },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = White, trackColor = Surface3,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${dl.filesDone}/${dl.filesTotal} files · ${Net.formatBytes(dl.bytesDone)} / ${Net.formatBytes(dl.bytesTotal)}",
                    style = MaterialTheme.typography.labelSmall, color = Grey400,
                )
                dl.error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = Danger, style = MaterialTheme.typography.labelSmall)
                }
                if (dl.active) {
                    TextButton(onClick = { vm.cancelInstall() }) { Text("Cancel", color = Grey400) }
                } else {
                    TextButton(onClick = { vm.clearDownload() }) { Text("Dismiss", color = Grey400) }
                }
            }
        }

        val jd = state.javaDownload
        if (jd.active || jd.error != null || jd.success) {
            Spacer(Modifier.height(12.dp))
            MonoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(jd.stage.ifBlank { "Runtime" }, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    Text("${(jd.percent * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { jd.percent }, modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = White, trackColor = com.voidlauncher.ui.theme.Surface3,
                )
                if (jd.filesTotal > 0) {
                    Spacer(Modifier.height(5.dp))
                    Text("${jd.filesDone}/${jd.filesTotal} files · ${Net.formatBytes(jd.bytesDone)}",
                        style = MaterialTheme.typography.labelSmall, color = Grey400)
                }
                jd.error?.let {
                    Spacer(Modifier.height(5.dp))
                    Text(it, color = Danger, style = MaterialTheme.typography.labelSmall)
                }
                if (!jd.active) {
                    TextButton(onClick = { vm.clearJavaDownload() }) {
                        Text("Dismiss", color = Grey400)
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ---- Launch ----
        PrimaryButton(
            text = if (installed) "Launch" else "Play",
            onClick = { vm.launchGame() },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.download.active.not() && state.javaDownload.active.not(),
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(6.dp).background(
                if (engine != null) White else Grey400, CircleShape))
            Spacer(Modifier.size(7.dp))
            Text(
                when {
                    state.lastLaunchMessage.isNotBlank() -> state.lastLaunchMessage
                    engine != null -> "${engine.displayName} ready"
                    else -> "Engine required — see Settings → Engine"
                },
                style = MaterialTheme.typography.labelSmall, color = Grey400,
                maxLines = 2,
            )
        }

        Spacer(Modifier.height(30.dp))
        DividerLine()
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatBlock("${state.installedVersions.size}", "Installed")
            StatBlock("${state.accounts.size}", "Profiles")
            StatBlock("${state.mods.size}", "Mods")
            StatBlock("${state.versions.size}", "Versions")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatBlock(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold))
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Grey400)
    }
}
