package com.voidlauncher.ui.screens

import androidx.compose.foundation.layout.heightIn

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.CircularProgressIndicator
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
import com.voidlauncher.ui.UiState
import com.voidlauncher.ui.VoidViewModel
import com.voidlauncher.ui.components.VoidContentFrame
import com.voidlauncher.ui.components.VoidDivider
import com.voidlauncher.ui.components.VoidPanel
import com.voidlauncher.ui.components.VoidPill
import com.voidlauncher.ui.components.VoidSecondaryButton
import com.voidlauncher.ui.components.VoidSectionTitle
import com.voidlauncher.ui.theme.VoidDesign
import com.voidlauncher.util.Net

private val SECTIONS = listOf("Versions", "Profiles", "Mods", "Loaders")

/** VOID Library: one place for everything installed, grouped into four simple sections. */
@Composable
fun LibraryScreen(state: UiState, vm: VoidViewModel) {
    var section by remember { mutableStateOf("Versions") }
    var confirm by remember { mutableStateOf<Pair<String, String>?>(null) }

    VoidContentFrame {
        Column(Modifier.fillMaxSize().background(VoidDesign.Canvas)) {
            Spacer(Modifier.height(20.dp))
            Text("LIBRARY", style = VoidDesign.Display)
            Spacer(Modifier.height(16.dp))

            SectionTabs(section) { section = it }
            Spacer(Modifier.height(16.dp))

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
            ) {
                when (section) {
                    "Versions" -> VersionsSection(state, vm) { k, v -> confirm = k to v }
                    "Profiles" -> ProfilesSection(state, vm) { k, v -> confirm = k to v }
                    "Mods" -> ModsSection(state, vm)
                    "Loaders" -> LoadersSection(state, vm)
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    ModVersionSheet(state, vm)

    confirm?.let { (kind, value) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = VoidDesign.Panel,
            title = { Text("Delete $kind?", style = VoidDesign.Title) },
            text = {
                Text(
                    if (kind == "profile")
                        state.profiles.firstOrNull { it.id == value }?.name ?: value
                    else value,
                    style = VoidDesign.Caption,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (kind) {
                        "version" -> vm.removeVersion(value)
                        "profile" -> vm.deleteProfile(value)
                    }
                    confirm = null
                }) { Text("Delete", color = VoidDesign.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel", color = VoidDesign.Muted) } },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SectionTabs(current: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SECTIONS.forEach { item ->
            val selected = item == current
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) VoidDesign.Ink else VoidDesign.Panel)
                    .clickable { onSelect(item) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    item.uppercase(),
                    style = VoidDesign.Label.copy(color = if (selected) VoidDesign.Canvas else VoidDesign.Muted),
                )
            }
        }
    }
}

@Composable
private fun VersionsSection(state: UiState, vm: VoidViewModel, onRemove: (String, String) -> Unit) {
    if (state.installedVersions.isEmpty()) {
        VoidPanel {
            Text("Nothing installed", style = VoidDesign.Title)
            Spacer(Modifier.height(4.dp))
            Text("Install a version from the Versions tab.", style = VoidDesign.Caption)
        }
        return
    }
    state.installedVersions.forEach { version ->
        VoidPanel(Modifier.padding(bottom = 8.dp), onClick = { vm.selectVersion(version.id) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(version.id, style = VoidDesign.Title)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${version.loader} · ${version.libraryCount} libraries · ${version.assetCount} assets",
                        style = VoidDesign.Caption,
                    )
                }
                if (version.id == state.selectedVersionId) VoidPill("Active", active = true)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoidSecondaryButton("Use", { vm.selectVersion(version.id) }, Modifier.weight(1f))
                VoidSecondaryButton("Remove", { onRemove("version", version.id) }, Modifier.weight(1f))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ProfilesSection(state: UiState, vm: VoidViewModel, onRemove: (String, String) -> Unit) {
    var naming by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    VoidSectionTitle("Saved profiles", trailing = {
        if (!naming) {
            Text("NEW", style = VoidDesign.Label.copy(color = VoidDesign.Ink), modifier = Modifier.clickable { naming = true })
        }
    })

    if (naming) {
        VoidPanel(Modifier.padding(bottom = 8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("Profile name", style = VoidDesign.Caption) },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Saves ${state.selectedVersionId ?: "the selected version"}, ${state.activeLoader} and the current mods.",
                style = VoidDesign.Caption,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoidSecondaryButton("Cancel", { naming = false; draft = "" }, Modifier.weight(1f))
                VoidSecondaryButton("Save", {
                    vm.createProfile(draft)
                    draft = ""
                    naming = false
                }, Modifier.weight(1f))
            }
        }
    }

    if (state.profiles.isEmpty()) {
        VoidPanel {
            Text("No profiles yet", style = VoidDesign.Title)
            Spacer(Modifier.height(4.dp))
            Text("A profile remembers your version, loader and mods.", style = VoidDesign.Caption)
        }
        return
    }

    state.profiles.forEach { profile ->
        Box(
            Modifier
                .padding(bottom = 8.dp)
                .combinedClickable(
                    onClick = { vm.selectProfile(profile.id) },
                    onLongClick = { onRemove("profile", profile.id) },
                ),
        ) {
            VoidPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, style = VoidDesign.Title)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${profile.versionId} · ${profile.loader} · ${profile.mods.size} mods",
                            style = VoidDesign.Caption,
                        )
                    }
                    if (profile.id == state.selectedProfileId) VoidPill("Active", active = true)
                }
                Spacer(Modifier.height(6.dp))
                Text("Hold to delete", style = VoidDesign.Caption)
            }
        }
    }
}

