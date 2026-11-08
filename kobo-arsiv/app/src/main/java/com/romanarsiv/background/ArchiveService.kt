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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Bölüm tespiti ya da indirme sürerken süreci canlı tutan ön plan hizmeti (tür: dataSync).
 * İşi kendisi yapmaz; [ArchiveJobs]'u izler, ilerlemeyi bildirimde gösterir, iş bitince durur.
 * Uygulama arka plandayken iş biterse sonuç bildirimi gönderir.
 */
class ArchiveService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observer: Job? = null
    private var lastStartId = 0
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var jobs: ArchiveJobs
    private lateinit var broker: ChallengeBroker

    override fun onCreate() {
        super.onCreate()
        val container = (application as ArchiverApp).container
        jobs = container.archiveJobs
        broker = container.challengeBroker
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        // Android, startForegroundService'ten sonra birkaç saniye içinde startForeground ister; iş çoktan
        // bitmiş olsa bile önce ön plana geç, sonra dur.
        ServiceCompat.startForeground(
            this,
            ArchiveNotifications.ID_PROGRESS,
            ArchiveNotifications.progress(this, jobs.detectState.value, jobs.downloadState.value, broker.pendingUrl.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        if (intent?.action == ACTION_CANCEL) jobs.cancelAll()
        acquireWakeLock()
        if (observer == null) observer = scope.launch { observe() }
        return START_NOT_STICKY
    }

    private suspend fun observe() {
        var wasDownloading = false
        var wasDetecting = false
        combine(jobs.detectState, jobs.downloadState, broker.pendingUrl) { d, dl, ch -> Triple(d, dl, ch) }
            .collect { (detect, download, challenge) ->
                val detecting = detect is DetectState.Loading
                val downloading = download is DownloadState.Loading

                if (wasDownloading && !downloading) notifyDownloadResult(download)
                if (wasDetecting && !detecting) notifyDetectResult()
                wasDownloading = downloading
                wasDetecting = detecting

                if (!detecting && !downloading) {
                    finish()
                } else {
                    ArchiveNotifications.post(
                        this,
                        ArchiveNotifications.ID_PROGRESS,
                        ArchiveNotifications.progress(this, detect, download, challenge),
                    )
                }
            }
    }

    private fun appInForeground(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun notifyDownloadResult(state: DownloadState) {
        if (appInForeground()) return // Snackbar zaten gösteriyor.
        val (title, text) = when (state) {
            is DownloadState.Success -> getString(R.string.notif_saved_title) to
                getString(R.string.saved_success, state.result.fileName, state.result.chapterCount)
            is DownloadState.Error -> getString(R.string.notif_failed_title) to state.message
            else -> return // iptal
        }
        ArchiveNotifications.post(this, ArchiveNotifications.ID_RESULT, ArchiveNotifications.result(this, title, text))
    }

    private fun notifyDetectResult() {
        if (appInForeground()) return
        val result = jobs.detectResult.value ?: return
        ArchiveNotifications.post(
            this,
            ArchiveNotifications.ID_RESULT,
            ArchiveNotifications.result(
                this,
                getString(R.string.notif_detected_title),
                getString(R.string.notif_detected_text, result.chapters.size),
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

        /** İş başlarken çağrılır (uygulama ön plandayken). Başlatılamazsa iş yine sürer, yalnızca korumasız kalır. */
        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, ArchiveService::class.java))
            }
        }
    }
}
