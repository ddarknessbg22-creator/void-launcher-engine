package com.voidlauncher.model

import kotlinx.serialization.Serializable

/** Launcher-level controls only. In-game video/audio settings are not configured here. */
@Serializable
data class AdvancedSettings(
    // Which native engine is preferred (this is a real effect: it changes the launched app).
    val glImplementation: String = "MobileGlues",

    // Java runtime selection.
    val javaMode: String = "Auto",
    val javaPath: String = "",

    // Automation & downloads.
    val autoInstallGame: Boolean = true,
    val verifyDownloads: Boolean = true,
    val autoRetry: Boolean = true,
    val downloadThreads: Int = 4,
)