@Composable
private fun ModsSection(state: UiState, vm: VoidViewModel) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.importMod(uri, uri.lastPathSegment?.substringAfterLast('/') ?: "mod.jar")
    }

    VoidSectionTitle("Installed mods", trailing = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("↻", style = VoidDesign.Label.copy(color = VoidDesign.Ink),
                modifier = Modifier
                    .clickable { vm.loadMods(state.selectedVersionId.orEmpty()) }
                    .padding(horizontal = 8.dp, vertical = 4.dp))
            Text("ADD JAR", style = VoidDesign.Label.copy(color = VoidDesign.Ink),
                modifier = Modifier.clickable { picker.launch("*/*") })
        }
    })
    VoidPanel(Modifier.padding(bottom = 16.dp)) {
        Text(state.selectedVersionId ?: "No version selected", style = VoidDesign.Caption)
        Spacer(Modifier.height(6.dp))
        Text("Mods go to this version's own mods folder.", style = VoidDesign.Caption)
        VoidDivider(Modifier.padding(vertical = 10.dp))
        if (state.mods.isEmpty()) {
            Text("No mods installed", style = VoidDesign.Body)
            Spacer(Modifier.height(4.dp))
            Text(
                if (state.selectedVersionId?.contains("fabric", true) == true)
                    "Fabric is installed. Add Fabric mods as .jar files."
                else "Add mods as .jar files, or install a mod loader first.",
                style = VoidDesign.Caption,
            )
        } else {
            state.mods.forEach { mod ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(mod.name, style = VoidDesign.Body)
                        Text("${mod.loader} · ${Net.formatBytes(mod.sizeBytes)}", style = VoidDesign.Caption)
                    }
                    VoidPill(if (mod.enabled) "On" else "Off", active = mod.enabled)
                    Spacer(Modifier.size(10.dp))
                    Text("TOGGLE", style = VoidDesign.Label, modifier = Modifier.clickable { vm.toggleMod(mod.fileName) })
                    Spacer(Modifier.size(10.dp))
                    Text("DEL", style = VoidDesign.Label.copy(color = VoidDesign.Danger),
                        modifier = Modifier.clickable { vm.deleteMod(mod.fileName) })
                }
                VoidDivider(Modifier.padding(vertical = 8.dp))
            }
        }
    }

    VoidSectionTitle("Download mods")
    OutlinedTextField(
        value = state.modQuery,
        onValueChange = { vm.setModQuery(it) },
        placeholder = { Text("Search CurseForge and Modrinth", style = VoidDesign.Caption) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        trailingIcon = {
            Text("GO", style = VoidDesign.Label.copy(color = VoidDesign.Ink), modifier = Modifier.clickable { vm.searchMods() })
        },
    )
    Spacer(Modifier.height(6.dp))
    Text(
        state.selectedVersionId?.let { "Matches $it" } ?: "Select a version for best results",
        style = VoidDesign.Caption,
    )
    Spacer(Modifier.height(10.dp))

    if (state.modSearching) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = VoidDesign.Ink)
            Spacer(Modifier.size(10.dp))
            Text("Searching", style = VoidDesign.Caption)
        }
    } else {
        state.modResults.take(40).forEach { result ->
            VoidPanel(Modifier.padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModBox(result.iconUrl)
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(result.name, style = VoidDesign.Title)
                        Spacer(Modifier.height(3.dp))
                        Text(result.summary, style = VoidDesign.Caption, maxLines = 2)
                    }
                    VoidPill(result.platform)
                }
                Spacer(Modifier.height(10.dp))
                VoidSecondaryButton("Install", { vm.downloadMod(result) }, Modifier.fillMaxWidth())
            }
        }
        if (state.modResults.isEmpty() && state.modQuery.isNotBlank() && !state.modSearching) {
            Text("Press GO to search.", style = VoidDesign.Caption)
        }
    }
}

