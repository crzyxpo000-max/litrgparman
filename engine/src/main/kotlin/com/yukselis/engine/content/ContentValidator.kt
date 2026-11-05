package com.yukselis.engine.content

import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.TokenResolver
import com.yukselis.engine.story.Content
import com.yukselis.engine.story.Effect
import java.io.File
import kotlin.system.exitProcess

/**
 * İçerik doğrulayıcı. Derlemeden önce yakalanması gereken her şeyi raporlar:
 * tanımsız sahne, ulaşılamayan sahne, çözülemeyen isim belirteci, eksik çeviri, tanımsız unvan.
 */
object ContentValidator {

    fun validate(content: Content): List<String> {
        val problems = mutableListOf<String>()
        val resolver = TokenResolver(content.names)

        fun checkText(text: LText, where: String) {
            if (!text.isComplete) problems += "$where: eksik çeviri (tr='${text.tr.take(30)}', en='${text.en.take(30)}')"
            (resolver.unresolvedTokens(text.tr) + resolver.unresolvedTokens(text.en)).distinct().forEach {
                problems += "$where: çözülemeyen belirteç $it"
            }
        }

        if (content.startScene !in content.scenes) problems += "Başlangıç sahnesi tanımsız: ${content.startScene}"

        content.names.all.forEach { n ->
            checkText(n.full, "isim ${n.id}.ad")
            checkText(n.short, "isim ${n.id}.kisa_ad")
        }
        content.titles.values.forEach { t ->
            checkText(t.name, "unvan ${t.id}.ad")
            checkText(t.description, "unvan ${t.id}.aciklama")
        }

        content.scenes.values.forEach { scene ->
            val at = "sahne ${scene.id}"
            if (scene.place != null && content.names.find(com.yukselis.engine.i18n.NameEntry.Kind.PLACE, scene.place) == null) {
                problems += "$at: tanımsız yer '${scene.place}'"
            }
            if (scene.paragraphs.isEmpty()) problems += "$at: paragraf yok"
            scene.paragraphs.forEachIndexed { i, p -> checkText(p.text, "$at paragraf ${i + 1}") }
            scene.notices.forEachIndexed { i, n ->
                checkText(n.notice.title, "$at bildirim ${i + 1} başlık")
                n.notice.lines.forEachIndexed { j, line -> checkText(line, "$at bildirim ${i + 1} satır ${j + 1}") }
            }
            if (!scene.isEnding && scene.choices.isEmpty()) problems += "$at: son sahne değil ama seçeneği yok"
            if (scene.isEnding && scene.choices.isNotEmpty()) problems += "$at: son sahnede seçenek olamaz"
            scene.choices.groupBy { it.id }.filterValues { it.size > 1 }.keys.forEach {
                problems += "$at: '$it' seçenek kimliği tekrar ediyor"
            }
            scene.choices.forEach { c ->
                checkText(c.label, "$at seçenek ${c.id}")
                if (c.next == null) problems += "$at seçenek ${c.id}: 'sonraki' eksik"
                else if (c.next !in content.scenes) problems += "$at seçenek ${c.id}: tanımsız sonraki sahne '${c.next}'"
                c.effects.filterIsInstance<Effect.GrantTitle>().forEach {
                    if (it.titleId !in content.titles) problems += "$at seçenek ${c.id}: tanımsız unvan '${it.titleId}'"
                }
            }
        }

        // Başlangıçtan ulaşılamayan sahneler
        val reachable = mutableSetOf<String>()
        val queue = ArrayDeque(listOf(content.startScene))
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!reachable.add(id)) continue
            content.scenes[id]?.choices?.mapNotNull { it.next }?.forEach { queue.addLast(it) }
        }
        (content.scenes.keys - reachable).sorted().forEach { problems += "sahne $it: başlangıçtan ulaşılamıyor" }

        return problems
    }
}

/** Gradle: ./gradlew :engine:validateContent */
fun main(args: Array<String>) {
    val dir = File(args.firstOrNull() ?: "content")
    val problems = try {
        ContentValidator.validate(ContentLoader.load(DirectoryContentSource(dir)))
    } catch (e: ContentFormatException) {
        listOf("Biçim hatası: ${e.message}")
    }
    if (problems.isEmpty()) {
        println("İçerik geçerli: ${dir.absolutePath}")
    } else {
        problems.forEach { System.err.println("✗ $it") }
        System.err.println("${problems.size} sorun bulundu.")
        exitProcess(1)
    }
}
