package com.voidlauncher.engine

import com.movtery.zalithlauncher.game.download.addon.platform.Platform
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSearchData
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.addon.platform.curseforge.CurseForgeSearcher
import com.movtery.zalithlauncher.game.download.addon.platform.curseforge.MCIM_CURSEFORGE_API
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.ModrinthSearcher

/** Real search against Modrinth and CurseForge using Zalith's proven API clients. */
object VoidModBrowser {
    /** \"1.21.11-fabric\" -> \"1.21.11\"; mod APIs only understand base MC versions. */
    fun baseMinecraftVersion(versionId: String?): String? =
        versionId?.substringBefore('-')?.takeIf { it.isNotBlank() }

    suspend fun search(query: String, versionId: String?): List<VoidModResult> {
        val minecraftVersion = baseMinecraftVersion(versionId)
        val filter = PlatformSearchFilter(
            searchName = query,
            gameVersion = minecraftVersion.orEmpty(),
        )
        val results = mutableListOf<VoidModResult>()

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
        }

        // The official CurseForge API needs an API key we cannot ship. Try official first,
        // then the keyless MCIM mirror so CurseForge mods actually appear.
        var cfError: Exception? = null
        for (searcher in listOf(
            CurseForgeSearcher(),
            CurseForgeSearcher(api = MCIM_CURSEFORGE_API, source = "CurseForge mirror"),
        )) {
            runCatching {
                searcher.searchAddons(query, filter, PlatformClasses.MOD)
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
                return@runCatching
            }.onFailure { cfError = it as? Exception }
            if (results.any { it.platform == Platform.CURSEFORGE.displayName }) break
        }

        return results.distinctBy { it.platform to it.id }
    }
}
