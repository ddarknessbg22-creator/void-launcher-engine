package com.voidlauncher.engine

import android.app.Activity
import android.content.Context
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountType as ZalithAccountType
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.launch.GameLaunchFlow
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.setting.RendererSettings
import com.movtery.zalithlauncher.setting.enums.ResolutionRule
import com.movtery.zalithlauncher.game.version.installed.SettingState
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.toAndroidString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.voidlauncher.model.AccountType as VoidAccountType
import com.voidlauncher.model.LaunchSettings
import com.voidlauncher.model.VoidAccount
import com.voidlauncher.util.TokenVault
import com.voidlauncher.util.VoidPaths
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Launches Minecraft inside this APK using Zalith's native engine. */
object VoidEngineBridge {
    suspend fun launchInstalledVersion(
        context: Context,
        versionId: String,
        account: VoidAccount,
        settings: LaunchSettings,
    ) = coroutineScope {
        val activity = context as? Activity
            ?: error("VOID needs an Activity context to start the game")
        TokenVault.init(context.applicationContext)
        // Ensure the engine renderer registry is initialized before GameLaunchFlow.
        com.voidlauncher.engine.VoidGameReady.reset()
        Renderers.init()

        val gameHome = VoidPaths.root(context).absolutePath
        val performance = VoidPerformancePreset.fromId(settings.performancePreset)
        val version = VersionsManager.loadVersion(gameHome, versionId)
            ?: error("Minecraft $versionId is not installed under $gameHome")
        check(File(version.getVersionPath(), "$versionId.json").isFile) { "Missing version JSON" }
        check(version.getClientJar().isFile) { "Missing client JAR" }

        // Renderer choice is per version: the modern Krypton wrapper that the registry lists
        // first renders 1.12.2 at ~1 FPS, while GL4ES is the fast path there. The selection also
        // verifies the renderer's native library is actually present before launch.
        val selection = VoidRendererSelector.bestFor(
            context = context,
            minecraftVersion = versionId.substringBefore('-'),
            preferredId = VoidRendererSelector.preferred(),
        ) ?: error("No loadable renderer found. Install a renderer plugin in Settings → System.")

        // Render resolution is the other half of frame rate: the engine renders at this percentage
        // of the physical screen, so 100% on a 1440p phone is a large, invisible GPU cost.
        runCatching {
            RendererSettings.resolutionRule.save(ResolutionRule.PERCENTAGE)
            RendererSettings.resolutionRatio.save(performance.resolutionPercent)
        }

        if (!VoidRendererSelector.apply(selection)) {
            error("Could not activate the ${selection.name} renderer")
        }

        version.getVersionConfig().apply {
            isolationType = SettingState.ENABLE
            skipGameIntegrityCheck = SettingState.ENABLE
            jvmArgs = mergeJvmArgs(settings.jvmArgs, performance.jvmArgs)
            ramAllocation = settings.memoryMb
        }

        // Write the performance profile into the real options.txt before the game starts. This is
        // what actually removes in-game lag: render/simulation distance, particles, clouds and
        // deferred chunk updates, not launcher-side tuning.
        val profileApplied = VoidPerformance.apply(context, version, performance)
        if (!profileApplied) {
            // Non-fatal: the game still launches with whatever options.txt already holds.
            android.util.Log.w(
                "VoidEngineBridge",
                "Could not write the ${performance.id} performance profile; launching with existing options",
            )
        }

        val zalithAccount = account.toZalithAccount()
        AccountsManager.suspendSaveAccount(zalithAccount)
        AccountsManager.setCurrentAccount(zalithAccount)
        AccountsManager.markSessionValidated(zalithAccount)
        check(AccountsManager.currentAccountFlow.value != null) { "Failed to activate account" }

        val scope = this
        suspendCancellableCoroutine<Unit> { cont ->
            val finished = AtomicBoolean(false)
            fun succeed() {
                if (finished.compareAndSet(false, true) && cont.isActive) cont.resume(Unit)
            }
            fun fail(th: Throwable) {
                if (finished.compareAndSet(false, true) && cont.isActive) cont.resumeWithException(th)
            }
            val flow = GameLaunchFlow(scope)
            cont.invokeOnCancellation { flow.cancel() }
            flow.launch(
                context = activity,
                version = version,
                skipAccountRefresh = true,
                exitActivity = { activity.finish() },
                onRendererDeclined = { succeed() },
                onReloginRequired = { fail(IllegalStateException("Microsoft re-login required in VOID")) },
                onRefreshFailed = { _, th -> fail(th) },
                onComplete = { succeed() },
                onError = { message: ErrorViewModel.ThrowableMessage ->
                    fail(IllegalStateException(message.message.toAndroidString(activity)))
                },
            )
        }
    }

    /**
     * Combines user JVM arguments with the preset's, dropping duplicates so the game does not
     * receive the same flag twice (the later one would silently win).
     */
    fun mergeJvmArgs(user: String, preset: String): String =
        (user.trim().split(Regex("\\s+")) + preset.trim().split(Regex("\\s+")))
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")

    private fun VoidAccount.toZalithAccount(): Account {
        val microsoft = type == VoidAccountType.MICROSOFT
        val alias = tokenAlias.orEmpty()
        return Account(
            uniqueUUID = id,
            accessToken = if (microsoft) TokenVault.read(alias).orEmpty() else "0",
            refreshToken = if (microsoft) TokenVault.read("${alias}_refresh").orEmpty() else "0",
            expiresAt = if (microsoft) Long.MAX_VALUE else 0L,
            clientToken = "0",
            username = username,
            profileId = uuid.ifBlank { id },
            xUid = xuid,
            accountType = if (microsoft) ZalithAccountType.MICROSOFT.tag
                          else ZalithAccountType.LOCAL.tag,
        )
    }
}
