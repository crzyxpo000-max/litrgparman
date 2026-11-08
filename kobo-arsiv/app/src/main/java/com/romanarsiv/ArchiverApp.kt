package com.romanarsiv

import android.app.Application
import com.romanarsiv.di.AppContainer

class ArchiverApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        container = AppContainer(this)
    }
}
