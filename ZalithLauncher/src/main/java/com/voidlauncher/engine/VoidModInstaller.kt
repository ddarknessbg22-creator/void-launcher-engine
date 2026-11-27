package com.voidlauncher.engine

import android.content.Context
import com.movtery.zalithlauncher.game.download.addon.platform.Platform
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSearchData
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.addon.platform.curseforge.CurseForgeSearcher
import com.movtery.zalithlauncher.game.download.addon.platform.getVersions
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.ModrinthSearcher
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.voidlauncher.util.Net
import com.voidlauncher.util.VoidPaths
import java.io.File

data class VoidModVersionOption(
    val id: String,
    val label: String,
    val fileName: String,
    val gameVersions: List<String>,
    val compatible: Boolean,
    val installed: Boolean,
    val recommended: Boolean = false,
)

data class VoidModResult(
    val id: String,
    val name: String,
    val summary: String,
    val platform: String,
    val platformKind: Platform,
    val iconUrl: String? = null,
    val installed: Boolean = false,
    val data: PlatformSearchData,
)

/** Downloads real mod files into the selected version's mods folder, with live progress. */
object VoidModInstaller {

    /** Searches Modrinth and CurseForge. */
    suspend fun search(query: String, minecraftVersion: String?): List<VoidModResult> {
        val filter = PlatformSearchFilter(searchName = query, gameVersion = minecraftVersion.orEmpty())
        val results = mutableListOf<VoidModResult>()

        runCatching {
            ModrinthSearcher().searchAddons(query, filter, PlatformClasses.MOD)
                .getAddonPage(PlatformClasses.MOD).data.forEach { (data, info) ->
                    results += VoidModResult(
                        id = data.platformId(),
                        name = info?.displayName()?.ifBlank { null } ?: "Modrinth mod",
                        summary = "", platform = Platform.MODRINTH.displayName,
                        platformKind = Platform.MODRINTH, iconUrl = data.platformIconUrl(), data = data,
                    )
                }
        }

        runCatching {
            CurseForgeSearcher().searchAddons(query, filter, PlatformClasses.MOD)
                .getAddonPage(PlatformClasses.MOD).data.forEach { (data, info) ->
                    results += VoidModResult(
                        id = data.platformId(),
                        name = info?.displayName()?.ifBlank { null } ?: "CurseForge mod",
                        summary = "", platform = Platform.CURSEFORGE.displayName,
                        platformKind = Platform.CURSEFORGE, iconUrl = data.platformIconUrl(), data = data,
                    )
                }
        }

        return results.distinctBy { it.platform to it.id }
    }

    /** Lists versions of a mod, newest compatible first. */
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

    /**
     * Downloads one mod version with real byte progress and copies it into the
     * version's mods folder. Returns fileName to projectId.
     */
    suspend fun downloadExact(
        context: Context,
        projectId: String,
        platform: Platform,
        selectedVersionId: String,
        minecraftVersion: String,
        installedVersionId: String,
        onProgress: (bytesDone: Long, bytesTotal: Long) -> Unit = { _, _ -> },
    ): Pair<String, String> {
        val match = getVersions(projectId, platform).firstOrNull { candidate ->
            candidate.platformId() == selectedVersionId &&
                runCatching { candidate.initFileStrict(projectId) }.getOrDefault(false)
        } ?: throw IllegalStateException("That mod file is no longer available")

        val gameHome = VoidPaths.root(context).absolutePath
        val version = VersionsManager.loadVersion(gameHome, installedVersionId)
            ?: throw IllegalStateException("$installedVersionId is not installed")

        val fileName = match.platformFileName()
        val cache = File(context.cacheDir, "void-addons/${match.platformSha1() ?: selectedVersionId}")
        cache.parentFile?.mkdirs()

        // Real streaming download, verified and cancellable, with byte-level progress.
        Net.downloadToFile(
            url = match.platformDownloadUrl(),
            destination = cache,
            expectedSha1 = match.platformSha1(),
            onProgress = onProgress,
        )

        val modsDir = File(version.getGameDir(), VersionFolders.MOD.folderName)
        modsDir.mkdirs()
        val target = File(modsDir, fileName)
        cache.copyTo(target, overwrite = true)
        return fileName to projectId
    }
}
