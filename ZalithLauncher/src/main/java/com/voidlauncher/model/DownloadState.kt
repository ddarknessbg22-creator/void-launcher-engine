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
    val skipped: Int = 0,
    /** Bytes per second, measured over the last second. */
    val bytesPerSecond: Long = 0L,
    /** Seconds remaining, or -1 when unknown. */
    val etaSeconds: Long = -1L,
) {
    val isFailure: Boolean get() = !active && error != null

    /** "1.2 MB/s" or "" when no sample yet. */
    val speedText: String
        get() = if (bytesPerSecond <= 0L) "" else "${formatSpeed(bytesPerSecond)}/s"

    val etaText: String
        get() = when {
            etaSeconds < 0L -> ""
            etaSeconds < 60L -> "${etaSeconds}s left"
            etaSeconds < 3600L -> "${etaSeconds / 60}m ${etaSeconds % 60}s left"
            else -> "${etaSeconds / 3600}h ${(etaSeconds % 3600) / 60}m left"
        }

    companion object {
        fun formatSpeed(bytesPerSecond: Long): String = when {
            bytesPerSecond < 1024L -> "$bytesPerSecond B"
            bytesPerSecond < 1024L * 1024L -> "%.1f KB".format(bytesPerSecond / 1024.0)
            else -> "%.1f MB".format(bytesPerSecond / (1024.0 * 1024.0))
        }
    }
}
