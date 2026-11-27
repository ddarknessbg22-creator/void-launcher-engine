package com.voidlauncher.engine

import android.content.Context
import com.movtery.zalithlauncher.game.download.addon.downloadSingleForVersions
import com.movtery.zalithlauncher.game.download.addon.platform.Platform
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.addon.platform.curseforge.CurseForgeSearcher
import com.movtery.zalithlauncher.game.download.addon.platform.getVersions
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.ModrinthSearcher
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

/** Downloads mods from Modrinth and CurseForge into the selected version's mods folder. */
object VoidModInstaller {

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
        }.sortedWith(
            compareByDescending<VoidModVersionOption> { it.installed }
                .thenByDescending { it.compatible }
                .thenByDescending { it.gameVersions.firstOrNull().orEmpty() },
        ).take(80)
    }

    /** Installs one specific file. Returns the copied file name and its project id. */
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

    /** Searches both platforms. Errors are surfaced rather than silently swallowed. */
    suspend fun search(query: String, minecraftVersion: String?): List<VoidModResult> {
        val filter = PlatformSearchFilter(
            searchName = query,
            gameVersion = minecraftVersion.orEmpty(),
        )
        val results = mutableListOf<VoidModResult>()
        var lastError: Exception? = null

        runCatching {
            ModrinthSearcher().searchAddons(query, filter, PlatformClasses.MOD)
                .getAddonPage(PlatformClasses.MOD).data.forEach { (data, info) ->
                    results += VoidModResult(
                        id = data.platformId(),
                        name = info?.displayName()?.ifBlank { null } ?: "Modrinth mod",
                        summary = "",
                        platform = Platform.MODRINTH.displayName,
                        platformKind = Platform.MODRINTH,
                        iconUrl = data.platformIconUrl(),
                        data = data,
                    )
                }
        }.onFailure { lastError = it as? Exception }

        runCatching {
            CurseForgeSearcher().searchAddons(query, filter, PlatformClasses.MOD)
                .getAddonPage(PlatformClasses.MOD).data.forEach { (data, info) ->
                    results += VoidModResult(
                        id = data.platformId(),
                        name = info?.displayName()?.ifBlank { null } ?: "CurseForge mod",
                        summary = "",
                        platform = Platform.CURSEFORGE.displayName,
                        platformKind = Platform.CURSEFORGE,
                        iconUrl = data.platformIconUrl(),
                        data = data,
                    )
                }
        }.onFailure { lastError = it as? Exception }

        if (results.isEmpty() && lastError != null) throw lastError!!
        return results.distinctBy { it.platform to it.id }
    }
}
