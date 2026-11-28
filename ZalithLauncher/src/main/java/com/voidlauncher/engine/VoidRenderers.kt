package com.voidlauncher.engine

import android.content.Context
import com.movtery.zalithlauncher.game.plugin.PluginLoader
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.renderer.RendererInterface
import com.movtery.zalithlauncher.setting.RendererSettings

/** Real renderer list from the engine, with actual native-library detection. */
data class RendererOption(
    val name: String,
    val uniqueId: String,
    val description: String,
    val available: Boolean,
    val isMobileGlues: Boolean,
    val library: String,
)

object VoidRenderers {
    /**
     * Plugins can be installed after VOID, so refresh the registry before listing.
     *
     * Order matters: `Renderers.init(reset = true)` clears the registry, so loading plugins first
     * and then resetting threw every plugin renderer away. Reset first, then load plugins.
     */
    fun refresh(context: Context) {
        val previous = runCatching { Renderers.getCurrentRenderer().getUniqueIdentifier() }.getOrNull()
            ?: runCatching { RendererSettings.renderer.getValue() }.getOrNull().orEmpty()
        runCatching { Renderers.init(reset = true) }
        runCatching { PluginLoader.loadAllPlugins(context, true) }
        if (previous.isNotBlank()) {
            val restored = Renderers.getRenderers().firstOrNull { it.getUniqueIdentifier() == previous }
                ?: Renderers.getRenderers().firstOrNull()
            if (restored != null) {
                runCatching { Renderers.setCurrentRenderer(restored.getUniqueIdentifier(), retryToFirstOnFailure = false) }
            }
        }
    }

    fun options(context: Context? = null): List<RendererOption> = runCatching {
        if (context != null) refresh(context)
        Renderers.getRenderers().map { renderer: RendererInterface ->
            val library = runCatching { renderer.getRendererLibrary() }.getOrDefault("")
            RendererOption(
                name = renderer.getRendererName(),
                uniqueId = renderer.getUniqueIdentifier(),
                description = renderer.getRendererSummary().orEmpty(),
                available = VoidRendererSelector.isLibraryPresent(renderer, library),
                isMobileGlues = renderer.getRendererId().contains("mobileglues", true),
                library = library,
            )
        }
    }.getOrDefault(emptyList())

    fun current(): String = runCatching {
        Renderers.init()
        Renderers.getCurrentRenderer().getUniqueIdentifier()
    }.getOrDefault("")

    /**
     * Selects a renderer. Returns false when the id is unknown or its native library is missing,
     * so the caller can tell the user instead of leaving the registry with no current renderer.
     */
    fun select(uniqueId: String): Boolean = runCatching {
        Renderers.init()
        val known = Renderers.getRenderers().firstOrNull { it.getUniqueIdentifier() == uniqueId }
            ?: return false
        if (!VoidRendererSelector.isLibraryPresent(known)) return false
        val selected = runCatching {
            Renderers.setCurrentRenderer(uniqueId, retryToFirstOnFailure = true)
        }.getOrNull() ?: return false
        RendererSettings.renderer.save(selected.getUniqueIdentifier())
        true
    }.getOrDefault(false)

    fun hasMobileGlues(): Boolean = options().any { it.isMobileGlues && it.available }
}
