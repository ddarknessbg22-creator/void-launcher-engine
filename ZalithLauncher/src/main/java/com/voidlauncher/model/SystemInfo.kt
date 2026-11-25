package com.voidlauncher.model

import kotlinx.serialization.Serializable

@Serializable
data class JavaRuntime(
    val id: String,
    val path: String,
    val version: String,
    val majorVersion: Int,
    val vendor: String = "",
    val source: String = "",
    val valid: Boolean = true,
    val detectedAt: Long = System.currentTimeMillis(),
)

data class EngineInfo(
    val packageName: String,
    val displayName: String,
    val installed: Boolean,
    val versionName: String? = null,
    val launchActivity: String? = null,
    val note: String = "",
)

data class ModEntry(
    val name: String,
    val fileName: String,
    val enabled: Boolean,
    val loader: String,
    val version: String? = null,
    val sizeBytes: Long = 0L,
)

data class GameFeature(
    val name: String,
    val description: String,
    val status: FeatureStatus,
)

enum class FeatureStatus {
    VERIFIED,
    BUILT_NOT_DEVICE_VERIFIED,
    BLOCKED,
    NOT_IMPLEMENTED,
}