@Composable
private fun ModVersionSheet(state: UiState, vm: VoidViewModel) {
    val selected = state.modSelected ?: return
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { vm.closeMod() },
        containerColor = VoidDesign.Panel,
        title = { Text(selected.name, style = VoidDesign.Title) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            ) {
                if (state.modVersionLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = VoidDesign.Ink)
                        Spacer(Modifier.size(10.dp))
                        Text("Finding compatible versions", style = VoidDesign.Caption)
                    }
                } else if (state.modVersions.isEmpty()) {
                    Text("No versions for this Minecraft version.", style = VoidDesign.Caption)
                } else {
                    state.modVersions.forEach { option ->
                        VoidPanel(Modifier.padding(bottom = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(option.label, style = VoidDesign.Body)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        option.gameVersions.take(4).joinToString(", "),
                                        style = VoidDesign.Caption,
                                    )
                                }
                                when {
                                    option.installed -> VoidPill("Installed", active = true)
                                    option.compatible -> VoidSecondaryButton("Download", {
                                        vm.installModVersion(option)
                                    })
                                    else -> VoidPill("Other MC")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.closeMod() }) { Text("Close", color = VoidDesign.Ink) } },
    )
}

@Composable
private fun ModBox(iconUrl: String?) {
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(VoidDesign.PanelRaised),
        contentAlignment = Alignment.Center,
    ) {
        if (iconUrl.isNullOrBlank()) {
            Text("? ", style = VoidDesign.Label)
        } else {
            coil3.compose.AsyncImage(
                model = iconUrl,
                contentDescription = null,
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}

@Composable
private fun LoadersSection(state: UiState, vm: VoidViewModel) {
    VoidSectionTitle("Mod loader")
    VoidPanel(Modifier.padding(bottom = 14.dp)) {
        Text(state.loaderBaseVersion ?: state.selectedVersionId ?: "No version selected", style = VoidDesign.Title)
        Spacer(Modifier.height(4.dp))
        Text("Installs as a separate version. Your vanilla install stays untouched.", style = VoidDesign.Caption)
    }

    Column {
        com.voidlauncher.engine.VoidLoaderInstaller.LOADERS.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { name ->
                    val selected = name == state.activeLoader
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (selected) VoidDesign.Ink else VoidDesign.Panel)
                            .clickable { vm.setLoader(name) }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name, style = VoidDesign.Label.copy(color = if (selected) VoidDesign.Canvas else VoidDesign.Muted))
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    when {
        state.loaderBusy -> {
            val progress = state.loaderProgress
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = VoidDesign.Ink)
                Spacer(Modifier.size(10.dp))
                Text(progress.label.ifBlank { "Installing ${state.activeLoader}" }, style = VoidDesign.Caption)
                Spacer(Modifier.weight(1f))
                Text("${(progress.fraction * 100).toInt()}%", style = VoidDesign.Caption)
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = VoidDesign.Ink, trackColor = VoidDesign.Line,
            )
        }
        state.loaderLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = VoidDesign.Ink)
            Spacer(Modifier.size(10.dp))
            Text("Fetching ${state.activeLoader} builds", style = VoidDesign.Caption)
        }
        state.loaderOptions.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("No ${state.activeLoader} build for ${state.selectedVersionId ?: "this version"}.", style = VoidDesign.Caption, modifier = Modifier.weight(1f))
            Text("RETRY", style = VoidDesign.Label.copy(color = VoidDesign.Ink), modifier = Modifier.clickable { vm.setLoader(state.activeLoader) })
        }
        else -> state.loaderOptions.take(10).forEach { option ->
            VoidPanel(Modifier.padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(option.version, style = VoidDesign.Title)
                        Spacer(Modifier.height(2.dp))
                        Text(if (option.recommended) "Recommended" else option.loader, style = VoidDesign.Caption)
                    }
                    VoidSecondaryButton("Install", { vm.installLoader(option.loader, option.version) })
                }
            }
        }
    }
}
