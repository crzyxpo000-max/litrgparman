package com.romanarsiv

import android.app.Application
import com.romanarsiv.background.ArchiveNotifications
import com.romanarsiv.di.AppContainer

class ArchiverApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        ArchiveNotifications.createChannels(this)
        container = AppContainer(this)
    }
}
