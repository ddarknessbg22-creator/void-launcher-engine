package com.voidlauncher.engine

import android.content.Context
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersions
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.download.game.GameInstaller
import com.voidlauncher.util.VoidPaths
import kotlinx.coroutines.CoroutineScope
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

        suspendCancellableCoroutine<Unit> { cont ->
            val done = AtomicBoolean(false)
            installer.installGame(
                onInstalled = {
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
