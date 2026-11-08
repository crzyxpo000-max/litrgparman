package com.romanarsiv.background

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.romanarsiv.MainActivity
import com.romanarsiv.R
import com.romanarsiv.data.remote.UrlUtils
import com.romanarsiv.domain.model.ArchiveProgress

/** Arka plan işinin bildirimleri: süren iş (ön plan hizmeti) ve sonuç. */
object ArchiveNotifications {
    const val CHANNEL_PROGRESS = "arsivleme"
    const val CHANNEL_RESULT = "sonuc"
    const val ID_PROGRESS = 1
    const val ID_RESULT = 2

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, context.getString(R.string.channel_progress), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.channel_progress_desc)
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_RESULT, context.getString(R.string.channel_result), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_result_desc)
            },
        )
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun cancelAction(context: Context): PendingIntent = PendingIntent.getService(
        context,
        1,
        Intent(context, ArchiveService::class.java).setAction(ArchiveService.ACTION_CANCEL),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Süren işin bildirimi; doğrulama bekleniyorsa kullanıcıyı uygulamaya çağırır. */
    fun progress(context: Context, detect: DetectState, download: DownloadState, challengeUrl: String?): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp(context))
            .addAction(0, context.getString(R.string.cancel), cancelAction(context))

        when {
            download is DownloadState.Loading -> {
                builder.setContentTitle(context.getString(R.string.notif_downloading, download.bookTitle))
                when (val p = download.progress) {
                    is ArchiveProgress.FetchingChapter -> {
                        builder.setContentText(context.getString(R.string.progress_chapter, p.index, p.total))
                        builder.setSubText(p.title.take(60))
                        builder.setProgress(p.total.coerceAtLeast(1), p.index, false)
                    }
                    ArchiveProgress.BuildingEpub -> builder.setContentText(context.getString(R.string.progress_building)).setProgress(0, 0, true)
                    ArchiveProgress.Saving -> builder.setContentText(context.getString(R.string.progress_saving)).setProgress(0, 0, true)
                    ArchiveProgress.DetectingChapters -> builder.setContentText(context.getString(R.string.progress_detecting)).setProgress(0, 0, true)
                }
            }
            detect is DetectState.Loading -> {
                builder.setContentTitle(context.getString(R.string.detect_title))
                builder.setContentText(
                    if (detect.found > 0) context.getString(R.string.detect_found, detect.found) else detect.source.siteName,
                )
                builder.setProgress(0, 0, true)
            }
            else -> builder.setContentTitle(context.getString(R.string.app_name))
        }

        if (challengeUrl != null) {
            // Doğrulama yalnızca uygulama içinde geçilebilir: dikkat çekecek metin.
            builder.setContentText(context.getString(R.string.notif_challenge, UrlUtils.hostOf(challengeUrl) ?: challengeUrl))
            builder.setSilent(false).setOnlyAlertOnce(false)
        }
        return builder.build()
    }

    fun result(context: Context, title: String, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()

    /** Android 13+ bildirim izni yoksa sessizce vazgeçer (iş yine sürer). */
    fun post(context: Context, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
