package com.voidlauncher.model

import kotlinx.serialization.Serializable

@Serializable
data class LaunchProfile(
    val id: String,
    val name: String,
    val accountId: String,
    val versionId: String,
    val memoryMb: Int = 2048,
    val jvmArgs: String = "",
    val loader: String = "Vanilla",
    val mods: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class ProfileStore(
    val profiles: List<LaunchProfile> = emptyList(),
    val selectedId: String? = null,
)

/** Launcher-level settings that affect how VOID starts the engine. */
@Serializable
data class LaunchSettings(
    val memoryMb: Int = 2048,
    val jvmArgs: String = "",
)
