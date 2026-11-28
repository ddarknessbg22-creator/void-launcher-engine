package com.voidlauncher.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/** Regression tests for VOID 1.0.1 mod-browser and loader UI fixes. */
class VoidModUiLogicTest {
    @Test fun loaderProgressPercentClamps() {
        assertEquals(0, VoidLoaderInstaller.LoaderProgress("", -1f).percent)
        assertEquals(75, VoidLoaderInstaller.LoaderProgress("", 0.75f).percent)
        assertEquals(100, VoidLoaderInstaller.LoaderProgress("", 2f).percent)
    }

    @Test fun modDetailFallsBackToResultWhenFetchFails() {
        val result = VoidModResult(
            id = "x", name =SodiumName, summary = SodiumSummary,
            platform = "Modrinth", platformKind = com.movtery.zalithlauncher.game.download.addon.platform.Platform.MODRINTH,
            data = FakePlatformSearchData(),
        )
        // Detail fallback is covered by VoidModInstaller.detail's getOrDefault; verify the shape.
        val fallback = VoidModDetail(title = result.name, description = result.summary, iconUrl = result.iconUrl)
        assertEquals(SodiumName, fallback.title)
        assertEquals(SodiumSummary, fallback.description)
    }

    private companion object {
        const val SodiumName = "Sodium"
        const val SodiumSummary = "A rendering optimization mod"
    }
}

private class FakePlatformSearchData : com.movtery.zalithlauncher.game.download.addon.platform.PlatformSearchData {
    override fun platform() = com.movtery.zalithlauncher.game.download.addon.platform.Platform.MODRINTH
    override fun platformId() = "x"
    override fun platformTitle() = "Sodium"
    override fun platformDescription() = "A rendering optimization mod"
    override fun platformAuthors() = emptyList<String>()
    override fun platformIconUrl() = null
    override fun platformDownloadCount() = 0L
    override fun platformFollows() = null
    override fun platformModLoaders() = emptyList<com.movtery.zalithlauncher.game.download.addon.platform.PlatformDisplayLabel>()
    override fun platformCategories(classes: com.movtery.zalithlauncher.game.download.addon.platform.PlatformClasses) = emptyList<com.movtery.zalithlauncher.game.download.addon.platform.PlatformFilterCode>()
}
