package com.voidlauncher.data

import android.content.Context
import com.voidlauncher.model.DownloadState
import com.voidlauncher.model.JavaRuntime
import com.voidlauncher.model.VersionDetails
import com.voidlauncher.util.Net
import com.voidlauncher.util.VoidPaths
import com.voidlauncher.util.newId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/** Detects Java runtimes and downloads a real ARM64 JRE when one is missing. */
class JavaRepository(context: Context) {
    private val app = context.applicationContext
    private val root = VoidPaths.root(app)
    private val prefs = app.getSharedPreferences("void_java", Context.MODE_PRIVATE)

    private val _runtimes = MutableStateFlow<List<JavaRuntime>>(emptyList())
    val runtimes: StateFlow<List<JavaRuntime>> = _runtimes.asStateFlow()

    private val _download = MutableStateFlow(DownloadState())
    val download: StateFlow<DownloadState> = _download.asStateFlow()

    fun scan(): List<JavaRuntime> {
        val candidates = LinkedHashSet<File>()
        VoidPaths.runtimes(root).listFiles()?.forEach { dir ->
            if (!dir.isDirectory) return@forEach
            dir.walkTopDown().firstOrNull { it.name == "java" && it.parentFile?.name == "bin" }
                ?.let { candidates += it }
            candidates += File(dir, "bin/java")
        }
        customPaths().forEach { candidates += File(it) }
        System.getenv("JAVA_HOME")?.takeIf { it.isNotBlank() }?.let { candidates += File(it, "bin/java") }

        val found = candidates.mapNotNull { inspect(it) }
            .filter { isArm64(it.path) }   // reject x86 runtimes that can never run here
            .distinctBy { it.path }
            .sortedByDescending { it.majorVersion }
        _runtimes.value = found
        return found
    }

    /** Reads the ELF machine type from bin/java; ARM64 is 0xB7. */
    internal fun isArm64(path: String): Boolean = runCatching {
        java.io.RandomAccessFile(path, "r").use { f ->
            if (f.read() != 0x7f || f.read() != 'E'.code || f.read() != 'L'.code || f.read() != 'F'.code) return false
            f.seek(18)
            val machine = f.read() or (f.read() shl 8)
            machine == 0xB7
        }
    }.getOrDefault(false)

    private fun inspect(binary: File): JavaRuntime? {
        if (!binary.exists() || !binary.isFile) return null
        val home = (if (binary.parentFile?.name == "bin") binary.parentFile?.parentFile else binary.parentFile)
            ?: return null
        val release = File(home, "release")
        var version = ""
        var major = 0
        var vendor = ""
        if (release.exists()) {
            runCatching {
                release.readLines().forEach { line ->
                    when {
                        line.startsWith("JAVA_VERSION=") -> version = line.removePrefix("JAVA_VERSION=").trim().trim('"')
                        line.startsWith("IMPLEMENTOR=") -> vendor = line.removePrefix("IMPLEMENTOR=").trim().trim('"')
                        line.startsWith("JAVA_MAJOR_VERSION=") ->
                            major = line.removePrefix("JAVA_MAJOR_VERSION=").trim().trim('"').toIntOrNull() ?: 0
                    }
                }
            }
        }
        if (major == 0) major = majorFromVersion(version)
        if (major == 0) major = majorFromName(home.name)
        if (major == 0) return null
        return JavaRuntime(
            id = newId(), path = binary.absolutePath,
            version = version.ifBlank { "Java $major" }, majorVersion = major,
            vendor = vendor,
            source = if (binary.absolutePath.startsWith(VoidPaths.runtimes(root).absolutePath)) "Managed"
                     else if (customPaths().contains(binary.absolutePath)) "User added" else "Detected",
            valid = binary.canRead(),
        )
    }

    private fun majorFromVersion(version: String): Int =
        version.removePrefix("1.").substringBefore(".").substringBefore("+").toIntOrNull() ?: 0

    private fun majorFromName(name: String): Int {
        Regex("(?:jre|jdk)[-_]?(\\d+)", RegexOption.IGNORE_CASE).find(name)
            ?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        for (major in listOf(21, 17, 11, 8)) {
            if (Regex("(^|[^0-9])$major([^0-9]|$)").containsMatchIn(name)) return major
        }
        return 0
    }

