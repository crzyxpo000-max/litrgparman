package com.romanarsiv

import android.app.Application
import com.romanarsiv.background.ArchiveNotifications
import com.romanarsiv.di.AppContainer
import com.romanarsiv.settings.AppPreferences

class ArchiverApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        // Kanal adları seçili dilde görünsün.
        ArchiveNotifications.createChannels(AppPreferences.localize(this))
        container = AppContainer(this)
    }
}
