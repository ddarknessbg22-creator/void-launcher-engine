package com.voidlauncher.engine

import android.content.Context
import com.movtery.zalithlauncher.game.download.addon.downloadSingleForVersions
import com.movtery.zalithlauncher.game.download.addon.platform.Platform
import com.movtery.zalithlauncher.game.download.addon.platform.getVersions
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.voidlauncher.util.VoidPaths

/** Downloads a mod's newest file that matches the selected Minecraft version. */
object VoidModInstaller {

    /** Stops a search result from being installed twice in a row. */
    private val inFlight = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    suspend fun download(
        context: Context,
        projectId: String,
        platform: Platform,
        minecraftVersion: String,
        versionId: String,
    ): String {
        val key = "$platform:$projectId:$versionId"
        check(inFlight.putIfAbsent(key, true) == null) { "This mod is already downloading" }

        return try {
            val versions = getVersions(projectId, platform)
            if (versions.isEmpty()) throw IllegalStateException("No files available for this mod")

            val match = versions.firstOrNull { candidate ->
                runCatching { candidate.initFileStrict(projectId) }.getOrDefault(false) &&
                    minecraftVersion in candidate.platformGameVersion().toList()
            } ?: versions.firstOrNull { candidate ->
                runCatching { candidate.initFileStrict(projectId) }.getOrDefault(false) &&
                    candidate.platformGameVersion().any { it.startsWith(minecraftVersion.substringBeforeLast('.')) }
            } ?: throw IllegalStateException("No file matches Minecraft $minecraftVersion")

            val gameHome = VoidPaths.root(context).absolutePath
            val version = VersionsManager.loadVersion(gameHome, versionId)
                ?: throw IllegalStateException("$versionId is not installed")

            downloadSingleForVersions(
                version = match,
                versions = listOf(version),
                folder = VersionFolders.MOD.folderName,
                submitError = { message: ErrorViewModel.ThrowableMessage ->
                    throw IllegalStateException(message.message.toString())
                },
            )
            match.platformFileName()
        } finally {
            inFlight.remove(key)
        }
    }
}
