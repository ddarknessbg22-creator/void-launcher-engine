package com.voidlauncher.ui.screens

import androidx.compose.foundation.border

import kotlinx.coroutines.launch

import kotlinx.coroutines.delay

import kotlinx.coroutines.SupervisorJob

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.CoroutineScope

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.voidlauncher.model.AccountType
import com.voidlauncher.model.FeatureStatus
import com.voidlauncher.ui.UiState
import com.voidlauncher.ui.VoidViewModel
import com.voidlauncher.ui.components.VoidChoiceRow
import com.voidlauncher.ui.components.VoidContentFrame
import com.voidlauncher.ui.components.VoidDivider
import com.voidlauncher.ui.components.VoidPanel
import com.voidlauncher.ui.components.VoidPill
import com.voidlauncher.ui.components.VoidRow
import com.voidlauncher.ui.components.VoidSecondaryButton
import com.voidlauncher.ui.components.VoidSectionTitle
import com.voidlauncher.ui.components.VoidToggleRow
import com.voidlauncher.ui.theme.VoidDesign

private val TABS = listOf("Account", "Launcher", "Java", "Network", "System", "About")

/** VOID 2.0 Settings: native-settings hierarchy, every row controls real behavior. */
@Composable
fun SettingsScreen(
    state: UiState,
    vm: VoidViewModel,
    onMicrosoftLogin: () -> Unit,
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf("Account") }
    var accountEdit by remember { mutableStateOf<Pair<String?, String>?>(null) }
    var javaPath by remember { mutableStateOf("") }
    var javaDialog by remember { mutableStateOf(false) }
    var deleteAccount by remember { mutableStateOf<String?>(null) }

    VoidContentFrame {
        Column(Modifier.fillMaxSize().background(VoidDesign.Canvas)) {
        Spacer(Modifier.height(20.dp))
        Text(
            "SETTINGS",
            style = VoidDesign.Display,
            modifier = Modifier.padding(horizontal = 22.dp),
        )
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 22.dp)) {
            TABS.chunked(3).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    row.forEach { item ->
                        val selected = item == tab
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (selected) VoidDesign.Ink else VoidDesign.Panel)
                                .clickable { tab = item }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                item.uppercase(),
                                style = VoidDesign.Label.copy(
                                    color = if (selected) VoidDesign.Canvas else VoidDesign.Muted,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            when (tab) {
                "Account" -> AccountSettings(state, vm, onMicrosoftLogin,
                    onAdd = { accountEdit = null to "" },
                    onEdit = { id, name -> accountEdit = id to name },
                    onDelete = { deleteAccount = it })
                "Launcher" -> LauncherSettings(state, vm, context)
                "Java" -> JavaSettings(state, vm,
                    onAdd = { javaDialog = true })
                "Network" -> NetworkSettings(state, vm)
                "System" -> SystemSettings(state, vm, context)
                "About" -> AboutSettings(vm)
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    accountEdit?.let { (id, draft) ->
        var name by remember { mutableStateOf(draft) }
        AlertDialog(
            onDismissRequest = { accountEdit = null },
            containerColor = VoidDesign.Panel,
            title = { Text(if (id == null) "New offline profile" else "Edit profile", style = VoidDesign.Title) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Username", style = VoidDesign.Caption) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (id == null) vm.createOffline(name) else vm.renameAccount(id, name)
                    accountEdit = null
                }) { Text("Save", color = VoidDesign.Ink) }
            },
            dismissButton = { TextButton(onClick = { accountEdit = null }) { Text("Cancel", color = VoidDesign.Muted) } },
        )
    }

    if (javaDialog) {
        AlertDialog(
            onDismissRequest = { javaDialog = false },
            containerColor = VoidDesign.Panel,
            title = { Text("Add Java runtime", style = VoidDesign.Title) },
            text = {
                OutlinedTextField(
                    value = javaPath,
                    onValueChange = { javaPath = it },
                    label = { Text("Path to bin/java", style = VoidDesign.Caption) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addJavaPath(javaPath)
                    javaPath = ""
                    javaDialog = false
                }) { Text("Add", color = VoidDesign.Ink) }
            },
            dismissButton = { TextButton(onClick = { javaDialog = false }) { Text("Cancel", color = VoidDesign.Muted) } },
        )
    }

    deleteAccount?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteAccount = null },
            containerColor = VoidDesign.Panel,
            title = { Text("Delete profile?", style = VoidDesign.Title) },
            text = { Text("Local data and stored tokens are removed.", style = VoidDesign.Caption) },
            confirmButton = {
                TextButton(onClick = { vm.deleteAccount(id); deleteAccount = null }) {
                    Text("Delete", color = VoidDesign.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { deleteAccount = null }) { Text("Cancel", color = VoidDesign.Muted) } },
        )
    }
}

    }

