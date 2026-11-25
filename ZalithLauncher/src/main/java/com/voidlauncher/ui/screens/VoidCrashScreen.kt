package com.voidlauncher.ui.screens

import android.content.Context
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
    onSaveLog: () -> Unit,
    onRestart: () -> Unit,
) {
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
        Spacer(Modifier.height(8.dp))
        Text(message, style = VoidDesign.Body)
        Spacer(Modifier.height(22.dp))

        VoidSectionTitle("Diagnostics")
        VoidPanel {
            Text("Log: ${logPath ?: "No log file"}", style = VoidDesign.Caption)
            Spacer(Modifier.height(8.dp))
            VoidDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                "VOID saves the relevant launcher/game log into the VOID Crashes folder on your device. Share that file when reporting a problem.",
                style = VoidDesign.Caption,
            )
        }
        Spacer(Modifier.height(18.dp))
        VoidPrimaryButton("Save debug log", onSaveLog, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        VoidPrimaryButton("Restart VOID", onRestart, Modifier.fillMaxWidth())
        Spacer(Modifier.height(28.dp))
    }
}

object VoidCrashLogs {
    fun save(context: Context, source: String?, reason: String): String? {
        val targetDir = File(context.getExternalFilesDir(null), "VOID/Crashes")
        targetDir.mkdirs()
        val file = File(targetDir, "void-crash-${System.currentTimeMillis()}.txt")
        val log = source?.let { runCatching { File(it).readText() }.getOrNull() }.orEmpty()
        val header = buildString {
            append("VOID Launcher crash report\n")
            append("Time: ${java.time.Instant.now()}\n")
            append("Device: ${android.os.Build.MODEL} / Android ${android.os.Build.VERSION.RELEASE}\n")
            append("Reason: $reason\n\n")
        }
        file.writeText(header + log)
        return file.absolutePath
    }
}
