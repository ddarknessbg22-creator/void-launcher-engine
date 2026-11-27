package com.voidlauncher.engine

import com.movtery.zalithlauncher.game.download.addon.platform.Platform
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSearchData
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.addon.platform.curseforge.CurseForgeSearcher
import com.movtery.zalithlauncher.game.download.addon.platform.curseforge.MCIM_CURSEFORGE_API
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.ModrinthSearcher

/** Search outcome: results plus per-provider warnings the UI can show instead of failing silently. */
data class VoidModSearchResult(
    val results: List<VoidModResult>,
    val warnings: List<String>,
)

/**
 * Real search against Modrinth and CurseForge using the engine's proven API clients.
 *
 * CurseForge's official endpoint requires an API key we cannot ship, so the keyless MCIM mirror is
 * tried as a fallback. A provider failure must never discard the other provider's results — that
 * was why CurseForge mods appeared to be missing.
 */
object VoidModBrowser {
    /** "1.21.11-fabric" -> "1.21.11"; mod APIs only understand base MC versions. */
    fun baseMinecraftVersion(versionId: String?): String? =
        versionId?.substringBefore('-')?.takeIf { it.isNotBlank() }

    suspend fun search(query: String, versionId: String?): VoidModSearchResult {
        val minecraftVersion = baseMinecraftVersion(versionId)
        val filter = PlatformSearchFilter(
            searchName = query,
            gameVersion = minecraftVersion.orEmpty(),
        )
        val results = mutableListOf<VoidModResult>()
        val warnings = mutableListOf<String>()

        runCatching {
            ModrinthSearcher().searchAddons(query, filter, PlatformClasses.MOD)
                .getAddonPage(PlatformClasses.MOD).data.forEach { (data, info) ->
                    results += VoidModResult(
                        id = data.platformId(),
                        name = info?.displayName()?.takeIf { it.isNotBlank() }
                            ?: data.platformTitle(),
                        summary = info?.description()?.takeIf { it.isNotBlank() }
                            ?: data.platformDescription(),
                        platform = Platform.MODRINTH.displayName,
                        platformKind = Platform.MODRINTH,
                        iconUrl = data.platformIconUrl(),
                        data = data,
                    )
                }
        }.onFailure { warnings += "Modrinth: ${it.message ?: it::class.simpleName}" }

        // Official CurseForge first, then the keyless mirror. Stop at the first that answers.
        var cfError: Exception? = null
        for (searcher in listOf(
            CurseForgeSearcher(),
            CurseForgeSearcher(api = MCIM_CURSEFORGE_API, source = "CurseForge mirror"),
        )) {
            val before = results.size
            runCatching {
                searcher.searchAddons(query, filter, PlatformClasses.MOD)
                    .getAddonPage(PlatformClasses.MOD).data.forEach { (data, info) ->
                        results += VoidModResult(
                            id = data.platformId(),
                            name = info?.displayName()?.takeIf { it.isNotBlank() }
                                ?: data.platformTitle(),
                            summary = data.platformDescription(),
                            platform = Platform.CURSEFORGE.displayName,
                            platformKind = Platform.CURSEFORGE,
                            iconUrl = data.platformIconUrl(),
                            data = data,
                        )
                    }
            }.onFailure { cfError = it as? Exception }
            if (results.size > before) break
        }
        if (results.none { it.platformKind == Platform.CURSEFORGE }) {
            warnings += "CurseForge: ${cfError?.message ?: "no results"}"
        }

        return VoidModSearchResult(
            results = results.distinctBy { it.platform to it.id },
            warnings = warnings,
        )
    }
}
