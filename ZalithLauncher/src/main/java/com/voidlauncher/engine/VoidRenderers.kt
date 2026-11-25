package com.voidlauncher.engine

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
    fun options(): List<RendererOption> = runCatching {
        Renderers.init()
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
    // A renderer is usable when its required native library is present in this APK.
    val library = getRendererLibrary()
    if (library.isBlank()) true
    else java.io.File(
        com.movtery.zalithlauncher.path.PathManager.DIR_NATIVE_LIB,
        if (library.startsWith("lib")) library else "lib$library.so",
    ).exists()
}.getOrDefault(false)
