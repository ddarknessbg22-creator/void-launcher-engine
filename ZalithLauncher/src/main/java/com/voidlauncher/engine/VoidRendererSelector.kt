package com.voidlauncher.engine

import android.content.Context
import com.movtery.zalithlauncher.game.plugin.Plugin
import com.movtery.zalithlauncher.game.plugin.PluginLoader
import com.movtery.zalithlauncher.game.renderer.RendererInterface
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.RendererSettings
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File

/** One renderer the device can actually load, with the Minecraft range it supports. */
data class RendererCandidate(
    val id: String,
    val name: String,
    val available: Boolean,
    val minVersion: String? = null,
    val maxVersion: String? = null,
    val library: String? = null,
)

/** The renderer a launch will use, and why. */
data class RendererSelection(
    val id: String,
    val name: String,
    val reason: String,
    val loadable: List<String>,
)

/**
 * Picks the renderer that will actually be fast AND actually loadable for a Minecraft version.
 *
 * Two bugs made this necessary:
 *  - the registry's first entry is the modern "Krypton Wrapper" (NGGL4ES); used on 1.12.2 it has no
 *    immediate-mode translation and the game renders at ~1 FPS;
 *  - availability was only ever used to grey out a settings row, never to gate a launch, so a
 *    renderer whose native library is missing could still be selected — and
 *    `Renderers.setCurrentRenderer(id, retryToFirstOnFailure = false)` then leaves no current
 *    renderer at all, which makes the next `getCurrentRenderer()` throw.
 */
object VoidRendererSelector {
    private const val TAG = "VoidRendererSelector"

    /** Preference order per Minecraft generation. */
    private val MODERN = listOf("mobileglues", "gl4es", "krypton", "zink", "virgl", "freedreno", "panfrost")
    private val LEGACY = listOf("gl4es", "mobileglues", "krypton", "zink", "virgl", "freedreno", "panfrost")

    /**
     * Pure selection: [candidates] in registry order, [minecraftVersion] like "1.12.2",
     * [preferredId] the user's explicit choice (honoured when usable).
     * Returns the chosen renderer id, or null when nothing is loadable.
     */
    fun choose(
        candidates: List<RendererCandidate>,
        minecraftVersion: String?,
        preferredId: String?,
    ): String? {
        val usable = candidates.filter { it.available }
        if (usable.isEmpty()) return null

        // An explicit user choice wins as long as it can load and supports this version.
        usable.firstOrNull { it.id == preferredId && supports(it, minecraftVersion) }?.let { return it.id }

        val order = if (isModern(minecraftVersion)) MODERN else LEGACY
        for (key in order) {
            usable.firstOrNull { candidate ->
                candidate.id.contains(key, ignoreCase = true) && supports(candidate, minecraftVersion)
            }?.let { return it.id }
        }

        // Nothing matched by name: take the first candidate that supports the version.
        return usable.firstOrNull { supports(it, minecraftVersion) }?.id ?: usable.first().id
    }

    /** 1.17 and newer use the modern path; 1.16.5 and older need GL4ES. */
    fun isModern(minecraftVersion: String?): Boolean {
        val parts = versionParts(minecraftVersion) ?: return true
        return parts.first > 1 || (parts.first == 1 && parts.second >= 17)
    }

    private fun supports(candidate: RendererCandidate, minecraftVersion: String?): Boolean {
        val version = versionParts(minecraftVersion) ?: return true
        val min = versionParts(candidate.minVersion)
        val max = versionParts(candidate.maxVersion)
        if (min != null && compare(version, min) < 0) return false
        if (max != null && compare(version, max) > 0) return false
        return true
    }

    /** "1.12.2" -> [1, 12]. Snapshots and unknown strings return null. */
    private fun versionParts(version: String?): List<Int>? {
        val cleaned = version?.trim()?.removePrefix("release/") ?: return null
        val numbers = cleaned.split('.').mapNotNull { it.toIntOrNull() }
        return if (numbers.size >= 2) numbers else null
    }

