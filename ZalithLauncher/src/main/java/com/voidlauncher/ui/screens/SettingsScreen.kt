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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voidlauncher.model.AccountType
import com.voidlauncher.model.FeatureStatus
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
import com.voidlauncher.ui.components.SliderRow
import com.voidlauncher.ui.components.SwitchRow
import com.voidlauncher.ui.theme.Amoled
import com.voidlauncher.ui.theme.Danger
import com.voidlauncher.ui.theme.Grey100
import com.voidlauncher.ui.theme.Grey400
import com.voidlauncher.ui.theme.Hairline
import com.voidlauncher.ui.theme.Surface1
import com.voidlauncher.ui.theme.Surface2
import com.voidlauncher.ui.theme.White

private val TABS = listOf("Account", "Game", "Java", "Network", "Engine", "Storage", "About")
private val MEMORY = listOf(1024, 2048, 3072, 4096, 6144)

@Composable
fun SettingsScreen(state: UiState, vm: VoidViewModel, onMicrosoftLogin: () -> Unit) {
    var tab by remember { mutableStateOf("Account") }
    var accountEdit by remember { mutableStateOf<Pair<String?, String>?>(null) }
    var javaDialog by remember { mutableStateOf(false) }
    var javaPath by remember { mutableStateOf("") }
    var deleteAccount by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(Amoled)) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 20.dp, top = 10.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp))
            Spacer(Modifier.height(12.dp))
        }
        // Compact scrollable tab strip
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(TABS.size) { i ->
                val t = TABS[i]
                val selected = tab == t
                Box(
                    Modifier
                        .background(if (selected) White else Surface2, RoundedCornerShape(8.dp))
                        .clickable { tab = t }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(t, color = if (selected) Amoled else Grey100,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            when (tab) {
                "Account" -> AccountTab(state, vm, onMicrosoftLogin,
                    onAdd = { accountEdit = null to "" },
                    onEdit = { id, n -> accountEdit = id to n },
                    onDelete = { deleteAccount = it })
                "Game" -> GameTab(state, vm)
                "Storage" -> StorageTab(state, vm)
                "Java" -> JavaTab(state, vm, onAdd = { javaDialog = true })
                "Network" -> NetworkTab(state, vm)
                "Engine" -> EngineTab(state, vm)
                "About" -> AboutTab(vm)
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    accountEdit?.let { (id, draft) ->
        var name by remember { mutableStateOf(draft) }
        AlertDialog(
            onDismissRequest = { accountEdit = null },
            containerColor = Surface1,
            title = { Text(if (id == null) "New offline profile" else "Edit profile", color = White) },
            text = { MonoTextField(name, { name = it }, "Username") },
            confirmButton = {
                TextButton(onClick = {
                    if (id == null) vm.createOffline(name) else vm.renameAccount(id, name)
                    accountEdit = null
                }) { Text("Save", color = White) }
            },
            dismissButton = { TextButton(onClick = { accountEdit = null }) { Text("Cancel", color = Grey400) } },
        )
    }

    if (javaDialog) {
        AlertDialog(
            onDismissRequest = { javaDialog = false },
            containerColor = Surface1,
            title = { Text("Add Java runtime", color = White) },
            text = { MonoTextField(javaPath, { javaPath = it }, "Path to bin/java") },
            confirmButton = {
                TextButton(onClick = { vm.addJavaPath(javaPath); javaPath = ""; javaDialog = false }) {
                    Text("Add", color = White)
                }
            },
            dismissButton = { TextButton(onClick = { javaDialog = false }) { Text("Cancel", color = Grey400) } },
        )
    }

    deleteAccount?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteAccount = null },
            containerColor = Surface1,
            title = { Text("Delete profile?", color = White) },
            text = { Text("Local data and stored tokens are removed.", color = Grey400) },
            confirmButton = {
                TextButton(onClick = { vm.deleteAccount(id); deleteAccount = null }) {
                    Text("Delete", color = Danger)
                }
            },
            dismissButton = { TextButton(onClick = { deleteAccount = null }) { Text("Cancel", color = Grey400) } },
        )
    }
}

