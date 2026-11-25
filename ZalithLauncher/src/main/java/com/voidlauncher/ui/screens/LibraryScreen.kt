package com.voidlauncher.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.voidlauncher.ui.UiState
import com.voidlauncher.ui.VoidViewModel
import com.voidlauncher.ui.components.VoidDivider
import com.voidlauncher.ui.components.VoidPanel
import com.voidlauncher.ui.components.VoidPill
import com.voidlauncher.ui.components.VoidPrimaryButton
import com.voidlauncher.ui.components.VoidSecondaryButton
import com.voidlauncher.ui.components.VoidSectionTitle
import com.voidlauncher.ui.theme.VoidDesign
import com.voidlauncher.util.Net

/** VOID 2.0 Library: inventory first, actions inline, no stacked cards. */
@Composable
fun LibraryScreen(state: UiState, vm: VoidViewModel) {
    val context = LocalContext.current
    var profileName by remember { mutableStateOf("") }
    var newProfile by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<Pair<String, String>?>(null) }

    val modPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.importMod(uri, uri.lastPathSegment?.substringAfterLast('/') ?: "mod.jar")
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(VoidDesign.Canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("LIBRARY", style = VoidDesign.Display)
        Spacer(Modifier.height(2.dp))
        Text("Installed game, profiles and mods", style = VoidDesign.Caption)
        Spacer(Modifier.height(26.dp))

        VoidSectionTitle("Installed versions", trailing = { Text("${state.installedVersions.size}", style = VoidDesign.Label) })
        if (state.installedVersions.isEmpty()) {
            VoidPanel {
                Text("Nothing installed", style = VoidDesign.Title)
                Spacer(Modifier.height(4.dp))
                Text("Press Play on Home to install a real version.", style = VoidDesign.Caption)
            }
        } else {
            state.installedVersions.forEach { version ->
                VoidPanel(modifier = Modifier.padding(bottom = 8.dp), onClick = { vm.selectVersion(version.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(version.id, style = VoidDesign.Title)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "${version.libraryCount} libraries · ${version.assetCount} assets · ${Net.formatBytes(version.totalBytes)}",
                                style = VoidDesign.Caption,
                            )
                        }
                        if (version.id == state.selectedVersionId) VoidPill("Active", active = true)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoidSecondaryButton("Use", { vm.selectVersion(version.id) }, Modifier.weight(1f))
                        VoidSecondaryButton("Remove", { confirm = "version" to version.id }, Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        VoidSectionTitle("Profiles", trailing = {
            Text("NEW", style = VoidDesign.Label.copy(color = VoidDesign.Ink), modifier = Modifier.clickable { newProfile = true })
        })
        if (state.profiles.isEmpty()) {
            VoidPanel {
                Text("No profiles", style = VoidDesign.Title)
                Spacer(Modifier.height(4.dp))
                Text("Save an account and version combination.", style = VoidDesign.Caption)
            }
        } else {
            state.profiles.forEach { profile ->
                VoidPanel(modifier = Modifier.padding(bottom = 8.dp), onClick = { vm.selectProfile(profile.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(profile.name, style = VoidDesign.Title)
                            Spacer(Modifier.height(3.dp))
                            Text("${profile.versionId} · ${profile.memoryMb} MB", style = VoidDesign.Caption)
                        }
                        if (profile.id == state.selectedProfileId) VoidPill("Active", active = true)
                        else Text("REMOVE", style = VoidDesign.Label, modifier = Modifier.clickable { confirm = "profile" to profile.id })
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        VoidSectionTitle("Mods", trailing = {
            Text("ADD JAR", style = VoidDesign.Label.copy(color = VoidDesign.Ink), modifier = Modifier.clickable { modPicker.launch("*/*") })
        })
        VoidPanel {
            Text(state.modsVersionId ?: "No version selected", style = VoidDesign.Body)
            VoidDivider(Modifier.padding(vertical = 10.dp))
            if (state.mods.isEmpty()) {
                Text("mods folder is empty", style = VoidDesign.Caption)
            } else {
                state.mods.forEach { mod ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(mod.name, style = VoidDesign.Body)
                            Text("${mod.loader} · ${Net.formatBytes(mod.sizeBytes)}", style = VoidDesign.Caption)
                        }
                        VoidPill(if (mod.enabled) "On" else "Off", active = mod.enabled)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "TOGGLE",
                            style = VoidDesign.Label,
                            modifier = Modifier.clickable { vm.toggleMod(mod.fileName) },
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("×", color = VoidDesign.Danger, modifier = Modifier.clickable { confirm = "mod" to mod.fileName })
                    }
                    VoidDivider(Modifier.padding(vertical = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        VoidSecondaryButton("Export launch plan", { vm.shareLaunchPlan(context) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(28.dp))
    }

    if (newProfile) {
        AlertDialog(
            onDismissRequest = { newProfile = false },
            containerColor = VoidDesign.Panel,
            title = { Text("New profile", style = VoidDesign.Title) },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Name", style = VoidDesign.Caption) },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Uses the active account and ${state.selectedVersionId ?: "selected version"}.", style = VoidDesign.Caption)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createProfile(profileName)
                    profileName = ""
                    newProfile = false
                }) { Text("Save", color = VoidDesign.Ink) }
            },
            dismissButton = { TextButton(onClick = { newProfile = false }) { Text("Cancel", color = VoidDesign.Muted) } },
        )
    }

    confirm?.let { (kind, value) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = VoidDesign.Panel,
            title = { Text("Remove $kind?", style = VoidDesign.Title) },
            text = { Text(value, style = VoidDesign.Caption) },
            confirmButton = {
                TextButton(onClick = {
                    when (kind) {
                        "version" -> vm.removeVersion(value)
                        "profile" -> vm.deleteProfile(value)
                        "mod" -> vm.deleteMod(value)
                    }
                    confirm = null
                }) { Text("Remove", color = VoidDesign.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel", color = VoidDesign.Muted) } },
        )
    }
}
