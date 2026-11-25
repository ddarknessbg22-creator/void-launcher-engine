package com.voidlauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voidlauncher.ui.UiState
import com.voidlauncher.ui.VoidViewModel
import com.voidlauncher.ui.components.EmptyState
import com.voidlauncher.ui.components.MonoTextField
import com.voidlauncher.ui.components.Pill
import com.voidlauncher.ui.theme.Amoled
import com.voidlauncher.ui.theme.Danger
import com.voidlauncher.ui.theme.Grey100
import com.voidlauncher.ui.theme.Grey400
import com.voidlauncher.ui.theme.Hairline
import com.voidlauncher.ui.theme.Surface2
import com.voidlauncher.ui.theme.White

private val FILTERS = listOf("All", "Release", "Snapshot", "Old")

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

    Column(Modifier.fillMaxSize().background(Amoled)) {
        Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, end = 20.dp, top = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Versions", style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp))
                    Text("${state.versions.size} versions · Mojang", color = Grey400,
                        style = MaterialTheme.typography.labelSmall)
                }
                if (state.versionLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = White)
                else TextButton(onClick = { vm.refreshVersions() }) { Text("Refresh", color = Grey100) }
            }
            Spacer(Modifier.height(14.dp))
            MonoTextField(state.search, { vm.setSearch(it) }, "Search versions")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FILTERS.forEach { f ->
                    val selected = state.typeFilter == f
                    Box(
                        Modifier
                            .background(if (selected) White else Surface2, RoundedCornerShape(8.dp))
                            .border(1.dp, if (selected) White else Hairline, RoundedCornerShape(8.dp))
                            .clickable { vm.setTypeFilter(f) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(f, color = if (selected) Amoled else Grey100,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            state.versionError?.let {
                Spacer(Modifier.height(8.dp))
                Text("Offline · showing cached list", color = Grey400,
                    style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(10.dp))
        }

        LazyColumn(Modifier.fillMaxSize()) {
            if (filtered.isEmpty() && !state.versionLoading) {
                item { EmptyState("No versions found", "Adjust the search or refresh") }
            }
            items(filtered, key = { it.id }) { version ->
                val installed = state.installedIds.contains(version.id)
                val selected = state.selectedVersionId == version.id
                val downloading = state.download.active && state.download.versionId == version.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.selectVersion(version.id) }
                        .background(if (selected) Surface2 else Amoled)
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(version.id, style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            Spacer(Modifier.size(8.dp))
                            Pill(version.type, filled = version.type.equals("release", true))
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(version.releaseTime.take(10), color = Grey400,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    when {
                        downloading -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = White)
                        installed -> Text("INSTALLED", color = Grey100, style = MaterialTheme.typography.labelSmall)
                        selected -> Text("SELECTED", color = White, style = MaterialTheme.typography.labelSmall)
                        else -> TextButton(onClick = { vm.installVersion(version.id) }) {
                            Text("Install", color = Grey100, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
