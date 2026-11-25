package com.voidlauncher.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voidlauncher.ui.UiState
import com.voidlauncher.ui.VoidViewModel
import com.voidlauncher.ui.components.DividerLine
import com.voidlauncher.ui.components.EmptyState
import com.voidlauncher.ui.components.GhostButton
import com.voidlauncher.ui.components.MonoCard
import com.voidlauncher.ui.components.MonoTextField
import com.voidlauncher.ui.components.Pill
import com.voidlauncher.ui.components.PrimaryButton
import com.voidlauncher.ui.components.SectionLabel
import com.voidlauncher.ui.components.SwitchRow
import com.voidlauncher.ui.theme.Amoled
import com.voidlauncher.ui.theme.Danger
import com.voidlauncher.ui.theme.Grey100
import com.voidlauncher.ui.theme.Grey400
import com.voidlauncher.ui.theme.Surface1
import com.voidlauncher.ui.theme.White
import com.voidlauncher.util.Net

@Composable
fun LibraryScreen(state: UiState, vm: VoidViewModel) {
    val context = LocalContext.current
    var newProfile by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<Pair<String, String>?>(null) }

    val modPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.importMod(uri, uri.lastPathSegment?.substringAfterLast('/') ?: "mod.jar")
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Amoled)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Text("Library", style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp))
        Text("Installed versions, profiles and mods", color = Grey400,
            style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(22.dp))

        SectionLabel("Installed", action = { Pill("${state.installedVersions.size}") })
        if (state.installedVersions.isEmpty()) {
            MonoCard { EmptyState("Nothing installed", "Press Play on Home to install automatically") }
        } else {
            state.installedVersions.forEach { v ->
                MonoCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = { vm.selectVersion(v.id) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(v.id, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${v.libraryCount} libraries · ${v.assetCount} assets · ${Net.formatBytes(v.totalBytes)}",
                                style = MaterialTheme.typography.labelSmall, color = Grey400,
                            )
                        }
                        if (v.id == state.selectedVersionId) Pill("ACTIVE", filled = true)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton("Use", { vm.selectVersion(v.id) })
                        GhostButton("Remove", { confirm = "version" to v.id })
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Profiles", action = {
            TextButton(onClick = { newProfile = true }) {
                Text("New", color = Grey100, style = MaterialTheme.typography.labelMedium)
            }
        })
        if (state.profiles.isEmpty()) {
            MonoCard { EmptyState("No saved profiles", "Save an account and version combination") }
        } else {
            state.profiles.forEach { p ->
                MonoCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = { vm.selectProfile(p.id) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(2.dp))
                            Text("${p.versionId} · ${p.memoryMb} MB",
                                style = MaterialTheme.typography.labelSmall, color = Grey400)
                        }
                        if (p.id == state.selectedProfileId) Pill("ACTIVE", filled = true)
                        else TextButton(onClick = { confirm = "profile" to p.id }) {
                            Text("Delete", color = Grey400, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Mods", action = {
            TextButton(onClick = { modPicker.launch("*/*") }) {
                Text("Add jar", color = Grey100, style = MaterialTheme.typography.labelMedium)
            }
        })
        MonoCard {
            if (state.modsVersionId == null) {
                EmptyState("No version selected", "Pick an installed version first")
            } else {
                Text(state.modsVersionId!!, style = MaterialTheme.typography.labelSmall, color = Grey100)
                DividerLine(Modifier.padding(vertical = 10.dp))
                if (state.mods.isEmpty()) {
                    Text("mods folder is empty", style = MaterialTheme.typography.bodyMedium, color = Grey400)
                } else {
                    state.mods.forEach { mod ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(mod.name, style = MaterialTheme.typography.bodyMedium)
                                Text("${mod.loader} · ${Net.formatBytes(mod.sizeBytes)}",
                                    style = MaterialTheme.typography.labelSmall, color = Grey400)
                            }
                            GhostButton(if (mod.enabled) "On" else "Off",
                                { vm.toggleMod(mod.fileName) })
                            Spacer(Modifier.size(6.dp))
                            TextButton(onClick = { confirm = "mod" to mod.fileName }) {
                                Text("×", color = Danger)
                            }
                        }
                        DividerLine(Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        GhostButton("Export launch plan", { vm.shareLaunchPlan(context) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }

    if (newProfile) {
        AlertDialog(
            onDismissRequest = { newProfile = false },
            containerColor = Surface1,
            title = { Text("New profile", color = White) },
            text = {
                Column {
                    MonoTextField(profileName, { profileName = it }, "Name")
                    Spacer(Modifier.height(8.dp))
                    Text("Uses ${state.selectedVersionId ?: "no version"} with the active account.",
                        style = MaterialTheme.typography.labelSmall, color = Grey400)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createProfile(profileName); profileName = ""; newProfile = false
                }) { Text("Save", color = White) }
            },
            dismissButton = { TextButton(onClick = { newProfile = false }) { Text("Cancel", color = Grey400) } },
        )
    }

    confirm?.let { (kind, value) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Surface1,
            title = { Text("Remove $kind?", color = White) },
            text = { Text(value, color = Grey400) },
            confirmButton = {
                TextButton(onClick = {
                    when (kind) {
                        "version" -> vm.removeVersion(value)
                        "profile" -> vm.deleteProfile(value)
                        "mod" -> vm.deleteMod(value)
                    }
                    confirm = null
                }) { Text("Remove", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel", color = Grey400) } },
        )
    }
}
