package com.upvotesaver.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.upvotesaver.core.NotSignedInException
import com.upvotesaver.core.Progress
import com.upvotesaver.core.RedgifsSignedOutException
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** One download run: the scheduled check, or a check / full scan / import started from the screen. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = Graph.settings
        val mode = inputData.getString(KEY_MODE) ?: MODE_AUTO
        val redgifsMode = mode == MODE_LINKS || mode == MODE_RG_NEW || mode == MODE_RG_FULL
        if (mode == MODE_AUTO && !settings.signedIn && !settings.redgifsSignedIn) return Result.success()
        if (!redgifsMode && mode != MODE_AUTO && !settings.signedIn) return Result.success()
        // Runs started from the screen may take long; a foreground notification keeps them alive.
        // (Not allowed for the background schedule, which only does short checks.)
        if (mode != MODE_AUTO) runCatching { setForeground(foregroundInfo()) }
        return Graph.syncLock.withLock {
            try {
                var total = Progress()
                fun report(p: Progress) {
                    setProgressAsync(data(Progress(total.scanned + p.scanned, total.saved + p.saved, total.failed + p.failed)))
                }
                fun add(p: Progress) {
                    total = Progress(total.scanned + p.scanned, total.saved + p.saved, total.failed + p.failed)
                }
                val saver = Graph.redgifsSaver(applicationContext, ::report)
                when (mode) {
                    MODE_LINKS -> add(saver.save(takeLinks(applicationContext)))
                    MODE_RG_NEW -> add(saver.syncLikes(full = false))
                    MODE_RG_FULL -> add(saver.syncLikes(full = true))
                    MODE_FULL -> add(Graph.engine(applicationContext, ::report).sync(settings.username, full = true))
                    MODE_IMPORT -> add(
                        Graph.engine(applicationContext, ::report)
                            .import(importFile(applicationContext).takeIf { it.exists() }?.readLines().orEmpty()),
                    )
                    MODE_NEW -> add(Graph.engine(applicationContext, ::report).sync(settings.username, full = false))
                    else -> {
                        // The schedule: Reddit upvotes and Redgifs likes, whichever is signed in.
                        if (settings.signedIn) add(Graph.engine(applicationContext, ::report).sync(settings.username, full = false))
                        if (settings.redgifsSignedIn) {
                            try {
                                add(saver.syncLikes(full = false))
                            } catch (e: RedgifsSignedOutException) {
                                // The token expired; the app asks to sign in again when it is opened.
                            }
                        }
                    }
                }
                settings.lastSyncMs = System.currentTimeMillis()
                Result.success(data(total))
            } catch (e: NotSignedInException) {
                Result.failure(workDataOf(KEY_SIGNED_OUT to true))
            } catch (e: RedgifsSignedOutException) {
                Result.failure(workDataOf(KEY_RG_SIGNED_OUT to true))
            } catch (e: IOException) {
                if (mode == MODE_AUTO) Result.retry() else Result.failure(workDataOf(KEY_ERROR to (e.message ?: "network error")))
            }
        }
    }

    private fun foregroundInfo(): ForegroundInfo {
        val context = applicationContext
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_sync), NotificationManager.IMPORTANCE_LOW),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_sync)
            .setContentTitle(context.getString(R.string.notification_title))
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
        return ForegroundInfo(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        const val KEY_MODE = "mode"
        const val KEY_ERROR = "error"
        const val KEY_SIGNED_OUT = "signed_out"
        const val MODE_AUTO = "auto"
        const val MODE_NEW = "new"
        const val MODE_FULL = "full"
        const val MODE_IMPORT = "import"
        const val MODE_LINKS = "links"
        const val MODE_RG_NEW = "rg_new"
        const val MODE_RG_FULL = "rg_full"
        const val KEY_RG_SIGNED_OUT = "rg_signed_out"
        const val WORK_LINKS = "links"
        const val WORK_AUTO = "auto"
        const val WORK_MANUAL = "manual"
        private const val CHANNEL = "sync"

        fun data(p: Progress) = workDataOf("scanned" to p.scanned, "saved" to p.saved, "failed" to p.failed)

        fun progress(data: androidx.work.Data) =
            Progress(scanned = data.getInt("scanned", 0), saved = data.getInt("saved", 0), failed = data.getInt("failed", 0))

        /** Post ids waiting for an import run (too many to pass as work input). */
        fun importFile(context: Context) = File(context.filesDir, "import_ids.txt")

        private fun linkFile(context: Context) = File(context.filesDir, "link_ids.txt")

        /** Queues Redgifs clips for download; a run that is already going is followed by another. */
        fun saveLinks(context: Context, ids: List<String>) {
            synchronized(this) { linkFile(context).appendText(ids.joinToString("\n", postfix = "\n")) }
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints(NetworkType.CONNECTED))
                .setInputData(workDataOf(KEY_MODE to MODE_LINKS))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_LINKS, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        private fun takeLinks(context: Context): List<String> = synchronized(this) {
            val file = linkFile(context)
            if (!file.exists()) return emptyList()
            file.readLines().filter { it.isNotBlank() }.distinct().also { file.delete() }
        }

        /** Turns the periodic check on or off according to the settings. */
        fun schedule(context: Context) {
            val settings = Graph.settings
            val work = WorkManager.getInstance(context)
            if ((!settings.signedIn && !settings.redgifsSignedIn) || settings.intervalMinutes <= 0) {
                work.cancelUniqueWork(WORK_AUTO)
                return
            }
            val request = PeriodicWorkRequestBuilder<SyncWorker>(settings.intervalMinutes.toLong(), TimeUnit.MINUTES)
                .setConstraints(Constraints(if (settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED))
                .setInputData(workDataOf(KEY_MODE to MODE_AUTO))
                .build()
            work.enqueueUniquePeriodicWork(WORK_AUTO, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun runNow(context: Context, mode: String) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints(NetworkType.CONNECTED))
                .setInputData(workDataOf(KEY_MODE to mode))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_MANUAL, ExistingWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_MANUAL)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_LINKS)
        }
    }
}