@Composable
private fun AccountTab(
    state: UiState, vm: VoidViewModel, onMsLogin: () -> Unit,
    onAdd: () -> Unit, onEdit: (String, String) -> Unit, onDelete: (String) -> Unit,
) {
    SectionLabel("Profiles")
    state.accounts.forEach { a ->
        MonoCard(Modifier.padding(bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(a.username.take(1))
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.username, style = MaterialTheme.typography.titleMedium)
                    Text(if (a.type == AccountType.OFFLINE) "Offline" else "Microsoft",
                        style = MaterialTheme.typography.labelSmall, color = Grey400)
                }
                if (a.id == state.selectedAccountId) Pill("ACTIVE", filled = true)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GhostButton("Use", { vm.selectAccount(a.id) })
                GhostButton("Rename", { onEdit(a.id, a.username) })
                if (a.type == AccountType.MICROSOFT) GhostButton("Refresh", { vm.refreshAccountToken(a.id) })
                TextButton(onClick = { onDelete(a.id) }) { Text("Delete", color = Danger) }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    PrimaryButton("New offline profile", onAdd, Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    GhostButton("Sign in with Microsoft", onMsLogin, Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    Text(
        "Offline profiles work without Microsoft. Servers with online-mode still require an authenticated account.",
        style = MaterialTheme.typography.labelSmall, color = Grey400,
    )
}

@Composable
private fun GameTab(state: UiState, vm: VoidViewModel) {
    SectionLabel("Graphics")
    MonoCard {
        OptRow("OpenGL implementation", state.advanced.glImplementation) { vm.setGlImplementation(listOf("MobileGlues", "ANGLE", "GL4ES").next(state.advanced.glImplementation)) }
        DividerLine()
    }
    Spacer(Modifier.height(6.dp))
    Text("MobileGlues is required for Sodium and modern OpenGL mods.",
        style = MaterialTheme.typography.labelSmall, color = Grey400)

    Spacer(Modifier.height(16.dp))
    SectionLabel("Java runtime")
    MonoCard {
        OptRow("Java selection", state.advanced.javaMode) {
            vm.updateAdvanced { a -> a.copy(javaMode = listOf("Auto", "Choose").next(a.javaMode)) }
        }
        DividerLine()
        OptRow("Installed runtimes", "${state.javaRuntimes.size} found") { vm.scanJava() }
        if (state.advanced.javaMode == "Choose" && state.javaRuntimes.isNotEmpty()) {
            DividerLine()
            state.javaRuntimes.forEach { r ->
                OptRow("Java ${r.majorVersion}", if (state.advanced.javaPath == r.path) "✓" else "use") {
                    vm.updateAdvanced { a -> a.copy(javaPath = r.path) }
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text("Java 8/17 runtimes download automatically; Java 21+ comes from the engine.",
        style = MaterialTheme.typography.labelSmall, color = Grey400)

    Spacer(Modifier.height(16.dp))
    SectionLabel("Automation")
    MonoCard {
        SwitchRow("Auto-install game", "Downloads files when you press Play",
            state.advanced.autoInstallGame) { v -> vm.updateAdvanced { it.copy(autoInstallGame = v) } }

        DividerLine()
        SwitchRow("Verify downloads", "SHA-1 check every file",
            state.advanced.verifyDownloads) { v -> vm.updateAdvanced { it.copy(verifyDownloads = v) } }
    }
    Spacer(Modifier.height(16.dp))
    SectionLabel("Memory")
    MonoCard {
        Text("${state.settings.memoryMb} MB allocated", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MEMORY.forEach { m ->
                val selected = state.settings.memoryMb == m
                Box(
                    Modifier.background(if (selected) White else Surface2, RoundedCornerShape(8.dp))
                        .clickable { vm.updateSettings { s -> s.copy(memoryMb = m) } }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text("${m / 1024}G", color = if (selected) Amoled else Grey100,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Text("Allocating more than the device can spare can make Android kill the game.",
        style = MaterialTheme.typography.labelSmall, color = Grey400)
}

@Composable
private fun OptRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = Grey100)
    }
}

/** Cycles to the next option, making every control actually change persisted state. */
private fun List<String>.next(current: String): String {
    val i = indexOf(current)
    return this[(i + 1).mod(size)]
}

@Composable
private fun StorageTab(state: UiState, vm: VoidViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    SectionLabel("Game data")
    MonoCard {
        OptRow("Installed versions", "${state.installedVersions.size}") { vm.verifyInstalls() }
        DividerLine()
        OptRow("Verify all files", "Checks jars and metadata") { vm.verifyInstalls() }
        DividerLine()
        OptRow("Clean partial downloads", "Removes interrupted files") { vm.clearCaches(context) }
        DividerLine()
        OptRow("Export launch plan", "Share JSON config") { vm.shareLaunchPlan(context) }
    }
    Spacer(Modifier.height(10.dp))
    val total = state.installedVersions.sumOf { it.totalBytes }
    Text(
        "Total installed: ${com.voidlauncher.util.Net.formatBytes(total)}",
        style = MaterialTheme.typography.labelSmall, color = Grey400,
    )
}

@Composable
private fun JavaTab(state: UiState, vm: VoidViewModel, onAdd: () -> Unit) {
    SectionLabel("Detected runtimes")
    MonoCard {
        if (state.javaRuntimes.isEmpty()) {
            Text("No runtime detected", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("Engines usually bundle their own runtime.", color = Grey400,
                style = MaterialTheme.typography.labelSmall)
        } else {
            state.javaRuntimes.forEach { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Java ${r.majorVersion}", style = MaterialTheme.typography.bodyMedium)
                        Text(r.path, style = MaterialTheme.typography.labelSmall, color = Grey400, maxLines = 1)
                    }
                    TextButton(onClick = { vm.removeJavaPath(r.path) }) { Text("×", color = Grey400) }
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostButton("Rescan", { vm.scanJava() })
        GhostButton("Add path", onAdd)
    }
}

@Composable
private fun NetworkTab(state: UiState, vm: VoidViewModel) {
    SectionLabel("Downloads")
    MonoCard {
        SwitchRow("Auto-retry failed downloads", "Repeats failed requests up to 3 times",
            state.advanced.autoRetry) { v -> vm.updateAdvanced { a -> a.copy(autoRetry = v) } }
        DividerLine()
        SliderRow("Parallel download threads", state.advanced.downloadThreads, 1..16) {
            vm.updateAdvanced { a -> a.copy(downloadThreads = it) }
        }
    }
    Spacer(Modifier.height(10.dp))
    Text("Downloads come from Mojang official servers. No telemetry, no tracking.",
        style = MaterialTheme.typography.labelSmall, color = Grey400)
}

@Composable
private fun EngineTab(state: UiState, vm: VoidViewModel) {
    SectionLabel("Game engine")
    MonoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Zalith Launcher", style = MaterialTheme.typography.titleMedium)
                Text("Real Minecraft Java engine", style = MaterialTheme.typography.labelSmall, color = Grey400)
            }
            if (state.engines.any { it.displayName == "Zalith Launcher" && it.installed }) {
                Pill("INSTALLED", filled = true)
            } else {
                GhostButton("Get Zalith", { vm.downloadVoidEngine() })
            }
        }
        DividerLine(Modifier.padding(vertical = 10.dp))
        val installed = state.engines.filter { it.installed }
        if (installed.isEmpty()) {
            Text("No compatible engine installed", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("VOID never installs a second launcher. If you already have a compatible engine, press Rescan.", style = MaterialTheme.typography.labelSmall, color = Grey400)
        } else {
            installed.forEach { e ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Pill("INSTALLED", filled = true)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostButton("Rescan", { vm.refreshEngines() })

    }
}

@Composable
private fun AboutTab(vm: VoidViewModel) {
    SectionLabel("Verification status")
    vm.featureStatus().forEach { f ->
        MonoCard(Modifier.padding(bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Pill(
                    when (f.status) {
                        FeatureStatus.VERIFIED -> "VERIFIED"
                        FeatureStatus.BUILT_NOT_DEVICE_VERIFIED -> "BUILT"
                        FeatureStatus.BLOCKED -> "BLOCKED"
                        FeatureStatus.NOT_IMPLEMENTED -> "NOT DONE"
                    },
                    filled = f.status == FeatureStatus.VERIFIED,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(f.description, style = MaterialTheme.typography.labelSmall, color = Grey400)
        }
    }
    Spacer(Modifier.height(10.dp))
    Text("VOID Launcher 1.0.0 · Kotlin · Compose · Material 3",
        style = MaterialTheme.typography.labelSmall, color = Grey400)
}
