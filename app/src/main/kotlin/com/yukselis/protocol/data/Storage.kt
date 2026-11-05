package com.yukselis.protocol.data

import android.content.Context
import android.content.res.AssetManager
import com.yukselis.engine.content.ContentSource
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.SaveStorage
import java.io.File
import java.util.Locale

/** content/ klasörü APK'ya assets olarak girer (app/build.gradle.kts → assets.srcDirs). */
class AssetContentSource(private val assets: AssetManager) : ContentSource {
    override fun read(path: String): String =
        assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    override fun list(dir: String): List<String> = assets.list(dir)?.toList() ?: emptyList()
}

/** Kayıtlar uygulamanın özel depolamasında, files/saves/<slot>.json olarak durur. */
class FileSaveStorage(private val dir: File) : SaveStorage {
    init {
        dir.mkdirs()
    }

    private fun file(name: String) = File(dir, "$name.json")

    override fun read(name: String): String? = file(name).takeIf { it.exists() }?.readText(Charsets.UTF_8)

    override fun write(name: String, data: String) {
        // Önce geçici dosyaya yaz, sonra yerine taşı: yazma yarıda kalırsa eski kayıt bozulmaz.
        val tmp = File(dir, "$name.json.tmp")
        tmp.writeText(data, Charsets.UTF_8)
        if (!tmp.renameTo(file(name))) {
            file(name).writeText(data, Charsets.UTF_8)
            tmp.delete()
        }
    }

    override fun delete(name: String) {
        file(name).delete()
    }
}

/** Açık/koyu tema seçimi; SYSTEM cihaz ayarını izler. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val typewriter: Boolean = true,
    val haptics: Boolean = true,
    val narratorMode: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** İlk açılışta cihaz dili; sonra oyuncunun seçimi. */
    fun language(): Lang =
        prefs.getString(KEY_LANG, null)?.let { Lang.fromCode(it) } ?: Lang.fromCode(Locale.getDefault().language)

    fun setLanguage(lang: Lang) {
        prefs.edit().putString(KEY_LANG, lang.code).apply()
    }

    fun settings(): Settings = Settings(
        typewriter = prefs.getBoolean(KEY_TYPEWRITER, true),
        haptics = prefs.getBoolean(KEY_HAPTICS, true),
        narratorMode = prefs.getBoolean(KEY_NARRATOR, false),
        theme = prefs.getString(KEY_THEME, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
    )

    fun save(settings: Settings) {
        prefs.edit()
            .putBoolean(KEY_TYPEWRITER, settings.typewriter)
            .putBoolean(KEY_HAPTICS, settings.haptics)
            .putBoolean(KEY_NARRATOR, settings.narratorMode)
            .putString(KEY_THEME, settings.theme.name)
            .apply()
    }

    private companion object {
        const val KEY_LANG = "lang"
        const val KEY_TYPEWRITER = "typewriter"
        const val KEY_HAPTICS = "haptics"
        const val KEY_NARRATOR = "narrator"
        const val KEY_THEME = "theme"
    }
}
