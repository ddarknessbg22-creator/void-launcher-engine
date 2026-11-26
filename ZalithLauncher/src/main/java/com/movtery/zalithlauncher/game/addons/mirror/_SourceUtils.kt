package com.movtery.zalithlauncher.game.addons.mirror

import com.movtery.zalithlauncher.setting.LauncherSettings
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

private const val TAG = "SourceUtils"

data class MirrorSource<T>(
    val type: SourceType,
    val block: suspend () -> T?
)

/**
 * 按顺序尝试各源，首个成功者胜出
 * 全部失败时抛出最后一次异常
 */
suspend fun <T> runMirrorable(
    sources: List<MirrorSource<T>>
): T? = coroutineScope {
    if (sources.isEmpty()) return@coroutineScope null

    // VOID: race every source in parallel and take the first usable answer, so a
    // slow or dead mirror never blocks the UI.
    val jobs = sources.map { source ->
        async(Dispatchers.IO) {
            runCatching { source.block() }
                .onFailure { Logger.debug(TAG, "Source ${source.type.displayName} failed!", it) }
                .getOrNull()
        }
    }

    var failure: Throwable? = null
    for (job in jobs) {
        try {
            val value = job.await()
            if (value != null) {
                jobs.forEach { it.cancel() }
                return@coroutineScope value
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure = e
        }
    }

    if (failure != null) throw failure
    null
}

/**
 * 把官方在前、镜像在后的规范源列表按“游戏内容下载源”设置重排；
 * 自动档下大陆用户镜像优先，官方档仅保留官方源
 */
fun <T> List<MirrorSource<T>>.orderedByGameSourcePreference(): List<MirrorSource<T>> =
    when (resolveMirrorPriority(LauncherSettings.gameDownloadSource.getValue(), mainland = true)) {
        MirrorPriority.OFFICIAL -> filter { it.type == SourceType.OFFICIAL }
        MirrorPriority.MIRROR_FIRST -> reversed()
    }
