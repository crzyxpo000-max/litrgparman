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

/** Arka plan işinin bildirimleri: süren işler (ön plan hizmeti) ve her indirmenin sonucu. */
object ArchiveNotifications {
    const val CHANNEL_PROGRESS = "arsivleme"
    const val CHANNEL_RESULT = "sonuc"
    const val ID_PROGRESS = 1
    const val ID_DETECT_RESULT = 2
    private const val ID_RESULT_BASE = 1000

    /** Bildirime dokununca İndirilenler sekmesi açılsın. */
    const val EXTRA_OPEN_DOWNLOADS = "com.romanarsiv.extra.OPEN_DOWNLOADS"

    /** Kanal adları dil değişince de güncellensin diye her açılışta ve dil değişiminde çağrılır. */
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

    fun resultId(taskId: Long): Int = ID_RESULT_BASE + (taskId % 100_000).toInt()

    private fun openApp(context: Context, downloads: Boolean): PendingIntent = PendingIntent.getActivity(
        context,
        if (downloads) 2 else 0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OPEN_DOWNLOADS, downloads),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun cancelAction(context: Context): PendingIntent = PendingIntent.getService(
        context,
        1,
        Intent(context, ArchiveService::class.java).setAction(ArchiveService.ACTION_CANCEL),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Süren işlerin tek bildirimi; doğrulama bekleniyorsa kullanıcıyı uygulamaya çağırır. */
    fun progress(context: Context, detect: DetectState, tasks: List<DownloadTask>, challengeUrl: String?): Notification {
        val running = tasks.filter { it.status == DownloadStatus.RUNNING }
        val queued = tasks.count { it.status == DownloadStatus.QUEUED }
        val builder = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp(context, downloads = running.isNotEmpty() || queued > 0))
            .addAction(0, context.getString(R.string.cancel_all), cancelAction(context))

        when {
            running.size == 1 -> {
                val task = running.first()
                builder.setContentTitle(context.getString(R.string.notif_downloading, task.bookTitle))
                builder.setContentText(progressLine(context, task.progress))
                if (queued > 0) builder.setSubText(context.getString(R.string.notif_queued, queued))
                val p = task.progress
                if (p is ArchiveProgress.FetchingChapter && p.total > 0) builder.setProgress(p.total, p.index, false)
                else builder.setProgress(0, 0, true)
            }
            running.size > 1 -> {
                builder.setContentTitle(context.getString(R.string.notif_downloading_many, running.size))
                val style = NotificationCompat.InboxStyle()
                running.forEach { style.addLine("${it.bookTitle} · ${progressLine(context, it.progress)}") }
                if (queued > 0) style.setSummaryText(context.getString(R.string.notif_queued, queued))
                builder.setStyle(style)
                builder.setContentText(running.joinToString(" · ") { it.bookTitle })
                val total = running.sumOf { it.requestedChapters }
                val done = running.sumOf { (it.progress as? ArchiveProgress.FetchingChapter)?.index ?: it.requestedChapters }
                builder.setProgress(total.coerceAtLeast(1), done.coerceAtMost(total), false)
            }
            detect is DetectState.Loading -> {
                builder.setContentTitle(context.getString(R.string.detect_title))
                builder.setContentText(
                    if (detect.found > 0) context.resources.getQuantityString(R.plurals.detect_found, detect.found, detect.found) else detect.source.siteName,
                )
                builder.setProgress(0, 0, true)
            }
            queued > 0 -> builder.setContentTitle(context.getString(R.string.notif_queued, queued))
            else -> builder.setContentTitle(context.getString(R.string.app_name))
        }

        if (challengeUrl != null) {
            // Doğrulama yalnızca uygulama içinde geçilebilir: dikkat çekecek metin.
            builder.setContentText(context.getString(R.string.notif_challenge, UrlUtils.hostOf(challengeUrl) ?: challengeUrl))
            builder.setContentIntent(openApp(context, downloads = false))
            builder.setSilent(false).setOnlyAlertOnce(false)
        }
        return builder.build()
    }

    fun progressLine(context: Context, progress: ArchiveProgress?): String = when (progress) {
        is ArchiveProgress.FetchingChapter ->
            if (progress.total > 0) context.getString(R.string.progress_chapter, progress.index, progress.total)
            else context.getString(R.string.progress_chapter_open, progress.index)
        ArchiveProgress.BuildingEpub -> context.getString(R.string.progress_building)
        ArchiveProgress.Saving -> context.getString(R.string.progress_saving)
        ArchiveProgress.DetectingChapters, null -> context.getString(R.string.progress_starting)
    }

    fun result(context: Context, title: String, text: String, downloads: Boolean): Notification =
        NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openApp(context, downloads))
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
