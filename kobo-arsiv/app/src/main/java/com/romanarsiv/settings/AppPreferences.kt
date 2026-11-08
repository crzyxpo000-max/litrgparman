package com.romanarsiv.settings

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import com.romanarsiv.background.ArchiveJobs
import com.romanarsiv.domain.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Koyu temada saf siyah arka plan: OLED ekranlarda pil tasarrufu ve gerçek siyah. */
enum class AmoledMode {
    /** Ekran OLED görünüyorsa açık ([ScreenInfo.isLikelyOled]). */
    AUTO,
    ON,
    OFF,
}

/** Uygulama dili; [SYSTEM] cihaz dilini izler (Türkçe değilse İngilizce). */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    TURKISH("tr"),
    ENGLISH("en"),
}

data class UiPrefs(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val amoled: AmoledMode = AmoledMode.AUTO,
    val dynamicColor: Boolean = true,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val maxParallel: Int = ArchiveJobs.DEFAULT_PARALLEL,
    val authorName: String = AppConfig.AUTHOR_NAME,
)

/** Kullanıcı ayarları (SharedPreferences); arayüz [prefs] akışını izler. */
class AppPreferences(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _prefs = MutableStateFlow(read())
    val prefs: StateFlow<UiPrefs> = _prefs.asStateFlow()

    val current: UiPrefs get() = _prefs.value

    fun update(change: (UiPrefs) -> UiPrefs) {
        val next = change(_prefs.value).let { it.copy(maxParallel = it.maxParallel.coerceIn(1, ArchiveJobs.MAX_PARALLEL)) }
        _prefs.value = next
        sp.edit {
            putString(KEY_THEME, next.theme.name)
            putString(KEY_AMOLED, next.amoled.name)
            putBoolean(KEY_DYNAMIC, next.dynamicColor)
            putString(KEY_LANGUAGE, next.language.name)
            putInt(KEY_PARALLEL, next.maxParallel)
            putString(KEY_AUTHOR, next.authorName)
        }
    }

    private fun read() = UiPrefs(
        theme = enumOr(sp.getString(KEY_THEME, null), ThemeMode.SYSTEM),
        amoled = enumOr(sp.getString(KEY_AMOLED, null), AmoledMode.AUTO),
        dynamicColor = sp.getBoolean(KEY_DYNAMIC, true),
        language = enumOr(sp.getString(KEY_LANGUAGE, null), AppLanguage.SYSTEM),
        maxParallel = sp.getInt(KEY_PARALLEL, ArchiveJobs.DEFAULT_PARALLEL).coerceIn(1, ArchiveJobs.MAX_PARALLEL),
        authorName = sp.getString(KEY_AUTHOR, null) ?: AppConfig.AUTHOR_NAME,
    )

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default

    companion object {
        private const val FILE = "ayarlar"
        private const val KEY_THEME = "tema"
        private const val KEY_AMOLED = "amoled"
        private const val KEY_DYNAMIC = "dinamik_renk"
        private const val KEY_LANGUAGE = "dil"
        private const val KEY_PARALLEL = "paralel"
        private const val KEY_AUTHOR = "yazar"

        /**
         * Activity ve hizmetin `attachBaseContext`'inde çağrılır: seçilen dili uygular.
         * SharedPreferences doğrudan okunur, çünkü bu an Application.onCreate'ten önce olabilir.
         */
        fun localize(base: Context): Context {
            val name = base.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
            val language = AppLanguage.entries.firstOrNull { it.name == name } ?: AppLanguage.SYSTEM
            if (language == AppLanguage.SYSTEM) return base
            val config = Configuration(base.resources.configuration)
            config.setLocale(Locale.forLanguageTag(language.tag))
            return base.createConfigurationContext(config)
        }
    }
}
