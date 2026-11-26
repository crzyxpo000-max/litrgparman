package com.yukselis.protocol

import android.app.Application
import com.yukselis.engine.content.ContentCache
import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.save.SaveManager
import com.yukselis.engine.story.StoryEngine
import com.yukselis.protocol.data.FileSaveStorage
import com.yukselis.protocol.data.SettingsStore
import java.io.File

/** Uygulama genelinde tek örnek: içerik, motor, kayıtlar ve ayarlar. */
class YukselisApplication : Application() {

    val engine: StoryEngine by lazy {
        // Derleme zamanında üretilen ikili önbellekten yüklenir (DECISIONS #179). YAML kaynakları pakette yoktur; önbellek
        // okunamazsa hata olduğu gibi yükselir (GameViewModel günlüğe yazar), başka bir hatayla örtülmez.
        StoryEngine(assets.open(ContentCache.FILE_NAME).use { ContentLoader.loadFromCache(it) })
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
