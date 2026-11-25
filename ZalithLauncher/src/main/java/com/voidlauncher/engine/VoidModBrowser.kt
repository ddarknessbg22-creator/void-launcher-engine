package com.voidlauncher.engine

import com.movtery.zalithlauncher.game.download.addon.platform.Platform
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSearchData
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.addon.platform.curseforge.CurseForgeSearcher
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.ModrinthSearcher

data class VoidModResult(
    val id: String,
    val name: String,
    val summary: String,
    val platform: String,
    val platformKind: Platform,
    val iconUrl: String? = null,
    val data: PlatformSearchData,
)

/** Real search against Modrinth and CurseForge using Zalith's proven API clients. */
object VoidModBrowser {
    suspend fun search(query: String, minecraftVersion: String?): List<VoidModResult> {
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
        }

        return results.distinctBy { it.platform to it.id }
    }
}
