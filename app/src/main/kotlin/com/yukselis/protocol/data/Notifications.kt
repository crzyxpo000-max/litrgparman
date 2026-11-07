package com.yukselis.protocol.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yukselis.engine.save.SaveManager
import com.yukselis.protocol.MainActivity
import com.yukselis.protocol.R
import com.yukselis.protocol.YukselisApplication
import java.util.concurrent.TimeUnit

/**
 * Oturumlar arası nadir, isteğe bağlı yerel bildirimler (Aşama 9, GDD: Bildirimler). Hiçbir şey ağdan gelmez:
 * işçi otomatik kaydı okur, motorun önerdiği bildirimi (rakip seni geçti, üst kademeden bir göz, taşacak kapı)
 * en fazla iki günde bir gösterir. Dokununca oyun açılır ve olay bir sonraki gün akışında gelir.
 */
object Notifications {
    const val CHANNEL = "sistem"
    const val EXTRA_NOTE = "bildirim"
    private const val WORK = "sistem_bildirimleri"
    private const val NOTIFICATION_ID = 7

    /** Bildirimler en az bu aralıkla gösterilir ("nadir"). */
    val MIN_INTERVAL_MS: Long = TimeUnit.HOURS.toMillis(48)

    fun permitted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Ayara göre zamanlayıcıyı kurar ya da kaldırır. */
    fun schedule(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(WORK)
            return
        }
        ensureChannel(context)
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(12, TimeUnit.HOURS)
            .setInitialDelay(12, TimeUnit.HOURS)
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notif_channel_desc)
            },
        )
    }

    fun post(context: Context, noteId: String, title: String, text: String) {
        if (!permitted(context)) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_NOTE, noteId)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // İzin bu arada geri alındıysa sessizce vazgeç.
        }
    }
}

/** Arka plan işçisi: otomatik kaydı okur, uygun bir bildirim varsa ve süre dolduysa gösterir. */
class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as YukselisApplication
        val settings = app.settings
        if (!settings.settings().notifications || !Notifications.permitted(applicationContext)) return Result.success()
        val now = System.currentTimeMillis()
        if (now - settings.lastNotificationMillis() < Notifications.MIN_INTERVAL_MS) return Result.success()
        val state = runCatching { app.saves.load(SaveManager.AUTO) }.getOrNull() ?: return Result.success()
        val rivalWasAhead = settings.rivalWasAhead()
        runCatching { app.engine.rivalAhead(state) }.getOrNull()?.let { settings.setRivalWasAhead(it) }
        val note = runCatching { app.engine.pendingNotification(state, settings.language(), rivalWasAhead) }.getOrNull() ?: return Result.success()
        // Aynı bildirimi aynı oyun gününde tekrarlama.
        val key = "${state.seed}:${state.day}:${note.first}"
        if (settings.lastNotificationKey() == key) return Result.success()
        Notifications.post(applicationContext, note.first, note.second, note.third)
        settings.markNotified(now, key)
        return Result.success()
    }
}
