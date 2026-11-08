package com.romanarsiv

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Yakalanmamış hataları dosyaya yazar; bir sonraki açılışta ekranda gösterilir ki kullanıcı
 * logcat olmadan da çökme nedenini kopyalayıp iletebilsin.
 */
object CrashLog {
    private const val FILE_NAME = "son_cokme.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
                val header = "Roman Arşivi ${versionName(app)} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · " +
                    "${Build.MANUFACTURER} ${Build.MODEL} · iş parçacığı: ${thread.name}\n\n"
                File(app.filesDir, FILE_NAME).writeText(header + trace.take(20_000))
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** Kayıtlı çökme metnini döndürür ve siler. */
    fun takeLast(context: Context): String? {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        if (!file.isFile) return null
        return runCatching { file.readText() }.getOrNull().also { file.delete() }
    }

    private fun versionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "?"
}
