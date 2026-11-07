package com.yukselis.protocol.data

import android.content.Context
import android.content.res.AssetManager
import androidx.core.content.edit
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
    /** Oturumlar arası bildirimler (isteğe bağlı; varsayılan kapalı). */
    val notifications: Boolean = false,
    /** Yazı boyutu: tüm sp ölçüleri bu oranla büyür (cihazın yazı ölçeğinin üstüne). */
    val textSize: TextSize = TextSize.MEDIUM,
    /** Savaş günlüğünde hasar sayıları. */
    val combatNumbers: Boolean = false,
    /** Bir kez gösterilen ipuçları (sembol açıklamaları); görüldükçe eklenir. */
    val seenTips: Set<String> = emptySet(),
)

/** Yazı boyutu seçenekleri. */
enum class TextSize(val scale: Float) { SMALL(0.9f), MEDIUM(1f), LARGE(1.15f), XLARGE(1.3f) }

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** İlk açılışta cihaz dili; sonra oyuncunun seçimi. */
    fun language(): Lang =
        prefs.getString(KEY_LANG, null)?.let { Lang.fromCode(it) } ?: Lang.fromCode(Locale.getDefault().language)

    fun setLanguage(lang: Lang) {
        prefs.edit { putString(KEY_LANG, lang.code) }
    }

    fun settings(): Settings = Settings(
        typewriter = prefs.getBoolean(KEY_TYPEWRITER, true),
        haptics = prefs.getBoolean(KEY_HAPTICS, true),
        theme = prefs.getString(KEY_THEME, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
        notifications = prefs.getBoolean(KEY_NOTIFICATIONS, false),
        textSize = prefs.getString(KEY_TEXT_SIZE, null)
            ?.let { name -> TextSize.entries.firstOrNull { it.name == name } } ?: TextSize.MEDIUM,
        combatNumbers = prefs.getBoolean(KEY_COMBAT_NUMBERS, false),
        seenTips = prefs.getStringSet(KEY_SEEN_TIPS, emptySet())?.toSet() ?: emptySet(),
    )

    fun save(settings: Settings) {
        prefs.edit {
            putBoolean(KEY_TYPEWRITER, settings.typewriter)
            putBoolean(KEY_HAPTICS, settings.haptics)
            // Kaldırılan yapay zeka anlatıcı ayarının eski kaydı silinir (DECISIONS #97).
            remove(LEGACY_NARRATOR)
            putString(KEY_THEME, settings.theme.name)
            putBoolean(KEY_NOTIFICATIONS, settings.notifications)
            putString(KEY_TEXT_SIZE, settings.textSize.name)
            putBoolean(KEY_COMBAT_NUMBERS, settings.combatNumbers)
            putStringSet(KEY_SEEN_TIPS, settings.seenTips)
        }
    }

    // ---- Bildirimler (Aşama 9) ----

    fun lastNotificationMillis(): Long = prefs.getLong(KEY_NOTIF_TIME, 0L)

    fun lastNotificationKey(): String? = prefs.getString(KEY_NOTIF_KEY, null)

    /** Önceki kontrolde rakip sıralamada öndeydi ("seni geçti" yalnızca geçişte). */
    fun rivalWasAhead(): Boolean = prefs.getBoolean(KEY_RIVAL_AHEAD, true)

    fun setRivalWasAhead(ahead: Boolean) {
        prefs.edit { putBoolean(KEY_RIVAL_AHEAD, ahead) }
    }

    fun markNotified(millis: Long, key: String) {
        prefs.edit { putLong(KEY_NOTIF_TIME, millis); putString(KEY_NOTIF_KEY, key) }
    }

    /** Android 13+ izni reddedildi: bir daha kendiliğinden istenmez (ısrar yok). */
    fun notificationPermissionDenied(): Boolean = prefs.getBoolean(KEY_NOTIF_DENIED, false)

    fun setNotificationPermissionDenied(denied: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIF_DENIED, denied) }
    }

    // ---- Aşama 7: oyunlar arası meta (Döngü sonu, New Game+) ----

    /** Bu cihazda bitirilmiş oyun sayısı (Döngü sonunun koşulu): farklı oyun tohumlarının sayısı. */
    fun completedGames(): Int = maxOf(prefs.getInt(KEY_COMPLETED, 0), finishedSeeds().size)

    private fun finishedSeeds(): Set<String> = prefs.getStringSet(KEY_SEEDS, emptySet()) ?: emptySet()

    /** Döngü sonu görüldü: başlıkta New Game+ açılır. */
    fun newGamePlusUnlocked(): Boolean = prefs.getBoolean(KEY_NG_PLUS, false)

    fun endingsSeen(): Set<String> = prefs.getStringSet(KEY_ENDINGS, emptySet()) ?: emptySet()

    /**
     * Bir oyunun sonunu kaydeder. Bitirilmiş oyun sayısı farklı tohumlardan gelir (aynı oyun ya da eski bir kaydın
     * yeniden yüklenmesi iki kez sayılmaz); görülen sonlar ve New Game+ her seferinde birleşir.
     */
    fun recordEnding(gameSeed: Long, endingId: String, unlocksNewGamePlus: Boolean) {
        prefs.edit {
            putStringSet(KEY_SEEDS, finishedSeeds() + gameSeed.toString())
            putStringSet(KEY_ENDINGS, endingsSeen() + endingId)
            putBoolean(KEY_NG_PLUS, newGamePlusUnlocked() || unlocksNewGamePlus)
        }
    }

    private companion object {
        const val KEY_COMPLETED = "meta_completed"
        const val KEY_NG_PLUS = "meta_ng_plus"
        const val KEY_ENDINGS = "meta_endings"
        const val KEY_SEEDS = "meta_finished_seeds"
        const val KEY_LANG = "lang"
        const val KEY_TYPEWRITER = "typewriter"
        const val KEY_HAPTICS = "haptics"
        const val LEGACY_NARRATOR = "narrator"
        const val KEY_THEME = "theme"
        const val KEY_NOTIFICATIONS = "notifications"
        const val KEY_TEXT_SIZE = "text_size"
        const val KEY_COMBAT_NUMBERS = "combat_numbers"
        const val KEY_SEEN_TIPS = "seen_tips"
        const val KEY_NOTIF_TIME = "notif_last_time"
        const val KEY_NOTIF_KEY = "notif_last_key"
        const val KEY_NOTIF_DENIED = "notif_denied"
        const val KEY_RIVAL_AHEAD = "notif_rival_ahead"
    }
}
