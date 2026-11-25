package com.voidlauncher.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.voidlauncher.model.EngineInfo
import com.voidlauncher.model.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.voidlauncher.util.Net
import com.voidlauncher.util.VoidPaths
import java.io.File
import org.json.JSONObject

private data class EngineDef(
    val packageName: String,
    val displayName: String,
    val note: String,
)

/**
 * VOID does not bundle a JVM / LWJGL / GL translation stack — that is a full native
 * engine in its own right. Instead it detects an installed, proven engine and hands
 * the launch to it. This is a real handoff, not a simulated launch.
 */
class EngineRepository(context: Context) {
    private val app = context.applicationContext
    private val pm = app.packageManager

    private val _download = MutableStateFlow(DownloadState())
    val download: StateFlow<DownloadState> = _download.asStateFlow()

    private val known = listOf(
        EngineDef("net.kdt.pojavlaunch", "PojavLauncher", "Most compatible; Android ARM64"),
        EngineDef("net.kdt.pojavlaunch.debug", "PojavLauncher (debug)", "Development build"),
        EngineDef("com.amethyst.launcher", "Amethyst", "PojavLauncher successor"),
        EngineDef("org.fcl.launcher", "Fold Craft Launcher", "Foldable / large-screen focused"),
        EngineDef("com.zalith.launcher", "Zalith Launcher", "MobileGlues renderer / Sodium friendly"),
        EngineDef("com.movtery.zalithlauncher", "Zalith Launcher", "MobileGlues renderer / Sodium friendly"),
        EngineDef("com.movtery.zalithlauncher.v2", "Zalith Launcher", "MobileGlues renderer / Sodium friendly"),
        EngineDef("com.movtery.zalithlauncher.v2.debug", "Zalith Launcher", "MobileGlues renderer / Sodium friendly"),
        EngineDef("com.rxlauncher.app", "RXLauncher", "Mod-focused launcher"),
        EngineDef("com.mcinabox.launcher", "MCinaBox", "Boardwalk-based launcher"),
    )

    fun detect(): List<EngineInfo> = known.map { def ->
        try {
            val info = pm.getPackageInfo(def.packageName, 0)
            EngineInfo(
                packageName = def.packageName,
                displayName = def.displayName,
                installed = true,
                versionName = info.versionName,
                note = def.note,
            )
        } catch (e: PackageManager.NameNotFoundException) {
            EngineInfo(
                packageName = def.packageName,
                displayName = def.displayName,
                installed = false,
                note = def.note,
            )
        }
    }

    fun installedEngines(): List<EngineInfo> = detect().filter { it.installed }

    fun preferred(preferredPackage: String?, available: List<EngineInfo>): EngineInfo? =
        available.firstOrNull { it.packageName == preferredPackage } ?: available.firstOrNull()

    fun launch(context: Context, engine: EngineInfo): Result<Unit> = runCatching {
        val intent = pm.getLaunchIntentForPackage(engine.packageName)
            ?: throw IllegalStateException("${engine.displayName} has no launchable activity")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    companion object {
        const val ENGINE_SOURCE_URL = "https://github.com/PojavLauncherTeam/PojavLauncher/releases"
        const val VOID_ENGINE_URL = "https://github.com/ZalithLauncher/ZalithLauncher2/releases"
            }
}
