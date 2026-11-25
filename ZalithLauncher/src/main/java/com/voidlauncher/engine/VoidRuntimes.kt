package com.voidlauncher.engine

import com.movtery.zalithlauncher.components.jre.Jre
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.path.PathManager
import com.voidlauncher.model.JavaRuntime
import java.io.File
import com.voidlauncher.util.newId

/** Reads the JREs actually unpacked by the in-process engine. */
object VoidRuntimes {
    fun installed(): List<JavaRuntime> = runCatching {
        RuntimesManager.getRuntimes()
            .filter { it.isCompatible() }
            .map { runtime ->
                JavaRuntime(
                    id = newId(),
                    path = File(PathManager.DIR_MULTIRT, runtime.name).absolutePath,
                    version = runtime.versionString ?: "Java ${runtime.javaVersion}",
                    majorVersion = runtime.javaVersion,
                    vendor = "Bundled",
                    source = "Included with VOID",
                    valid = true,
                )
            }
            .sortedByDescending { it.majorVersion }
    }.getOrDefault(emptyList())

    /** Runtime components bundled in the APK but not yet unpacked. */
    fun bundled(): List<JavaRuntime> = Jre.entries.map { jre ->
        JavaRuntime(
            id = newId(),
            path = "asset:${jre.jrePath}",
            version = jre.jreName,
            majorVersion = jre.majorVersion,
            vendor = "Bundled",
            source = "Will unpack on first launch",
            valid = false,
        )
    }
}
