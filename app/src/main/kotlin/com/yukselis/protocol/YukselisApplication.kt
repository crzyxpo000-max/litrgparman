package com.yukselis.protocol

import android.app.Application
import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.save.SaveManager
import com.yukselis.engine.story.StoryEngine
import com.yukselis.protocol.data.AssetContentSource
import com.yukselis.protocol.data.FileSaveStorage
import com.yukselis.protocol.data.SettingsStore
import java.io.File

/** Uygulama genelinde tek örnek: içerik, motor, kayıtlar ve ayarlar. */
class YukselisApplication : Application() {

    val engine: StoryEngine by lazy {
        StoryEngine(ContentLoader.load(AssetContentSource(assets)))
    }

    val saves: SaveManager by lazy {
        SaveManager(FileSaveStorage(File(filesDir, "saves")))
    }

    val settings: SettingsStore by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        // Ayar ile gerçek izin uyumlu kalsın (yedekten geri yükleme, sistemden izin geri alma): zamanlayıcı yeniden kurulur.
        val s = settings.settings()
        val on = s.notifications && com.yukselis.protocol.data.Notifications.permitted(this)
        if (s.notifications && !on) settings.save(s.copy(notifications = false))
        com.yukselis.protocol.data.Notifications.schedule(this, on)
    }
}
