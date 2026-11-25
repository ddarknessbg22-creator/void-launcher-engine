package com.voidlauncher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.movtery.zalithlauncher.R
import com.voidlauncher.model.DownloadState
import com.voidlauncher.util.Net
import com.voidlauncher.util.VoidPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Downloads keep running after VOID is closed. The UI observes the same app-scoped
 * repositories, so returning to the app always shows the true live progress.
 */
class DownloadService : Service() {

    companion object {
        private const val CHANNEL = "void_downloads"
        private const val NOTIFICATION_ID = 41
        const val ACTION_VERSION = "void.action.install_version"
        const val ACTION_JAVA = "void.action.download_java"
        const val EXTRA_ID = "void.extra.id"
        const val EXTRA_MAJOR = "void.extra.major"
        const val EXTRA_COMPONENT = "void.extra.component"

        fun startVersion(context: Context, id: String) {
            context.startForegroundService(
                Intent(context, DownloadService::class.java).apply {
                    action = ACTION_VERSION
                    putExtra(EXTRA_ID, id)
                },
            )
        }

        fun startJava(context: Context, component: String, major: Int) {
            context.startForegroundService(
                Intent(context, DownloadService::class.java).apply {
                    action = ACTION_JAVA
                    putExtra(EXTRA_COMPONENT, component)
                    putExtra(EXTRA_MAJOR, major)
                },
            )
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var workJob: Job? = null
    private var observerJob: Job? = null
    private lateinit var notifications: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notifications = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifications.createNotificationChannel(
                NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW),
            )
        }
        startForeground(NOTIFICATION_ID, notification("Starting download", 0f, 0L, 0L))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        workJob?.cancel()
        observerJob?.cancel()

        when (intent?.action) {
            ACTION_VERSION -> {
                val id = intent.getStringExtra(EXTRA_ID)
                if (id.isNullOrBlank()) {
                    finish("No version specified")
                    return START_NOT_STICKY
                }
                // Reset any stale completed state before observing.
                val repo = (application as VoidApplication).versions
                workJob = scope.launch { repo.install(id).join() }
                observerJob = scope.launch { observe(repo.download) }
            }
            ACTION_JAVA -> {
                val major = intent.getIntExtra(EXTRA_MAJOR, 17)
                val component = intent.getStringExtra(EXTRA_COMPONENT) ?: "jre-legacy"
                val repo = (application as VoidApplication).javaRepo
                workJob = scope.launch {
                    val result = repo.downloadRuntime(component, major)
                    finish(
                        if (result.isSuccess) "Runtime ready" else "Runtime failed: ${result.exceptionOrNull()?.message}"
                    )
                }
                observerJob = scope.launch { observe(repo.download) }
            }
            else -> finish("Unknown download task")
        }
        return START_NOT_STICKY
    }

    private suspend fun observe(flow: StateFlow<DownloadState>) {
        flow.collect { state ->
            when {
                state.active -> update(
                    state.stage.ifBlank { "Downloading" }, state.percent, state.bytesDone, state.bytesTotal,
                )
                state.success -> {
                    update("Download complete", 1f, state.bytesDone, state.bytesTotal)
                    finish("Download complete")
                }
                state.error != null -> {
                    update("Download failed: ${state.error}", 0f, state.bytesDone, state.bytesTotal)
                    finish("Download failed: ${state.error}")
                }
            }
        }
    }

    private fun update(text: String, progress: Float, done: Long, total: Long) {
        notifications.notify(NOTIFICATION_ID, notification(text, progress, done, total))
    }

    private fun notification(text: String, progress: Float, done: Long, total: Long): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val detail = if (total > 0) "${Net.formatBytes(done)} / ${Net.formatBytes(total)}" else ""
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle("VOID Launcher")
            .setContentText(if (detail.isBlank()) text else "$text · $detail")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(1000, (progress * 1000).toInt(), total <= 0)
            .build()
    }

    private fun finish(message: String) {
        update(message, 1f, 0L, 0L)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        workJob?.cancel()
        observerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