@Composable
private fun AccountSettings(
    state: UiState,
    vm: VoidViewModel,
    onMsLogin: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    VoidSectionTitle("Profiles")
    state.accounts.forEach { account ->
        VoidPanel(Modifier.padding(bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(account.username, style = VoidDesign.Title)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (account.type == AccountType.OFFLINE) "Offline profile" else "Microsoft account",
                        style = VoidDesign.Caption,
                    )
                }
                if (account.id == state.selectedAccountId) VoidPill("Active", active = true)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoidSecondaryButton("Use", { vm.selectAccount(account.id) }, Modifier.weight(1f))
                VoidSecondaryButton("Edit", { onEdit(account.id, account.username) }, Modifier.weight(1f))
                if (account.type == AccountType.MICROSOFT) {
                    VoidSecondaryButton("Refresh", { vm.refreshAccountToken(account.id) }, Modifier.weight(1f))
                }
                VoidSecondaryButton("Del", { onDelete(account.id) }, Modifier.weight(1f))
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    VoidSecondaryButton("Create offline profile", onAdd, Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    VoidSecondaryButton("Microsoft sign-in", onMsLogin, Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    Text("Offline profiles work without Microsoft. Online servers require an authenticated account.", style = VoidDesign.Caption)
}

@Composable
private fun LauncherSettings(state: UiState, vm: VoidViewModel, context: android.content.Context) {
    VoidSectionTitle("Automation")
    VoidPanel {
        VoidToggleRow(
            label = "Auto-install game",
            detail = "Game files download when you press Play",
            checked = state.advanced.autoInstallGame,
            onChange = { value -> vm.updateAdvanced { it.copy(autoInstallGame = value) } },
        )
        VoidDivider()
        VoidRow("JVM arguments", if (state.settings.jvmArgs.isBlank()) "None" else state.settings.jvmArgs)
    }
}

@Composable
private fun JavaSettings(state: UiState, vm: VoidViewModel, onAdd: () -> Unit) {
    VoidSectionTitle("Java")
    VoidPanel {
        VoidRow("Detected runtimes", "${state.javaRuntimes.size} found")
        state.javaRuntimes.forEach { runtime ->
            VoidRow("Java ${runtime.majorVersion}", runtime.version, onClick = {
                vm.updateAdvanced { it.copy(javaPath = runtime.path, javaMode = "Choose") }
            })
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoidSecondaryButton("Scan", { vm.scanJava() }, Modifier.weight(1f))
            VoidSecondaryButton("Add path", onAdd, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NetworkSettings(state: UiState, vm: VoidViewModel) {
    VoidSectionTitle("Downloads")
    VoidPanel {
        VoidToggleRow(
            label = "Auto-retry",
            detail = "Retry failed downloads",
            checked = state.advanced.autoRetry,
            onChange = { value -> vm.updateAdvanced { it.copy(autoRetry = value) } },
        )
        VoidDivider()
        VoidToggleRow(
            label = "Verify files",
            detail = "SHA-1 check every download",
            checked = state.advanced.verifyDownloads,
            onChange = { value -> vm.updateAdvanced { it.copy(verifyDownloads = value) } },
        )
        VoidDivider()
        VoidChoiceRow(
            label = "Parallel downloads",
            value = "${state.advanced.downloadThreads} at once",
            detail = "Higher is faster on good Wi-Fi; 4 is a safe default",
            options = listOf("1", "2", "4", "6", "8", "12", "16"),
            onSelect = { picked ->
                vm.updateAdvanced { a -> a.copy(downloadThreads = picked.toInt()) }
                vm.notify("Parallel downloads set to $picked")
            },
        )
    }
}

@Composable
private fun SystemSettings(state: UiState, vm: VoidViewModel, context: android.content.Context) {
    val renderers = remember { com.voidlauncher.engine.VoidRenderers.options() }
    val current = remember(state.preferredEngine) {
        com.voidlauncher.engine.VoidRenderers.current()
    }
    var jvmDialog by remember { mutableStateOf(false) }
    var jvmText by remember { mutableStateOf(state.settings.jvmArgs) }

    VoidSectionTitle("Renderer")
    VoidPanel {
        val mobileGlues = renderers.firstOrNull { it.isMobileGlues }
        VoidRow(
            "MobileGlues",
            when {
                mobileGlues == null -> "Not in this engine"
                mobileGlues.available -> "Available"
                else -> "Present / files missing"
            },
        )
        renderers.forEach { renderer ->
            VoidRow(
                label = renderer.name
                    .replace("Renderer", "")
                    .replace("Krypton Wrapper", "GL4ES (Krypton)")
                    .replace("Freedreno (Adreno)", "Freedreno")
                    .replace("Panfrost (Mali)", "Panfrost"),
                value = when {
                    renderer.uniqueId == current -> "ACTIVE"
                    renderer.available -> "Use"
                    else -> "Unavailable"
                },
                onClick = { if (renderer.available) vm.setRenderer(renderer.uniqueId) },
            )
            VoidDivider()
        }
    }

    Spacer(Modifier.height(18.dp))
    VoidSectionTitle("About")
    VoidPanel(Modifier.padding(bottom = 16.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(com.movtery.zalithlauncher.R.drawable.elite_pfp),
                contentDescription = "Elite",
                modifier = Modifier
                    .size(96.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .border(1.dp, VoidDesign.Line, androidx.compose.foundation.shape.CircleShape),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text("MADE BY ELITE", style = VoidDesign.Headline)
        Spacer(Modifier.height(4.dp))
        Text("Design, engine integration and everything you see.", style = VoidDesign.Caption)
        Spacer(Modifier.height(16.dp))
        VoidDivider()
        Spacer(Modifier.height(12.dp))
        Text("VOID Launcher", style = VoidDesign.Title)
        Spacer(Modifier.height(3.dp))
        Text(
            "A single Android app for real Minecraft: Java Edition. Kotlin, Jetpack Compose and a native engine.",
            style = VoidDesign.Caption,
        )
        Spacer(Modifier.height(12.dp))
        Text("Version 1.0.0 · GPL-3.0 engine", style = VoidDesign.Caption)
    }

    VoidSectionTitle("Credits")
    VoidPanel(Modifier.padding(bottom = 8.dp)) {
        VoidRow("Engine base", "Zalith Launcher 2 (GPL-3.0)")
        VoidDivider()
        VoidRow("Runtime", "Android OpenJDK builds")
        VoidDivider()
        VoidRow("Renderer", "MobileGlues / GL4ES / Zink")
        VoidDivider()
        VoidRow("Mod platforms", "CurseForge + Modrinth")
    }
}
