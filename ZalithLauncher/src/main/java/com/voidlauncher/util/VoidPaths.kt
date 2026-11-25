package com.voidlauncher.util

import android.content.Context
import java.io.File

/** All game data lives in VOID's own app-specific external directory: no root, no broad storage permission. */
object VoidPaths {
    fun root(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "VOID").apply { mkdirs() }

    fun versions(root: File): File = File(root, "versions").apply { mkdirs() }
    fun libraries(root: File): File = File(root, "libraries").apply { mkdirs() }
    fun assets(root: File): File = File(root, "assets").apply { mkdirs() }
    fun indexes(root: File): File = File(assets(root), "indexes").apply { mkdirs() }
    fun objects(root: File): File = File(assets(root), "objects").apply { mkdirs() }
    fun natives(root: File): File = File(root, "natives").apply { mkdirs() }
    fun runtimes(root: File): File = File(root, "runtimes").apply { mkdirs() }
    fun logs(root: File): File = File(root, "logs").apply { mkdirs() }
    fun exports(root: File): File = File(root, "exports").apply { mkdirs() }
    fun mods(root: File, versionId: String): File = File(versions(root), "$versionId/mods").apply { mkdirs() }
    fun versionDir(root: File, versionId: String): File = File(versions(root), versionId).apply { mkdirs() }
}
