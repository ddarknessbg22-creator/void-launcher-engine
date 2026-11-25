package com.voidlauncher.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class VersionManifest(
    val latest: LatestVersion,
    val versions: List<VersionSummary>,
)

@Serializable
data class LatestVersion(val release: String, val snapshot: String)

@Serializable
data class VersionSummary(
    val id: String,
    val type: String,
    val url: String,
    val time: String = "",
    val releaseTime: String = "",
    val sha1: String? = null,
    val complianceLevel: Int? = null,
)

@Serializable
data class VersionDetails(
    val id: String? = null,
    val type: String? = null,
    val assets: String? = null,
    @SerialName("assetIndex")
    val assetsIndex: DownloadArtifactRef? = null,
    val mainClass: String? = null,
    val inheritsFrom: String? = null,
    val javaVersion: JavaVersionInfo? = null,
    val libraries: List<Library> = emptyList(),
    val arguments: LaunchArguments? = null,
    val minecraftArguments: String? = null,
    val downloads: GameDownloads? = null,
    val releaseTime: String? = null,
)

@Serializable
data class DownloadArtifactRef(
    val url: String,
    val sha1: String? = null,
    val size: Long? = null,
)

@Serializable
data class JavaVersionInfo(val component: String, val majorVersion: Int)

@Serializable
data class Library(
    val name: String? = null,
    val downloads: LibraryDownloads? = null,
    val rules: List<LibraryRule>? = null,
    val natives: Map<String, String>? = null,
    val extract: ExtractRule? = null,
    val url: String? = null,
    val checksums: Map<String, String>? = null,
)

@Serializable
data class LibraryDownloads(
    val artifact: DownloadArtifact? = null,
    val classifiers: Map<String, DownloadArtifact>? = null,
)

@Serializable
data class DownloadArtifact(
    val path: String? = null,
    val url: String,
    val sha1: String? = null,
    val size: Long? = null,
)

@Serializable
data class LibraryRule(
    val action: String,
    val os: RuleOs? = null,
)

@Serializable
data class RuleOs(
    val name: String? = null,
    val version: String? = null,
    val arch: String? = null,
)

@Serializable
data class ExtractRule(val exclude: List<String> = emptyList())

@Serializable
data class LaunchArguments(
    // Mojang mixes plain strings with {"rules": [...], "value": ...} objects here.
    val game: List<JsonElement> = emptyList(),
    val jvm: List<JsonElement> = emptyList(),
)

@Serializable
data class GameDownloads(
    val client: DownloadArtifact? = null,
    val server: DownloadArtifact? = null,
    val client_mappings: DownloadArtifact? = null,
    val server_mappings: DownloadArtifact? = null,
)

@Serializable
data class AssetIndex(
    val objects: Map<String, AssetObject> = emptyMap(),
)

@Serializable
data class AssetObject(val hash: String, val size: Long)

/** A version that has actually been downloaded and verified on this device. */
@Serializable
data class InstalledVersion(
    val id: String,
    val jsonPath: String,
    val jarPath: String,
    val installedAt: Long,
    val libraryCount: Int,
    val assetCount: Int,
    val totalBytes: Long,
    val loader: String = "vanilla",
    val mainClass: String? = null,
    val javaMajor: Int? = null,
)
