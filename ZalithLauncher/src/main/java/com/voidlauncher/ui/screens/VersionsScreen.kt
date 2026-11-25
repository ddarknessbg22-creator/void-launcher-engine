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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voidlauncher.ui.UiState
import com.voidlauncher.ui.VoidViewModel
import com.voidlauncher.ui.components.VoidContentFrame
import com.voidlauncher.ui.components.VoidDivider
import com.voidlauncher.ui.components.VoidPill
import com.voidlauncher.ui.theme.VoidDesign

private val FILTERS = listOf("All", "Release", "Snapshot", "Old")

/** VOID 2.0 Versions: dense, fast, keyboard-like list rather than card soup. */
@Composable
fun VersionsScreen(state: UiState, vm: VoidViewModel) {
    val filtered = state.versions.filter { v ->
        val typeOk = when (state.typeFilter) {
            "Release" -> v.type.equals("release", true)
            "Snapshot" -> v.type.equals("snapshot", true)
            "Old" -> !v.type.equals("release", true) && !v.type.equals("snapshot", true)
            else -> true
        }
        typeOk && (state.search.isBlank() || v.id.contains(state.search, true))
    }

    VoidContentFrame {
        Column(Modifier.fillMaxSize().background(VoidDesign.Canvas)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("VERSIONS", style = VoidDesign.Display)
                    Spacer(Modifier.height(2.dp))
                    Text("${state.versions.size} real Minecraft builds", style = VoidDesign.Caption)
                }
                if (state.versionLoading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = VoidDesign.Ink)
                } else {
                    Text("REFRESH", style = VoidDesign.Label, modifier = Modifier.clickable { vm.refreshVersions() })
                }
            }
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = state.search,
                onValueChange = { vm.setSearch(it) },
                placeholder = { Text("Search versions", style = VoidDesign.Caption) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VoidDesign.Ink,
                    unfocusedTextColor = VoidDesign.Ink,
                    cursorColor = VoidDesign.Ink,
                    focusedBorderColor = VoidDesign.Ink,
                    unfocusedBorderColor = VoidDesign.Line,
                    focusedContainerColor = VoidDesign.Panel,
                    unfocusedContainerColor = VoidDesign.Panel,
                ),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FILTERS.forEach { filter ->
                    val selected = state.typeFilter == filter
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (selected) VoidDesign.Ink else VoidDesign.Panel)
                            .clickable { vm.setTypeFilter(filter) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            filter.uppercase(),
                            style = VoidDesign.Label.copy(
                                color = if (selected) VoidDesign.Canvas else VoidDesign.Muted,
                            ),
                        )
                    }
                }
            }
            state.versionError?.let {
                Spacer(Modifier.height(8.dp))
                Text("Offline - showing cached versions", style = VoidDesign.Caption)
            }
            Spacer(Modifier.height(12.dp))
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(filtered, key = { it.id }) { version ->
                val selected = state.selectedVersionId == version.id
                val installed = state.installedIds.contains(version.id)
                val downloading = state.download.active && state.download.versionId == version.id
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(if (selected) VoidDesign.Panel else VoidDesign.Canvas)
                        .clickable { vm.selectVersion(version.id) }
                        .padding(horizontal = 22.dp, vertical = 13.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                version.id,
                                style = VoidDesign.Title.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                ),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(version.releaseTime.take(10), style = VoidDesign.Caption)
                        }
                        Spacer(Modifier.size(8.dp))
                        VoidPill(version.type, active = selected)
                        Spacer(Modifier.size(8.dp))
                        when {
                            downloading -> CircularProgressIndicator(
                                Modifier.size(16.dp), strokeWidth = 2.dp, color = VoidDesign.Ink,
                            )
                            installed -> Text("READY", style = VoidDesign.Label)
                            else -> Text(
                                "INSTALL",
                                style = VoidDesign.Label.copy(color = VoidDesign.Ink),
                                modifier = Modifier.clickable { vm.installVersion(version.id) },
                            )
                        }
                    }
                    if (selected) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (installed) "Selected and verified on this device"
                            else "Selected - press Play on Home to install automatically",
                            style = VoidDesign.Caption,
                        )
                    }
                }
                VoidDivider()
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
    }
