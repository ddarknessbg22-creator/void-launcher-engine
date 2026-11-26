package com.voidlauncher.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/** Non-fatal notification for background download problems. */
object VoidFailureNotifier {
    private const val CHANNEL = "void_download_errors"

    fun report(context: Context, message: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "VOID download problems", NotificationManager.IMPORTANCE_LOW),
            )
        }
        manager.notify(
            77,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(com.movtery.zalithlauncher.R.drawable.ic_stat_download)
                .setContentTitle("VOID download problem")
                .setContentText(message.take(160))
                .setAutoCancel(true)
                .build(),
        )
    }
}
