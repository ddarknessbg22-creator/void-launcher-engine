package com.voidlauncher.engine

import android.content.Context
import com.movtery.zalithlauncher.game.download.addon.downloadSingleForVersions
import com.movtery.zalithlauncher.game.download.addon.platform.Platform
import com.movtery.zalithlauncher.game.download.addon.platform.getVersions
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.voidlauncher.util.VoidPaths

data class VoidModVersionOption(
    val id: String,
    val label: String,
    val fileName: String,
    val gameVersions: List<String>,
    val compatible: Boolean,
    val installed: Boolean,
)

/** Downloads a mod's newest file that matches the selected Minecraft version. */
object VoidModInstaller {

    /** Stops a search result from being installed twice in a row. */
    private val inFlight = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    suspend fun listVersions(
        projectId: String,
        platform: Platform,
        minecraftVersion: String,
        installedFiles: Set<String>,
    ): List<VoidModVersionOption> {
        val fetched = getVersions(projectId, platform)
        return fetched.mapNotNull { candidate ->
            val ready = runCatching { candidate.initFileStrict(projectId) }.getOrDefault(false)
            if (!ready) return@mapNotNull null
            val games = candidate.platformGameVersion().toList()
            VoidModVersionOption(
                id = candidate.platformId(),
                label = candidate.platformDisplayName().ifBlank { candidate.platformFileName() },
                fileName = candidate.platformFileName(),
                gameVersions = games,
                compatible = minecraftVersion in games,
                installed = candidate.platformFileName() in installedFiles,
            )
        }.sortedWith(compareByDescending<VoidModVersionOption> { it.installed }
            .thenByDescending { it.compatible }
            .thenByDescending { it.gameVersions.firstOrNull().orEmpty() })
            .take(80)
    }

    suspend fun downloadExact(
        context: Context,
        projectId: String,
        platform: Platform,
        selectedVersionId: String,
        minecraftVersion: String,
        installedVersionId: String,
    ): Pair<String, String> {
        val key = "$platform:$projectId:$selectedVersionId"
        check(inFlight.putIfAbsent(key, true) == null) { "This mod is already downloading" }

        return try {
            val match = getVersions(projectId, platform).firstOrNull { candidate ->
                candidate.platformId() == selectedVersionId &&
                    runCatching { candidate.initFileStrict(projectId) }.getOrDefault(false)
            } ?: throw IllegalStateException("That mod file is no longer available")

            val gameHome = VoidPaths.root(context).absolutePath
            val version = VersionsManager.loadVersion(gameHome, installedVersionId)
                ?: throw IllegalStateException("$installedVersionId is not installed")

            downloadSingleForVersions(
                version = match,
                versions = listOf(version),
                folder = VersionFolders.MOD.folderName,
                submitError = { message: ErrorViewModel.ThrowableMessage ->
                    throw IllegalStateException(message.message.toString())
                },
            )
            match.platformFileName() to projectId
        } finally {
            inFlight.remove(key)
        }
    }
}
