package com.voidlauncher.engine

import android.content.Context
import com.movtery.zalithlauncher.game.launch.MCOptions
import com.movtery.zalithlauncher.game.version.installed.Version

/**
 * In-game performance profiles.
 *
 * Minecraft on Android is fill-rate and chunk-update bound long before it is CPU bound, so the
 * biggest wins come from render distance, simulation distance, particles, clouds and resolution —
 * not from the JVM. These presets write the real `options.txt` the game reads, so they take effect
 * on the next launch without the user hunting through in-game menus.
 */
enum class VoidPerformancePreset(
    val id: String,
    val label: String,
    val detail: String,
    val renderDistance: Int,
    val simulationDistance: Int,
    val maxFps: Int,
    val graphicsMode: String,
    val particles: String,
    val clouds: Boolean,
    val entityDistance: Double,
    val mipmapLevels: Int,
    val ao: Int,
    val vsync: Boolean,
    /** Percentage of the physical screen the game renders at. */
    val resolutionPercent: Int,
    val jvmArgs: String,
) {
    BATTERY(
        id = "battery",
        label = "Battery",
        detail = "Lowest GPU load. 4 chunks, 30 FPS cap. Best for weak or hot devices.",
        renderDistance = 4,
        simulationDistance = 4,
        maxFps = 30,
        graphicsMode = "fast",
        particles = "minimal",
        clouds = false,
        entityDistance = 0.5,
        mipmapLevels = 0,
        ao = 0,
        vsync = false,
        resolutionPercent = 55,
        jvmArgs = "-XX:+UseG1GC -XX:MaxGCPauseMillis=60 -XX:ActiveProcessorCount=4",
    ),
    PERFORMANCE(
        id = "performance",
        label = "Performance",
        detail = "Smooth frame times. 6 chunks, 60 FPS cap, fast graphics, no clouds.",
        renderDistance = 6,
        simulationDistance = 5,
        maxFps = 60,
        graphicsMode = "fast",
        particles = "minimal",
        clouds = false,
        entityDistance = 0.5,
        mipmapLevels = 2,
        ao = 1,
        vsync = false,
        resolutionPercent = 75,
        jvmArgs = "-XX:+UseG1GC -XX:MaxGCPauseMillis=40 -XX:G1NewSizePercent=30 -XX:+ParallelRefProcEnabled",
    ),
    BALANCED(
        id = "balanced",
        label = "Balanced",
        detail = "Default. 8 chunks, 60 FPS cap, fewer particles, mipmaps on.",
        renderDistance = 8,
        simulationDistance = 6,
        maxFps = 60,
        graphicsMode = "fast",
        particles = "decreased",
        clouds = true,
        entityDistance = 0.75,
        mipmapLevels = 4,
        ao = 2,
        vsync = false,
        resolutionPercent = 90,
        jvmArgs = "-XX:+UseG1GC -XX:MaxGCPauseMillis=50",
    ),
    QUALITY(
        id = "quality",
        label = "Quality",
        detail = "12 chunks, fancy graphics, full particles. Only for powerful devices.",
        renderDistance = 12,
        simulationDistance = 8,
        maxFps = 120,
        graphicsMode = "fancy",
        particles = "all",
        clouds = true,
        entityDistance = 1.0,
        mipmapLevels = 4,
        ao = 2,
        vsync = false,
        resolutionPercent = 100,
        jvmArgs = "-XX:+UseG1GC",
    );

    companion object {
        fun fromId(id: String?): VoidPerformancePreset =
            entries.firstOrNull { it.id == id } ?: BALANCED
    }
}

/**
 * Applies a performance preset to the version's real `options.txt`.
 *
 * Only keys the game actually reads are touched, and every value is written through the engine's
 * own [MCOptions] so the file stays in the format Minecraft expects.
 */
object VoidPerformance {
    /** Keys written by a preset. Used by tests and by the "reset" path. */
    val MANAGED_KEYS = listOf(
        "renderDistance",
        "simulationDistance",
        "maxFps",
        "graphicsMode",
        "particles",
        "renderClouds",
        "entityDistanceScaling",
        "mipmapLevels",
        "ao",
        "enableVsync",
        "guiScale",
        "fullscreen",
        "fov",
        "reducedDebugInfo",
        "prioritizeChunkUpdates",
    )

    /**
     * Writes [preset] into the version's options file.
     *
     * Safe to call before the game starts: [MCOptions.setup] creates the file from the bundled
     * defaults when it is missing, then the preset overwrites the managed keys and is saved.
     * Returns false when the file could not be written, so the caller can report it instead of
     * pretending the profile applied.
     */
    fun apply(context: Context, version: Version, preset: VoidPerformancePreset): Boolean = runCatching {
        MCOptions.setup(context, version)
        MCOptions.set("renderDistance", preset.renderDistance.toString())
        MCOptions.set("simulationDistance", preset.simulationDistance.toString())
        MCOptions.set("maxFps", preset.maxFps.toString())
        MCOptions.set("graphicsMode", preset.graphicsMode)
        MCOptions.set("particles", preset.particles)
        MCOptions.set("renderClouds", if (preset.clouds) "true" else "false")
        MCOptions.set("entityDistanceScaling", preset.entityDistance.toString())
        MCOptions.set("mipmapLevels", preset.mipmapLevels.toString())
        MCOptions.set("ao", preset.ao.toString())
        MCOptions.set("enableVsync", if (preset.vsync) "true" else "false")
        // 0 = auto (match the screen), which avoids a blurry upscale on high-density phones.
        MCOptions.set("guiScale", "0")
        MCOptions.set("fullscreen", "false")
        MCOptions.set("reducedDebugInfo", "true")
        // Chunk meshes are built on the render thread; deferring them removes the classic
        // "stutter while walking" spike without changing what is drawn.
        MCOptions.set("prioritizeChunkUpdates", "none")
        MCOptions.save()
        true
    }.getOrDefault(false)

    /** JVM arguments for [preset], appended after any user-supplied arguments. */
    fun jvmArgs(preset: VoidPerformancePreset): String = preset.jvmArgs
}
