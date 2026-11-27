package com.voidlauncher.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression tests for the "1 FPS in game" bug: the launcher used to take the first renderer in
 * the registry (the modern Krypton/NGGL4ES wrapper) for every version.
 */
class VoidRendererSelectorTest {

    private val registry = listOf(
        RendererCandidate("e7b90ed6-krypton", "Krypton Wrapper", available = true, maxVersion = "26.2"),
        RendererCandidate("acaf8100-mobileglues", "MobileGlues", available = true),
        RendererCandidate("8b52d82d-gl4es", "GL4ES", available = true, maxVersion = "1.21.4"),
        RendererCandidate("0fa435e2-zink", "Kopper Zink", available = true, maxVersion = "26.2"),
    )

    @Test
    fun legacyVersionUsesGl4esNotTheFirstRegistryEntry() {
        val chosen = VoidRendererSelector.choose(registry, "1.12.2", preferredId = null)
        assertEquals("8b52d82d-gl4es", chosen)
    }

    @Test
    fun modernVersionUsesMobileGlues() {
        val chosen = VoidRendererSelector.choose(registry, "1.21.11", preferredId = null)
        assertEquals("acaf8100-mobileglues", chosen)
    }

    @Test
    fun explicitChoiceIsHonouredWhenUsable() {
        val chosen = VoidRendererSelector.choose(registry, "1.12.2", preferredId = "0fa435e2-zink")
        assertEquals("0fa435e2-zink", chosen)
    }

    @Test
    fun unavailableRenderersAreSkipped() {
        val onlyKryptonAndGl4es = registry.map {
            if (it.id == "8b52d82d-gl4es") it.copy(available = false) else it
        }
        val chosen = VoidRendererSelector.choose(onlyKryptonAndGl4es, "1.12.2", preferredId = null)
        assertEquals("e7b90ed6-krypton", chosen)
    }

    @Test
    fun nothingAvailableReturnsNull() {
        val none = registry.map { it.copy(available = false) }
        assertNull(VoidRendererSelector.choose(none, "1.12.2", preferredId = null))
    }

    @Test
    fun versionGenerationBoundary() {
        assertEquals(false, VoidRendererSelector.isModern("1.16.5"))
        assertEquals(true, VoidRendererSelector.isModern("1.17"))
        assertEquals(true, VoidRendererSelector.isModern("1.21.11"))
        assertEquals(true, VoidRendererSelector.isModern(null))
    }

    @Test
    fun loaderProgressMapsIndeterminateValuesToZero() {
        assertEquals(0f, VoidLoaderInstaller.progressFraction(-1f), 0f)
        assertEquals(0f, VoidLoaderInstaller.progressFraction(Float.NaN), 0f)
        assertEquals(0.75f, VoidLoaderInstaller.progressFraction(0.75f), 0.0001f)
        assertEquals(1f, VoidLoaderInstaller.progressFraction(1.4f), 0f)
    }

    @Test
    fun jvmArgsDeduplicateAndAppendPreset() {
        val merged = VoidEngineBridge.mergeJvmArgs("-Xmx2G -XX:+UseG1GC", "-XX:+UseG1GC -XX:MaxGCPauseMillis=40")
        assertEquals("-Xmx2G -XX:+UseG1GC -XX:MaxGCPauseMillis=40", merged)
    }

    @Test
    fun presetIdsResolveWithFallback() {
        assertEquals(VoidPerformancePreset.PERFORMANCE, VoidPerformancePreset.fromId("performance"))
        assertEquals(VoidPerformancePreset.BALANCED, VoidPerformancePreset.fromId("nonsense"))
        assertEquals(VoidPerformancePreset.BATTERY, VoidPerformancePreset.fromId("battery"))
    }
}