    fun customPaths(): Set<String> = prefs.getStringSet("custom_paths", emptySet()) ?: emptySet()

    fun addCustomPath(path: String): Boolean {
        if (!File(path).exists()) return false
        prefs.edit().putStringSet("custom_paths", customPaths() + path).apply()
        scan(); return true
    }

    fun removeCustomPath(path: String) {
        prefs.edit().putStringSet("custom_paths", customPaths() - path).apply()
        scan()
    }

    fun selectFor(
        details: VersionDetails,
        available: List<JavaRuntime> = runtimes.value,
        forcedPath: String? = null,
    ): JavaRuntime? {
        if (!forcedPath.isNullOrBlank()) return available.firstOrNull { it.path == forcedPath }
        val required = requiredMajor(details)
        return available.filter { it.majorVersion >= required }.minByOrNull { it.majorVersion }
    }

    /** Downloads an ARM64 JRE from PojavLauncher's official multiarch release. */
    suspend fun downloadRuntime(component: String, major: Int): Result<JavaRuntime> =
        withContext(Dispatchers.IO) {
            runCatching {
                _download.value = DownloadState(active = true, versionId = "java-$major", stage = "Resolving ARM64 runtime")
                val releases = org.json.JSONArray(Net.fetchText(ANDROID_JRE_RELEASES))
                var url = ""
                for (i in 0 until releases.length()) {
                    val release = releases.getJSONObject(i)
                    val assets = release.optJSONArray("assets") ?: continue
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val name = asset.optString("name", "")
                        if (name.startsWith("jre$major-arm64") && name.endsWith(".tar.xz")) {
                            url = asset.getString("browser_download_url")
                            break
                        }
                    }
                    if (url.isNotBlank()) break
                }
                if (url.isBlank()) throw IllegalStateException("JRE $major ARM64 is not published yet")

                // If an ARM64 runtime of this major exists already, reuse it rather than re-downloading.
                scan().firstOrNull { it.majorVersion == major }?.let { return@runCatching it }

                val dir = File(VoidPaths.runtimes(root), "jre${major}-arm64-${newId().take(6)}")
                val archive = File(VoidPaths.runtimes(root), "jre${major}-arm64.tar.xz")
                if (!archive.exists() || archive.length() < 5_000_000L) {
                    Net.downloadToFile(
                    url = url, destination = archive, expectedSha1 = null,
                    onProgress = { done, total ->
                        _download.value = DownloadState(
                            active = true, versionId = "java-$major", stage = "Downloading ARM64 runtime",
                            bytesDone = done, bytesTotal = total,
                            percent = if (total > 0) done.toFloat() / total else 0f,
                        )
                        },
                    )
                }
                _download.value = DownloadState(active = true, versionId = "java-$major", stage = "Unpacking runtime", percent = 1f)
                dir.mkdirs()
                val proc = ProcessBuilder("/system/bin/tar", "-xJf", archive.absolutePath, "-C", dir.absolutePath)
                    .redirectErrorStream(true).start()
                val output = proc.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val code = proc.waitFor()
                val binary = dir.walkTopDown().firstOrNull { it.name == "java" && it.parentFile?.name == "bin" }
                    ?: throw IllegalStateException("Unpack failed: ${output.take(200)} (exit $code)")
                binary.setExecutable(true)

                val detected = scan().first { it.path == binary.absolutePath }
                if (detected.majorVersion != major) {
                    throw IllegalStateException("Runtime is Java ${detected.majorVersion}, expected $major")
                }
                _download.value = DownloadState(
                    active = false, versionId = "java-$major", stage = "Ready", success = true, percent = 1f,
                )
                detected
            }.onFailure {
                _download.value = DownloadState(active = false, versionId = "java-$major", stage = "Failed", error = it.message)
            }
        }

    companion object {
        private const val ANDROID_JRE_RELEASES =
            "https://api.github.com/repos/PojavLauncherTeam/android-openjdk-build-multiarch/releases"
        fun requiredMajor(details: VersionDetails): Int = details.javaVersion?.majorVersion ?: 8
        fun componentFor(details: VersionDetails, major: Int): String =
            details.javaVersion?.component ?: when {
                major >= 21 -> "java-runtime-delta"
                major >= 17 -> "java-runtime-gamma"
                else -> "jre-legacy"
            }
    }
}
