package com.voidlauncher.engine

import android.content.Context
import com.movtery.zalithlauncher.game.plugin.renderer.RendererPluginManager
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.renderer.RendererInterface
import com.movtery.zalithlauncher.setting.RendererSettings

/** Real renderer list from the engine, with actual MobileGlues detection. */
data class RendererOption(
    val name: String,
    val uniqueId: String,
    val description: String,
    val available: Boolean,
    val isMobileGlues: Boolean,
)

object VoidRenderers {
    /** Plugins can be installed after VOID; always refresh the registry before listing. */
    fun refresh(context: Context) {
        runCatching {
            com.movtery.zalithlauncher.game.plugin.PluginLoader.loadAllPlugins(context, true)
        }
        runCatching { Renderers.init(reset = true) }
    }

    fun options(context: Context? = null): List<RendererOption> = runCatching {
        if (context != null) refresh(context)
        Renderers.getRenderers().map { renderer: RendererInterface ->
            RendererOption(
                name = renderer.getRendererName(),
                uniqueId = renderer.getUniqueIdentifier(),
                description = renderer.getRendererSummary().orEmpty(),
                available = renderer.isAvailable(),
                isMobileGlues = renderer.getRendererId().contains("mobileglues", true),
            )
        }
    }.getOrDefault(emptyList())

    fun current(): String = runCatching {
        Renderers.init()
        Renderers.getCurrentRenderer().getUniqueIdentifier()
    }.getOrDefault("")

    fun select(uniqueId: String) {
        runCatching {
            Renderers.init()
            val chosen = Renderers.setCurrentRenderer(uniqueId, retryToFirstOnFailure = false)
            RendererSettings.renderer.save(chosen.getUniqueIdentifier())
        }
    }

    fun hasMobileGlues(): Boolean = options().any { it.isMobileGlues && it.available }
}

private fun RendererInterface.isAvailable(): Boolean = runCatching {
    // Plugin renderers live in their own APK's nativeLibraryDir; built-ins use ours.
    val directory = (this as? com.movtery.zalithlauncher.game.plugin.Plugin)
        ?.getNativeLibPath()
        ?.takeIf { it.isNotBlank() }
        ?: com.movtery.zalithlauncher.path.PathManager.DIR_NATIVE_LIB
    val library = getRendererLibrary()
    if (library.isBlank()) true
    else {
        val fileName = if (library.startsWith("lib")) library else "lib$library.so"
        java.io.File(directory, fileName).exists()
    }
}.getOrDefault(false)
