package com.voidlauncher

import android.app.Application
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.setting.RendererSettings
import com.voidlauncher.data.JavaRepository
import com.voidlauncher.data.SettingsRepository
import com.voidlauncher.data.VersionRepository
import com.voidlauncher.engine.EngineRepository
import com.voidlauncher.util.TokenVault
import com.voidlauncher.util.VoidPaths

class VoidApplication : com.movtery.zalithlauncher.ZLApplication() {
    // App-scoped repositories survive Activity recreation; the foreground service owns long downloads.
    val versions: VersionRepository by lazy { VersionRepository(this) }
    val engines: EngineRepository by lazy { EngineRepository(this) }
    val javaRepo: JavaRepository by lazy { JavaRepository(this) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        TokenVault.init(this)
        val root = VoidPaths.root(this)
        VoidPaths.versions(root)
        VoidPaths.libraries(root)
        VoidPaths.objects(root)
        VoidPaths.indexes(root)
        VoidPaths.natives(root)
        VoidPaths.runtimes(root)
        VoidPaths.logs(root)
        VoidPaths.exports(root)

        // VOID does not launch Zalith's launcher UI, so initialize the engine here.
        runCatching {
            Renderers.init()
            val saved = RendererSettings.renderer.getValue()
            if (saved.isNotBlank() && Renderers.getRenderers().any { it.getUniqueIdentifier() == saved }) {
                Renderers.setCurrentRenderer(saved, retryToFirstOnFailure = false)
            } else {
                val first = Renderers.getRenderers().firstOrNull()
                if (first != null) {
                    Renderers.setCurrentRenderer(first.getUniqueIdentifier(), retryToFirstOnFailure = false)
                    RendererSettings.renderer.save(first.getUniqueIdentifier())
                }
            }
        }
    }
}
