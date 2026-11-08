package com.romanarsiv.background

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.romanarsiv.ArchiverApp
import com.romanarsiv.R
import com.romanarsiv.data.remote.ChallengeBroker
import com.romanarsiv.settings.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Bölüm tespiti ya da indirmeler sürerken süreci canlı tutan ön plan hizmeti (tür: dataSync).
 * İşi kendisi yapmaz; [ArchiveJobs]'u izler, ilerlemeyi tek bir bildirimde gösterir, tüm işler
 * bitince durur. Her indirme bitince (ya da başarısız olunca) ayrı bir sonuç bildirimi gönderir.
 */
class ArchiveService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observer: Job? = null
    private var lastStartId = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastShape: Any? = null
    private var lastPostAt = 0L

    private lateinit var jobs: ArchiveJobs
    private lateinit var broker: ChallengeBroker

    override fun onCreate() {
        super.onCreate()
        val container = (application as ArchiverApp).container
        jobs = container.archiveJobs
        broker = container.challengeBroker
    }

    /** Bildirim metinleri uygulamanın seçili dilinde. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppPreferences.localize(newBase))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        // Android, startForegroundService'ten sonra birkaç saniye içinde startForeground ister; iş çoktan
        // bitmiş olsa bile önce ön plana geç, sonra dur.
        ServiceCompat.startForeground(
            this,
            ArchiveNotifications.ID_PROGRESS,
            ArchiveNotifications.progress(this, jobs.detectState.value, jobs.tasks.value, broker.pendingUrl.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        if (intent?.action == ACTION_CANCEL) jobs.cancelAll()
        acquireWakeLock()
        if (observer == null) observer = scope.launch { observe() }
        return START_NOT_STICKY
    }

    private suspend fun observe() {
        var wasDetecting = false
        // Son görülen durumlar: aktiften bitmişe geçen her indirme için bir sonuç bildirimi.
        var lastStatus = jobs.tasks.value.associate { it.id to it.status }
        combine(jobs.detectState, jobs.tasks, broker.pendingUrl) { d, t, ch -> Triple(d, t, ch) }
            .collect { (detect, tasks, challenge) ->
                val detecting = detect is DetectState.Loading
                tasks.forEach { task ->
                    val before = lastStatus[task.id]
                    if (before != null && before.isActive && !task.status.isActive) notifyDownloadResult(task)
                }
                lastStatus = tasks.associate { it.id to it.status }
                if (wasDetecting && !detecting) notifyDetectResult()
                wasDetecting = detecting

                if (!detecting && tasks.none { it.status.isActive }) {
                    finish()
                } else {
                    // Android sık güncellenen bildirimleri düşürür; birkaç indirme aynı anda ilerlerken seyrelt.
                    // Yapı değişince (indirme başladı/bitti, doğrulama) hemen güncelle.
                    val shape = Triple(detecting, tasks.filter { it.status.isActive }.map { it.id to it.status }, challenge)
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (shape != lastShape || now - lastPostAt >= NOTIFY_INTERVAL_MS) {
                        lastShape = shape
                        lastPostAt = now
                        ArchiveNotifications.post(
                            this,
                            ArchiveNotifications.ID_PROGRESS,
                            ArchiveNotifications.progress(this, detect, tasks, challenge),
                        )
                    }
                }
            }
    }

    private fun appInForeground(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    /** Her indirme bitince bildirim (uygulama açık olsa da); iptal edilenler için yok. */
    private fun notifyDownloadResult(task: DownloadTask) {
        val (title, text) = when (task.status) {
            DownloadStatus.DONE -> {
                val r = task.result ?: return
                getString(R.string.notif_saved_title) to
                    if (r.skippedChapters > 0) getString(R.string.saved_success_skipped, r.fileName, r.chapterCount, r.skippedChapters)
                    else getString(R.string.saved_success, r.fileName, r.chapterCount)
            }
            DownloadStatus.FAILED -> getString(R.string.notif_failed_title, task.bookTitle) to
                (task.error?.let { describeError(this, it) } ?: "")
            else -> return
        }
        ArchiveNotifications.post(
            this,
            ArchiveNotifications.resultId(task.id),
            ArchiveNotifications.result(this, title, text, downloads = true),
        )
    }

    private fun notifyDetectResult() {
        if (appInForeground()) return
        val result = jobs.detectResult.value ?: return
        ArchiveNotifications.post(
            this,
            ArchiveNotifications.ID_DETECT_RESULT,
            ArchiveNotifications.result(
                this,
                getString(R.string.notif_detected_title),
                getString(R.string.notif_detected_text, result.chapters.size),
                downloads = false,
            ),
        )
    }

    private fun finish() {
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        // Bu arada yeni bir iş başladıysa (daha yeni startId) hizmet durmaz.
        stopSelf(lastStartId)
    }

    /** Ekran kapalıyken de ağ istekleri sürsün; en fazla 3 saat. */
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(PowerManager::class.java) ?: return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RomanArsivi:indirme").apply {
            setReferenceCounted(false)
            acquire(3 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    /** Android 15+: dataSync hizmetleri günde 6 saatle sınırlı; süre dolunca dur. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        jobs.cancelAll()
        finish()
    }

    override fun onDestroy() {
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CANCEL = "com.romanarsiv.action.CANCEL"
        private const val NOTIFY_INTERVAL_MS = 800L

        /** İş başlarken çağrılır (uygulama ön plandayken). Başlatılamazsa iş yine sürer, yalnızca korumasız kalır. */
        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, ArchiveService::class.java))
            }
        }
    }
}