    private fun compare(a: List<Int>, b: List<Int>): Int {
        for (index in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrElse(index) { 0 }
            val right = b.getOrElse(index) { 0 }
            if (left != right) return left - right
        }
        return 0
    }

    /**
     * Every renderer in the registry with a real on-device availability check.
     *
     * [context] is required: it loads renderer plugins so a plugin installed after first launch is
     * considered, and it is the only way to reach a plugin's own nativeLibraryDir.
     */
    fun loadable(context: Context?): List<RendererCandidate> {
        // init() without reset: a reset clears the registry, which would also drop every renderer
        // contributed by an installed renderer plugin.
        runCatching { Renderers.init() }
        runCatching { context?.let { PluginLoader.loadAllPlugins(it) } }
        val renderers = runCatching { Renderers.getRenderers() }.getOrDefault(emptyList())
        return renderers.map { renderer ->
            val library = runCatching { renderer.getRendererLibrary() }.getOrNull().orEmpty()
            RendererCandidate(
                id = renderer.getUniqueIdentifier(),
                name = renderer.getRendererName(),
                available = isLibraryPresent(renderer, library),
                minVersion = runCatching { renderer.getMinMCVersion() }.getOrNull(),
                maxVersion = runCatching { renderer.getMaxMCVersion() }.getOrNull(),
                library = library.takeIf { it.isNotBlank() },
            )
        }
    }

    /**
     * True when the renderer's native library is present, or when it needs none (a plain
     * passthrough renderer reports an empty library name).
     */
    fun isLibraryPresent(renderer: RendererInterface, library: String = runCatching { renderer.getRendererLibrary() }.getOrDefault("")): Boolean {
        if (library.isBlank()) return true
        val directory = (renderer as? Plugin)?.getNativeLibPath()?.takeIf { it.isNotBlank() }
            ?: runCatching { PathManager.DIR_NATIVE_LIB }.getOrNull()
            ?: return false
        val fileName = if (library.startsWith("lib")) library else "lib$library.so"
        return File(directory, fileName).isFile
    }

    /**
     * Registry-backed selection with a hard availability check. Returns null only when no renderer
     * can load, so the caller can report it instead of launching into a broken GL path.
     */
    fun bestFor(context: Context?, minecraftVersion: String?, preferredId: String? = null): RendererSelection? {
        val candidates = loadable(context)
        val loadableIds = candidates.filter { it.available }.map { it.id }
        val chosen = choose(candidates, minecraftVersion, preferredId) ?: run {
            Logger.error(TAG, "No loadable renderer for $minecraftVersion (registry: ${candidates.map { it.id to it.available }})")
            return null
        }
        val name = candidates.firstOrNull { it.id == chosen }?.name ?: chosen
        val reason = when {
            preferredId == chosen -> "your choice"
            isModern(minecraftVersion) -> "best for $minecraftVersion"
            else -> "required for $minecraftVersion (legacy GL)"
        }
        Logger.info(TAG, "Renderer $name ($chosen) selected for $minecraftVersion: $reason")
        return RendererSelection(chosen, name, reason, loadableIds)
    }

    /** The user's saved preference, or null when they have not chosen one. */
    fun preferred(): String? = runCatching { RendererSettings.renderer.getValue() }
        .getOrNull()?.takeIf { it.isNotBlank() }

    fun save(uniqueId: String) = runCatching { RendererSettings.renderer.save(uniqueId) }

    /**
     * Applies [selection] to the engine registry. Never leaves the registry without a current
     * renderer: an unknown id is ignored rather than clearing the selection.
     */
    fun apply(selection: RendererSelection): Boolean = runCatching {
        val known = Renderers.getRenderers().any { it.getUniqueIdentifier() == selection.id }
        if (!known) {
            Logger.error(TAG, "Refusing to select unknown renderer ${selection.id}")
            return false
        }
        Renderers.setCurrentRenderer(selection.id, retryToFirstOnFailure = true)
        save(selection.id)
        true
    }.getOrDefault(false)
}
