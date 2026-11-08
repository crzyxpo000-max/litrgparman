package com.romanarsiv

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Android'in düzenli ifade motoru ICU'dur; JVM'de çalışan bazı söz dizimleri telefonda
 * PatternSyntaxException / ExceptionInInitializerError ile uygulamayı kapatır. JVM testleri bunu
 * yakalayamadığı için kaynak kodu tarar.
 */
class RegexAndroidSafetyTest {

    private val forbidden = listOf(
        // (?U) / (?u) / (?iU) …: UNICODE_CHARACTER_CLASS ve UNICODE_CASE gömülü bayrakları
        Regex("""\(\?[a-zA-Z]*[uU][a-zA-Z]*[):]""") to "gömülü (?U)/(?u) bayrağı; RegexOption.IGNORE_CASE kullan",
        Regex("""UNICODE_CHARACTER_CLASS|CANON_EQ""") to "Android'de desteklenmeyen Pattern bayrağı",
        Regex("""\\\\p\{(?:Lower|Upper|ASCII|Alpha|Digit|Alnum|Punct|Graph|Print|Blank|Cntrl|XDigit|Space|java\w+)\}""") to
            "POSIX/java karakter sınıfı; açık aralık ya da \\p{L} gibi Unicode özelliği kullan",
    )

    @Test
    fun `ana kaynaklarda Android'de çöken düzenli ifade söz dizimi yok`() {
        val root = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }
        val problems = root.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            file.readLines().withIndex().filterNot { (_, line) ->
                line.trimStart().let { it.startsWith("//") || it.startsWith("*") || it.startsWith("/*") }
            }.flatMap { (i, line) ->
                forbidden.filter { (re, _) -> re.containsMatchIn(line) }
                    .map { (_, why) -> "${file.path}:${i + 1}: $why\n    ${line.trim()}" }
            }
        }.toList()
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
