package com.voidlauncher.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.voidlauncher.ui.components.VoidDivider
import com.voidlauncher.ui.components.VoidPanel
import com.voidlauncher.ui.components.VoidPrimaryButton
import com.voidlauncher.ui.components.VoidSectionTitle
import com.voidlauncher.ui.theme.VoidDesign
import java.io.File

@Composable
fun VoidCrashScreen(
    title: String,
    message: String,
    logPath: String?,
    onSaveLog: () -> String?,
    onRestart: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var saved by remember { mutableStateOf<String?>(null) }
    val cause = remember(logPath) { VoidCrashLogs.cause(logPath, message) }

    Column(
        Modifier
            .fillMaxSize()
            .background(VoidDesign.Canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Text("VOID", style = VoidDesign.Display)
        Spacer(Modifier.height(6.dp))
        Text("RECOVERY", style = VoidDesign.Label)
        Spacer(Modifier.height(28.dp))

        Text(title, style = VoidDesign.Headline)
        Spacer(Modifier.height(12.dp))
        VoidPanel {
            Text("WHAT HAPPENED", style = VoidDesign.Label)
            Spacer(Modifier.height(8.dp))
            Text(cause, style = VoidDesign.Body)
        }
        Spacer(Modifier.height(18.dp))

        VoidSectionTitle("Details")
        VoidPanel {
            Text(message, style = VoidDesign.Caption, maxLines = 3)
            Spacer(Modifier.height(8.dp))
            VoidDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                if (saved != null) "Saved to: $saved" else "Log: ${logPath ?: "none"}",
                style = VoidDesign.Caption,
            )
        }
        Spacer(Modifier.height(18.dp))
        VoidPrimaryButton(
            text = if (saved == null) "Save debug log" else "Saved - tap to copy path",
            onClick = {
                val path = saved ?: onSaveLog()
                if (path != null) {
                    saved = path
                    Toast.makeText(context, "Debug log saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not save log", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        VoidPrimaryButton("Restart VOID", onRestart, Modifier.fillMaxWidth())
        Spacer(Modifier.height(28.dp))
    }
}

object VoidCrashLogs {

    /** Reads the game/launcher log and returns the single most useful line. */
    fun cause(logPath: String?, fallback: String): String {
        val text = logPath?.let { runCatching { File(it).readText() }.getOrNull() }.orBlank()
        if (text.isBlank()) return fallback.take(200)

        // Missing mod dependency is by far the most common crash; surface it first.
        val missing = Regex("""Mod '([^']+)'[^\n]*?requires[^\n]*""", RegexOption.IGNORE_CASE)
            .find(text)?.value
        if (missing != null) return "A mod is missing a dependency:\n$missing"

        val incompatible = Regex("""Incompatible mods found![\s\S]{0,300}""", RegexOption.IGNORE_CASE)
            .find(text)?.value?.lineSequence()?.firstOrNull()
        if (incompatible != null) return "Incompatible mods found. Check mod versions and dependencies."

        val error = text.lineSequence()
            .filter { it.contains("ERROR", true) || it.contains("Exception", true) || it.contains("Caused by", true) }
            .map { it.substringAfterLast(']').trim() }
            .firstOrNull { it.length in 6..200 }
        return error ?: fallback.take(200)
    }

    /** Saves a real log file, including recent game log when the path is stale. */
    fun save(context: Context, source: String?, reason: String): String? {
        val dir = File(context.getExternalFilesDir(null), "VOID/Crashes")
        if (!dir.exists() && !dir.mkdirs()) return null
        val file = File(dir, "void-crash-${System.currentTimeMillis()}.txt")
        val log = source?.let { runCatching { File(it).readText() }.getOrNull() }.orEmpty()
        val header = buildString {
            append("VOID Launcher crash report\n")
            append("Time: ${java.time.Instant.now()}\n")
            append("Device: ${android.os.Build.MODEL} / Android ${android.os.Build.VERSION.RELEASE}\n")
            append("Reason: $reason\n\n")
        }
        return runCatching {
            file.writeText(header + log)
            file.absolutePath
        }.getOrNull()
    }

    private fun String?.orBlank(): String = this ?: ""
}
