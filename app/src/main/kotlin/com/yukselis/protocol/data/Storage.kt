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

/** Tema seçimi; SYSTEM cihaz ayarını izler, AMOLED saf siyah koyu temadır. */
enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

data class Settings(
    val typewriter: Boolean = true,
    val haptics: Boolean = true,
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
        theme = prefs.getString(KEY_THEME, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
    )

    fun save(settings: Settings) {
        prefs.edit()
            .putBoolean(KEY_TYPEWRITER, settings.typewriter)
            .putBoolean(KEY_HAPTICS, settings.haptics)
            // Kaldırılan yapay zeka anlatıcı ayarının eski kaydı silinir (DECISIONS #97).
            .remove(LEGACY_NARRATOR)
            .putString(KEY_THEME, settings.theme.name)
            .apply()
    }

    // ---- Aşama 7: oyunlar arası meta (Döngü sonu, New Game+) ----

    /** Bu cihazda bitirilmiş oyun sayısı (Döngü sonunun koşulu). */
    fun completedGames(): Int = prefs.getInt(KEY_COMPLETED, 0)

    /** Döngü sonu görüldü: başlıkta New Game+ açılır. */
    fun newGamePlusUnlocked(): Boolean = prefs.getBoolean(KEY_NG_PLUS, false)

    fun endingsSeen(): Set<String> = prefs.getStringSet(KEY_ENDINGS, emptySet()) ?: emptySet()

    /** Bir oyunun sonunu bir kez kaydeder (aynı oyun tohumu iki kez sayılmaz). */
    fun recordEnding(gameSeed: Long, endingId: String, unlocksNewGamePlus: Boolean) {
        if (prefs.getLong(KEY_LAST_SEED, 0L) == gameSeed && prefs.contains(KEY_LAST_SEED)) return
        prefs.edit()
            .putLong(KEY_LAST_SEED, gameSeed)
            .putInt(KEY_COMPLETED, completedGames() + 1)
            .putStringSet(KEY_ENDINGS, endingsSeen() + endingId)
            .putBoolean(KEY_NG_PLUS, newGamePlusUnlocked() || unlocksNewGamePlus)
            .apply()
    }

    private companion object {
        const val KEY_COMPLETED = "meta_completed"
        const val KEY_NG_PLUS = "meta_ng_plus"
        const val KEY_ENDINGS = "meta_endings"
        const val KEY_LAST_SEED = "meta_last_seed"
        const val KEY_LANG = "lang"
        const val KEY_TYPEWRITER = "typewriter"
        const val KEY_HAPTICS = "haptics"
        const val LEGACY_NARRATOR = "narrator"
        const val KEY_THEME = "theme"
    }
}
