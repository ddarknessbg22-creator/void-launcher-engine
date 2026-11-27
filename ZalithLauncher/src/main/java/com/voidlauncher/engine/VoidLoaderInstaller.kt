package com.voidlauncher.engine

import com.movtery.zalithlauncher.ui.toAndroidString

import kotlinx.coroutines.launch

import android.content.Context
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersions
import com.movtery.zalithlauncher.coroutine.TaskStage
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.download.game.GameInstaller
import com.voidlauncher.util.VoidPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A loader build that can be installed for the selected Minecraft version. */
data class VoidLoaderOption(
    val loader: String,
    val version: String,
    val recommended: Boolean,
)

/**
 * Fetches and installs mod loaders using Zalith's proven downloader, so the created
 * version is a normal instalment that the in-process engine can launch.
 */
object VoidLoaderInstaller {

    val LOADERS = listOf("Vanilla", "Fabric", "Quilt", "Forge", "NeoForge")

    suspend fun list(loader: String, minecraftVersion: String?): List<VoidLoaderOption> {
        val mc = minecraftVersion ?: return emptyList()
        return runCatching {
            when (loader) {
                "Fabric" -> FabricVersions.fetchFabricLoaderList(mc).orEmpty()
                    .map { VoidLoaderOption("Fabric", it.version, it.stable) }
                "Quilt" -> QuiltVersions.fetchQuiltLoaderList(mc).orEmpty()
                    .map { VoidLoaderOption("Quilt", it.version, !it.version.contains("beta")) }
                "Forge" -> ForgeVersions.fetchForgeList(mc).orEmpty()
                    .filter { it.category == "installer" }
                    .map { VoidLoaderOption("Forge", it.versionName, it.isRecommended) }
                "NeoForge" -> NeoForgeVersions.fetchNeoForgeList(gameVersion = mc).orEmpty()
                    .map { VoidLoaderOption("NeoForge", it.versionName, !it.isBeta) }
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Installs [loader] [loaderVersion] for [minecraftVersion] as a new version named
     * `<minecraftVersion>-<loader>` and returns that version name.
     */
    data class LoaderProgress(val label: String, val fraction: Float)

    /**
     * Maps the engine's progress value onto a bar fraction.
     *
     * The engine reports `-1` (and occasionally NaN) for "running but not measurable yet"; both
     * must render as 0 rather than poisoning the bar.
     */
    fun progressFraction(raw: Float): Float =
        if (raw.isFinite() && raw >= 0f) raw.coerceIn(0f, 1f) else 0f

    private val _loaderTasks = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())
    val loaderTasks: kotlinx.coroutines.flow.StateFlow<List<String>> get() = _loaderTasks

    private val _loaderProgress = kotlinx.coroutines.flow.MutableStateFlow(LoaderProgress("", 0f))
    val loaderProgress: kotlinx.coroutines.flow.StateFlow<LoaderProgress> get() = _loaderProgress

    suspend fun install(
        context: Context,
        scope: CoroutineScope,
        loader: String,
        loaderVersion: String,
        minecraftVersion: String,
    ): String {
        val targetName = "$minecraftVersion-${loader.lowercase()}"
        val info = GameDownloadInfo(
            gameVersion = minecraftVersion,
            customVersionName = targetName,
            fabric = if (loader == "Fabric") {
                FabricVersion(inherit = minecraftVersion, version = loaderVersion, stable = true)
            } else null,
            quilt = if (loader == "Quilt") {
                QuiltVersion(minecraftVersion, loaderVersion)
            } else null,
            forge = if (loader == "Forge") {
                ForgeVersions.fetchForgeList(minecraftVersion).orEmpty()
                    .firstOrNull { it.versionName == loaderVersion }
            } else null,
            neoforge = if (loader == "NeoForge") {
                NeoForgeVersion(loaderVersion, isLegacyForge = minecraftVersion == "1.20.1")
            } else null,
        )

        val installer = GameInstaller(
            context = context,
            info = info,
            scope = scope,
            targetGameFolder = VoidPaths.root(context),
        )

        _loaderTasks.value = emptyList()
        _loaderProgress.value = LoaderProgress("Starting", 0f)
        suspendCancellableCoroutine<Unit> { cont ->
            val done = AtomicBoolean(false)
            val observer = scope.launch {
                // A task's progress is its own StateFlow: the task LIST does not change while a
                // download advances, so collecting only tasksFlow left the bar frozen at 0%.
                // Track the running task and collect its progress flow instead.
                var progressJob: Job? = null
                var observed: com.movtery.zalithlauncher.coroutine.Task? = null
                installer.tasksFlow.collect { tasks ->
                    _loaderTasks.value = tasks.map { it.title.toAndroidString(context) }
                    val running = tasks.firstOrNull { it.task.stage.value == TaskStage.RUNNING }
                        ?: tasks.lastOrNull()
                    if (running == null) return@collect
                    val label = running.title.toAndroidString(context)
                    if (running.task !== observed) {
                        observed = running.task
                        progressJob?.cancel()
                        progressJob = launch {
                            running.task.progress.collect { raw ->
                                _loaderProgress.value = LoaderProgress(label, progressFraction(raw))
                            }
                        }
                    }
                    _loaderProgress.value =
                        LoaderProgress(label, progressFraction(running.task.progress.value))
                }
            }
            cont.invokeOnCancellation { observer.cancel() }
            installer.installGame(
                onInstalled = {
                    observer.cancel()
                    _loaderProgress.value = LoaderProgress("Done", 1f)
                    if (done.compareAndSet(false, true) && cont.isActive) cont.resume(Unit)
                },
                onError = { th ->
                    if (done.compareAndSet(false, true) && cont.isActive) cont.resumeWithException(th)
                },
                onGameAlreadyInstalled = {
                    if (done.compareAndSet(false, true) && cont.isActive) cont.resume(Unit)
                },
            )
        }
        return targetName
    }

    fun modsDirectory(context: Context, versionId: String): File =
        File(File(VoidPaths.versions(VoidPaths.root(context)), versionId), "mods")
}
