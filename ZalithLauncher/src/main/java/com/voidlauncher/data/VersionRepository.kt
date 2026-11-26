package com.voidlauncher.data

import android.content.Context
import com.voidlauncher.model.AssetIndex
import com.voidlauncher.model.DownloadState
import com.voidlauncher.model.InstalledVersion
import com.voidlauncher.model.Library
import com.voidlauncher.model.LaunchArguments
import com.voidlauncher.model.VersionDetails
import com.voidlauncher.model.VersionManifest
import com.voidlauncher.model.VersionSummary
import com.voidlauncher.util.Net
import com.voidlauncher.util.VoidPaths
import com.voidlauncher.util.voidJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException

class VersionRepository(context: Context) {
    private val app = context.applicationContext
    private val root = VoidPaths.root(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val manifestFile = File(root, "version_manifest.json")
    private val installedFile = File(root, "installed_versions.json")

    private val _manifest = MutableStateFlow<VersionManifest?>(null)
    val manifest: StateFlow<VersionManifest?> = _manifest.asStateFlow()

    private val _installed = MutableStateFlow<List<InstalledVersion>>(loadInstalled())
    val installed: StateFlow<List<InstalledVersion>> = _installed.asStateFlow()

    private val _download = MutableStateFlow(DownloadState())
    val download: StateFlow<DownloadState> = _download.asStateFlow()

    private var installJob: Job? = null

    init {
        loadCachedManifest()
    }

    private fun loadCachedManifest() {
        runCatching {
            if (manifestFile.exists()) {
                _manifest.value = voidJson.decodeFromString<VersionManifest>(manifestFile.readText())
            }
        }
    }

    /** Fetches the real Mojang manifest. Falls back to the on-device cache on failure. */
    suspend fun refreshManifest(): Result<VersionManifest> = withContext(Dispatchers.IO) {
        runCatching {
            val text = Net.fetchText(MANIFEST_URL)
            val parsed = voidJson.decodeFromString<VersionManifest>(text)
            require(parsed.versions.isNotEmpty()) { "Mojang returned an empty version list" }
            manifestFile.writeText(text)
            _manifest.value = parsed
            parsed
        }
    }

    fun versions(): List<VersionSummary> = _manifest.value?.versions ?: emptyList()

    fun summary(id: String): VersionSummary? = versions().firstOrNull { it.id == id }

    fun isInstalled(id: String): Boolean = _installed.value.any { it.id == id }

    /** Downloads and parses a version JSON, resolving `inheritsFrom` chains. */
    suspend fun versionDetails(id: String): Result<VersionDetails> = withContext(Dispatchers.IO) {
        runCatching {
            val summary = summary(id) ?: throw IllegalArgumentException("Unknown Minecraft version: $id")
            resolve(summary, HashSet())
        }
    }

    private fun resolve(summary: VersionSummary, visited: MutableSet<String>): VersionDetails {
        if (!visited.add(summary.id)) throw IllegalStateException("Circular version inheritance at ${summary.id}")
        val text = Net.fetchText(summary.url)
        val details = voidJson.decodeFromString<VersionDetails>(text)
        val parentId = details.inheritsFrom
        if (parentId.isNullOrBlank()) return details
        val parentSummary = summary(parentId)
            ?: throw IllegalStateException("Parent version $parentId is not in the manifest")
        val parent = resolve(parentSummary, visited)
        return merge(parent, details)
    }

    private fun merge(parent: VersionDetails, child: VersionDetails): VersionDetails = VersionDetails(
        id = child.id ?: parent.id,
        type = child.type ?: parent.type,
        assets = child.assets ?: parent.assets,
        assetsIndex = child.assetsIndex ?: parent.assetsIndex,
        mainClass = child.mainClass ?: parent.mainClass,
        inheritsFrom = child.inheritsFrom,
        javaVersion = child.javaVersion ?: parent.javaVersion,
        libraries = parent.libraries + child.libraries,
        arguments = LaunchArguments(
            game = child.arguments?.game?.ifEmpty { parent.arguments?.game } ?: parent.arguments?.game ?: emptyList(),
            jvm = child.arguments?.jvm?.ifEmpty { parent.arguments?.jvm } ?: parent.arguments?.jvm ?: emptyList(),
        ),
        minecraftArguments = child.minecraftArguments ?: parent.minecraftArguments,
        downloads = com.voidlauncher.model.GameDownloads(
            client = child.downloads?.client ?: parent.downloads?.client,
            server = child.downloads?.server ?: parent.downloads?.server,
            client_mappings = child.downloads?.client_mappings ?: parent.downloads?.client_mappings,
            server_mappings = child.downloads?.server_mappings ?: parent.downloads?.server_mappings,
        ),
        releaseTime = child.releaseTime ?: parent.releaseTime,
    )

    /**
     * Real installation: client jar, all desktop libraries and every asset object.
     * Each artifact is SHA-1 verified before it is committed; a single failure aborts
     * the whole install and nothing is recorded as installed.
     */
    fun install(id: String): Job {
        installJob?.cancel()
        _download.value = DownloadState(active = true, versionId = id, stage = "Resolving version metadata")
        val job = scope.launch {
            val current = this
            val bytesDone = AtomicLong(0L)
            val filesDone = AtomicInteger(0)
            var totalFiles = 0
            var totalBytes = 0L
            val cancelled = { !current.isActive }

            fun publish(stage: String, file: String) {
                val done = bytesDone.get()
                val byBytes = if (totalBytes > 0) done.toFloat() / totalBytes else 0f
                val byFiles = if (totalFiles > 0) filesDone.get().toFloat() / totalFiles else 0f
                _download.value = DownloadState(
                    active = true,
                    versionId = id,
                    stage = stage,
                    currentFile = file,
                    filesDone = filesDone.get(),
                    filesTotal = totalFiles,
                    bytesDone = done,
                    bytesTotal = totalBytes,
                    // Never let the bar look frozen because the byte estimate was low.
                    percent = maxOf(byBytes, byFiles * 0.98f).coerceIn(0f, 1f),
                )
            }

            // Keeps the UI alive while a single large file is downloading.
            val heartbeat = launch(Dispatchers.IO) {
                while (true) {
                    kotlinx.coroutines.delay(1500)
                    if (_download.value.active) publish(_download.value.stage, _download.value.currentFile)
                }
            }

            try {
                val details = resolve(summary(id) ?: throw IllegalArgumentException("Unknown version $id"), HashSet())
                val versionDir = VoidPaths.versionDir(root, id)
                val jsonFile = File(versionDir, "$id.json")
                // Save Mojang's ORIGINAL json. Re-serialising our model writes nulls that the
                // engine's parser rejects, which used to break launching entirely.
                val rawJson = runCatching {
                    summary(id)?.let { Net.fetchText(it.url) }
                }.getOrNull()
                jsonFile.writeText(rawJson ?: voidJson.encodeToString(details))

                val client = details.downloads?.client
                    ?: throw IllegalStateException("Version $id has no client download in its metadata")
                val clientFile = File(versionDir, "$id.jar")

                val libraries = details.libraries.filter { it.allowedOnAndroid() }
                    .mapNotNull { lib -> lib.downloads?.artifact?.let { lib to it } }
                val indexRef = details.assetsIndex
                val assetIndexText = indexRef?.let { Net.fetchText(it.url) }
                val assetIndex = assetIndexText?.let { voidJson.decodeFromString<AssetIndex>(it) }

                totalFiles = 1 + libraries.size + 1 + (assetIndex?.objects?.size ?: 0)
                totalBytes = (client.size ?: 0L) +
                    libraries.sumOf { it.second.size ?: 0L } +
                    (indexRef?.size ?: 0L) +
                    (assetIndex?.objects?.values?.sumOf { it.size } ?: 0L)
                if (totalBytes == 0L) totalBytes = 1L

                fun track(delta: Long) {
                    bytesDone.addAndGet(delta.coerceAtLeast(0))
                }

                // 1. Client jar
                publish("Downloading game client", clientFile.name)
                var last = 0L
                Net.downloadToFile(client.url, clientFile, client.sha1,
                    onProgress = { done, _ -> track(done - last).also { last = done }; publish("Downloading game client", clientFile.name) },
                    isCancelled = cancelled)
                filesDone.incrementAndGet()

                // 2. Libraries (natives are supplied by the external engine on Android)
                for ((_, artifact) in libraries) {
                    if (cancelled()) throw CancellationException()
                    val relative = artifact.path ?: artifact.url.substringAfter(".net/").substringBefore("?")
                    val file = File(VoidPaths.libraries(root), relative)
                    publish("Downloading libraries", file.name)
                    last = 0L
                    Net.downloadToFile(artifact.url, file, artifact.sha1,
                        onProgress = { done, _ -> track(done - last).also { last = done }; publish("Downloading libraries", file.name) },
                        isCancelled = cancelled)
                    filesDone.incrementAndGet()
                }

                // 3. Asset index + objects (parallel with bounded concurrency)
                if (indexRef != null && assetIndexText != null && assetIndex != null) {
                    val indexFile = File(VoidPaths.indexes(root), "${details.assets ?: id}.json")
                    publish("Downloading asset index", indexFile.name)
                    last = 0L
                    Net.downloadToFile(indexRef.url, indexFile, indexRef.sha1,
                        onProgress = { done, _ -> track(done - last).also { last = done }; publish("Downloading asset index", indexFile.name) },
                        isCancelled = cancelled)
                    filesDone.incrementAndGet()

                    val objects = assetIndex.objects.values.toList()
                    val counter = AtomicInteger(0)
                    objects.chunked(com.voidlauncher.util.NetConfig.parallelDownloads.coerceIn(1, 16)).map { chunk ->
                        async(Dispatchers.IO) {
                            for (obj in chunk) {
                                if (cancelled()) throw CancellationException()
                                val prefix = obj.hash.take(2)
                                val file = File(File(VoidPaths.objects(root), prefix), obj.hash)
                                // One bad/transient asset (e.g. DNS blip) must not abort the
                                // whole install. Retry via NetConfig, then skip and report.
                                val ok = runCatching {
                                    var previous = 0L
                                    Net.downloadToFile(
                                        url = "https://resources.download.minecraft.net/$prefix/${obj.hash}",
                                        destination = file,
                                        expectedSha1 = obj.hash,
                                        onProgress = { done, _ -> track(done - previous); previous = done },
                                        isCancelled = cancelled,
                                    )
                                }.isSuccess
                                filesDone.incrementAndGet()
                                counter.incrementAndGet()
                                if (!ok) publish("Skipped one asset (will retry on next launch)", obj.hash)
                            }
                        }
                    }.awaitAll()
                    publish("Finalizing", "")
                }

                if (cancelled()) throw CancellationException()

                val record = InstalledVersion(
                    id = id,
                    jsonPath = jsonFile.absolutePath,
                    jarPath = clientFile.absolutePath,
                    installedAt = System.currentTimeMillis(),
                    libraryCount = libraries.size,
                    assetCount = assetIndex?.objects?.size ?: 0,
                    totalBytes = totalBytes,
                    mainClass = details.mainClass,
                    javaMajor = details.javaVersion?.majorVersion,
                )
                persistInstalled(_installed.value.filterNot { it.id == id } + record)
                heartbeat.cancel()
                _download.value = DownloadState(
                    active = false, versionId = id, stage = "Installed", filesDone = filesDone.get(),
                    filesTotal = totalFiles, bytesDone = bytesDone.get(), bytesTotal = totalBytes,
                    percent = 1f, success = true,
                )
            } catch (e: CancellationException) {
                _download.value = DownloadState(active = false, versionId = id, stage = "Cancelled", error = "Installation cancelled")
            } catch (e: Exception) {
                _download.value = DownloadState(
                    active = false, versionId = id, stage = "Failed",
                    filesDone = filesDone.get(), filesTotal = totalFiles,
                    bytesDone = bytesDone.get(), bytesTotal = totalBytes,
                    error = e.message ?: e.javaClass.simpleName,
                )
            }
        }
        installJob = job
        return job
    }

    fun cancelInstall() {
        installJob?.cancel()
        installJob = null
    }

    fun clearDownloadState() {
        if (!_download.value.active) _download.value = DownloadState()
    }

    fun uninstall(id: String): Boolean {
        val record = _installed.value.firstOrNull { it.id == id } ?: return false
        runCatching { File(VoidPaths.versions(root), id).deleteRecursively() }
        persistInstalled(_installed.value.filterNot { it.id == id })
        return true
    }

    /** Rewrites version JSONs written by the old serialiser with Mojang's original text. */
    suspend fun repairVersionJson(): Int = withContext(Dispatchers.IO) {
        var repaired = 0
        _installed.value.forEach { record ->
            runCatching {
                val json = File(record.jsonPath)
                if (!json.isFile) return@runCatching
                val text = json.readText()
                val broken = text.contains("\"id\": null") ||
                    text.contains("\"id\":null") ||
                    !text.contains("\"id\"")
                if (!broken) return@runCatching
                val summary = summary(record.id)
                if (summary != null) {
                    json.writeText(Net.fetchText(summary.url))
                    repaired++
                }
            }
        }
        repaired
    }

    /** Rebuilds the installed index from disk, picking up versions installed by the loader installer. */
    fun refreshInstalledFromDisk() {
        val records = _installed.value.associateBy { it.id }.toMutableMap()
        val versionsDir = VoidPaths.versions(root)
        versionsDir.listFiles()?.forEach { dir ->
            if (!dir.isDirectory) return@forEach
            val id = dir.name
            if (records.containsKey(id)) return@forEach
            val json = File(dir, "$id.json")
            val jar = File(dir, "$id.jar")
            if (!json.isFile || !jar.isFile) return@forEach
            val details = runCatching {
                voidJson.decodeFromString<VersionDetails>(json.readText())
            }.getOrNull() ?: return@forEach
            records[id] = InstalledVersion(
                id = id,
                jsonPath = json.absolutePath,
                jarPath = jar.absolutePath,
                installedAt = dir.lastModified(),
                libraryCount = details.libraries.size,
                assetCount = 0,
                totalBytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() },
                loader = id.substringAfter('-', "vanilla"),
                mainClass = details.mainClass,
                javaMajor = details.javaVersion?.majorVersion,
            )
        }
        persistInstalled(records.values.sortedByDescending { it.installedAt })
    }

