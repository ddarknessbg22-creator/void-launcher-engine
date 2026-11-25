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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.voidlauncher.engine.VoidModBrowser
import com.voidlauncher.engine.VoidModResult
import com.voidlauncher.ui.components.VoidContentFrame
import com.voidlauncher.ui.components.VoidDivider
import com.voidlauncher.ui.components.VoidPill
import com.voidlauncher.ui.components.VoidSectionTitle
import com.voidlauncher.ui.theme.VoidDesign

/** Minimal VOID mod browser: search CurseForge + Modrinth, real results, no clutter. */
@Composable
fun ModBrowserScreen(selectedVersion: String?) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<VoidModResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    LaunchedEffect(query, selectedVersion) {
        if (query.isBlank()) {
            results = emptyList()
            searched = false
            return@LaunchedEffect
        }
        loading = true
        results = VoidModBrowser.search(query.trim(), selectedVersion)
        searched = true
        loading = false
    }

    VoidContentFrame {
        Column(Modifier.fillMaxSize().background(VoidDesign.Canvas)) {
            Spacer(Modifier.height(20.dp))
            Text("MODS", style = VoidDesign.Display)
            Spacer(Modifier.height(4.dp))
            Text("CurseForge + Modrinth", style = VoidDesign.Caption)
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search mods", style = VoidDesign.Caption) },
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
            Spacer(Modifier.height(8.dp))
            Text(
                selectedVersion?.let { "Compatibility: $it" } ?: "No version selected",
                style = VoidDesign.Caption,
            )
            Spacer(Modifier.height(16.dp))

            when {
                loading -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                    CircularProgressIndicator(color = VoidDesign.Ink, strokeWidth = 2.dp)
                }
                !searched -> Text("Search to see real mods.", style = VoidDesign.Body)
                results.isEmpty() -> Text("No mods found.", style = VoidDesign.Body)
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(results, key = { it.platform to it.id }) { mod ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .background(Color(0xFF0B140B))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(mod.name, style = VoidDesign.Title)
                                Spacer(Modifier.height(2.dp))
                                Text(mod.summary, style = VoidDesign.Caption, maxLines = 2)
                            }
                            Spacer(Modifier.width(8.dp))
                            VoidPill(mod.platform)
                        }
                        VoidDivider()
                    }
                }
            }
        }
    }
}
