package com.voidlauncher.data

import android.content.Context
import android.net.Uri
import com.voidlauncher.model.ModEntry
import com.voidlauncher.util.VoidPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

/** Real mod-directory management: detects loaders from jar metadata and toggles mods by renaming. */
class ModsRepository(context: Context) {
    private val app = context.applicationContext
    private val root = VoidPaths.root(app)

    fun directory(versionId: String): File = VoidPaths.mods(root, versionId)

    fun list(versionId: String): List<ModEntry> = runCatching {
        directory(versionId).listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".jar") || it.name.endsWith(".jar.disabled")) }
            ?.map { file ->
                val enabled = file.name.endsWith(".jar")
                val (loader, modVersion) = detect(file)
                ModEntry(
                    name = file.name.removeSuffix(".disabled").removeSuffix(".jar")
                        .replace('_', ' ').replace('-', ' '),
                    fileName = file.name,
                    enabled = enabled,
                    loader = loader,
                    version = modVersion,
                    sizeBytes = file.length(),
                )
            }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }.getOrElse { emptyList() }

    private fun detect(file: File): Pair<String, String?> = runCatching {
        ZipFile(file).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toSet()
            when {
                "fabric.mod.json" in names -> {
                    val text = zip.getInputStream(zip.getEntry("fabric.mod.json"))
                        .bufferedReader(Charsets.UTF_8).use { it.readText().take(4096) }
                    "Fabric" to Regex("\"version\"\\s*:\\s*\"([^\"]+)\"")
                        .find(text)?.groupValues?.get(1)
                }
                "neoforge.mods.toml" in names -> "NeoForge" to null
                "META-INF/mods.toml" in names -> "Forge" to null
                "mcmod.info" in names -> "Forge (legacy)" to null
                else -> "Unknown" to null
            }
        }
    }.getOrElse { "Unknown" to null }

    fun toggle(versionId: String, fileName: String): Result<Boolean> = runCatching {
        val dir = directory(versionId)
        val source = File(dir, fileName)
        require(source.exists()) { "Mod file not found" }
        val targetName = if (fileName.endsWith(".jar.disabled"))
            fileName.removeSuffix(".disabled")
        else "$fileName.disabled"
        val target = File(dir, targetName)
        require(!target.exists()) { "A file with the target name already exists" }
        require(source.renameTo(target)) { "Could not rename mod file" }
        targetName.endsWith(".jar")
    }

    fun delete(versionId: String, fileName: String): Boolean =
        runCatching { File(directory(versionId), fileName).delete() }.getOrDefault(false)

    suspend fun import(versionId: String, uri: Uri, displayName: String): Result<ModEntry> =
        withContext(Dispatchers.IO) {
            runCatching {
                val name = displayName.substringAfterLast('/').ifBlank { "mod.jar" }
                require(name.endsWith(".jar")) { "Only .jar mod files are supported" }
                val target = File(directory(versionId), name)
                app.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Could not read the selected file")
                val (loader, version) = detect(target)
                ModEntry(
                    name = name.removeSuffix(".jar"),
                    fileName = name,
                    enabled = true,
                    loader = loader,
                    version = version,
                    sizeBytes = target.length(),
                )
            }
        }

    /** Live loader release data from Fabric's public metadata API — no hardcoded fake versions. */
    suspend fun fabricLoaderVersions(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val text = com.voidlauncher.util.Net.fetchText("https://meta.fabricmc.net/v2/versions/loader")
            val array = org.json.JSONArray(text)
            (0 until array.length()).map { array.getJSONObject(it).getString("version") }
        }
    }
}
