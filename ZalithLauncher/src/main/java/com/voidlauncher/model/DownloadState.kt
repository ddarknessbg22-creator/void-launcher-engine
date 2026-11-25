package com.voidlauncher.model

/** Snapshot of a live installation job. Emitted by VersionRepository. */
data class DownloadState(
    val active: Boolean = false,
    val versionId: String = "",
    val stage: String = "",
    val currentFile: String = "",
    val filesDone: Int = 0,
    val filesTotal: Int = 0,
    val bytesDone: Long = 0L,
    val bytesTotal: Long = 0L,
    val percent: Float = 0f,
    val success: Boolean = false,
    val error: String? = null,
) {
    val isFailure: Boolean get() = !active && error != null
}