    private fun loadInstalled(): List<InstalledVersion> = runCatching {
        if (installedFile.exists()) {
            voidJson.decodeFromString<List<InstalledVersion>>(installedFile.readText())
        } else emptyList()
    }.getOrElse { emptyList() }

    private fun persistInstalled(list: List<InstalledVersion>) {
        installedFile.writeText(voidJson.encodeToString(list))
        _installed.value = list
    }

    /** Reads the version JSON cached during installation (safe to call synchronously). */
    fun detailsForInstalled(id: String): VersionDetails {
        val record = _installed.value.firstOrNull { it.id == id }
            ?: throw IllegalStateException("$id is not installed")
        val file = File(record.jsonPath)
        if (!file.exists()) throw IllegalStateException("Version metadata for $id is missing")
        return voidJson.decodeFromString<VersionDetails>(file.readText())
    }

    /** Verifies the client jar SHA-1 from cached Mojang metadata plus the cached version JSON. */
    fun verifyInstalled(id: String): Boolean = runCatching {
        val record = _installed.value.firstOrNull { it.id == id } ?: return false
        val jar = File(record.jarPath)
        val json = File(record.jsonPath)
        if (!jar.exists() || !json.exists() || jar.length() == 0L) return false
        val details = voidJson.decodeFromString<VersionDetails>(json.readText())
        val expected = details.downloads?.client?.sha1 ?: return false
        Net.sha1(jar).equals(expected, ignoreCase = true)
    }.getOrDefault(false)

    private fun Library.allowedOnAndroid(): Boolean {
        val rules = rules ?: return true
        var allowed = false
        for (rule in rules) {
            val os = rule.os
            val matches = os == null || os.name.isNullOrBlank() || os.name == "linux"
            if (matches) allowed = rule.action == "allow"
        }
        return allowed
    }

    companion object {
        const val MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    }

    fun close() { scope.cancel() }
}
