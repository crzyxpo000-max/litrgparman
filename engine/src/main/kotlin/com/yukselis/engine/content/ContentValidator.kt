package com.yukselis.engine.content

import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.NameEntry
import com.yukselis.engine.i18n.TokenResolver
import com.yukselis.engine.story.Condition
import com.yukselis.engine.story.Content
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.SystemTexts
import com.yukselis.engine.system.Affinity
import com.yukselis.engine.system.ClassKind
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.ItemType
import java.io.File
import kotlin.system.exitProcess

/**
 * İçerik doğrulayıcı. Derlemeden önce yakalanması gereken her şeyi raporlar:
 * tanımsız sahne/unvan/yetenek/sınıf/eşya, ulaşılamayan sahne, çözülemeyen isim belirteci,
 * eksik çeviri, eksik Sistem metni ya da yer tutucusu, kırık evrim/kaynaştırma/tarif referansı.
 */
object ContentValidator {

    /** Günlük Görev türleri; `gunluk` etkisi de bunlardan birini kullanmalı. */
    val DAILY_KINDS = setOf("antrenman", "kesif", "savas", "kapi")

    fun validate(content: Content): List<String> {
        val problems = mutableListOf<String>()
        val resolver = TokenResolver(content.names)

        fun checkText(text: LText, where: String) {
            if (!text.isComplete) problems += "$where: eksik çeviri (tr='${text.tr.take(30)}', en='${text.en.take(30)}')"
            (resolver.unresolvedTokens(text.tr) + resolver.unresolvedTokens(text.en)).distinct().forEach {
                problems += "$where: çözülemeyen belirteç $it"
            }
        }

        fun checkCondition(c: Condition?, where: String) {
            when (c) {
                null -> Unit
                is Condition.All -> c.conditions.forEach { checkCondition(it, where) }
                is Condition.HasSkill -> if (c.skillId !in content.skills) problems += "$where: koşulda tanımsız yetenek '${c.skillId}'"
                is Condition.HasTitle -> if (c.titleId !in content.titles) problems += "$where: koşulda tanımsız unvan '${c.titleId}'"
                is Condition.HasClass -> if (c.classId !in content.classes) problems += "$where: koşulda tanımsız sınıf '${c.classId}'"
                is Condition.HasItem -> if (c.itemId !in content.items) problems += "$where: koşulda tanımsız eşya '${c.itemId}'"
                else -> Unit
            }
        }

        fun checkEffect(e: Effect, where: String) {
            when (e) {
                is Effect.GrantTitle -> if (e.titleId !in content.titles) problems += "$where: tanımsız unvan '${e.titleId}'"
                is Effect.GrantSkill -> if (e.skillId !in content.skills) problems += "$where: tanımsız yetenek '${e.skillId}'"
                is Effect.SkillXp -> if (e.skillId !in content.skills) problems += "$where: tanımsız yetenek '${e.skillId}'"
                is Effect.AddItem -> if (e.itemId !in content.items) problems += "$where: tanımsız eşya '${e.itemId}'"
                is Effect.EssenceInsight -> if (e.essenceId !in content.essences) problems += "$where: tanımsız Öz '${e.essenceId}'"
                is Effect.DailyProgress -> if (e.kind !in DAILY_KINDS) problems += "$where: bilinmeyen Günlük Görev türü '${e.kind}'"
                else -> Unit
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

        // ---- Sistem metinleri ----
        if (content.classes.isNotEmpty() || content.skills.isNotEmpty() || content.texts.keys.isNotEmpty()) {
            SystemTexts.REQUIRED.forEach { (key, placeholders) ->
                val t = content.texts[key]
                if (t == null) {
                    problems += "system.json: eksik anahtar '$key'"
                } else {
                    checkText(t, "system.$key")
                    placeholders.forEach { ph ->
                        if ("{$ph}" !in t.tr || "{$ph}" !in t.en) problems += "system.$key: '{$ph}' yer tutucusu iki dilde de olmalı"
                    }
                }
            }
        }

        // ---- Yetenekler ----
        content.skills.values.forEach { s ->
            val at = "yetenek ${s.id}"
            checkText(s.name, "$at.ad")
            checkText(s.description, "$at.aciklama")
            if (s.evolutions.isNotEmpty() && s.evolutions.size != 2) problems += "$at: evrim tam 2 dal olmalı"
            s.evolutions.forEach { if (it !in content.skills) problems += "$at: tanımsız evrim dalı '$it'" }
            if (s.id in s.evolutions) problems += "$at: kendine evrilemez"
        }
        content.fusions.forEach { f ->
            val at = "kaynaştırma ${f.id}"
            if (f.inputs.size != 2) problems += "$at: tam 2 girdi olmalı"
            (f.inputs + f.result).forEach { if (it !in content.skills) problems += "$at: tanımsız yetenek '$it'" }
            checkCondition(f.condition, at)
        }

        // ---- Sınıflar ----
        content.classes.values.forEach { c ->
            val at = "sınıf ${c.id}"
            checkText(c.name, "$at.ad")
            checkText(c.description, "$at.aciklama")
            c.skill?.let { if (it !in content.skills) problems += "$at: tanımsız yetenek '$it'" } ?: run { problems += "$at: 'yetenek' eksik" }
            if (c.growth.isEmpty()) problems += "$at: seviye başı stat artışı ('artis') eksik"
            when (c.kind) {
                ClassKind.BASE -> if (c.affinity == null) problems += "$at: temel sınıfın 'egilim'i yok"
                ClassKind.HIDDEN, ClassKind.RARE -> if (c.unlock == null) problems += "$at: açılma koşulu ('kosul') yok"
                ClassKind.EVOLVED -> {
                    if (c.evolvesFrom !in content.classes) problems += "$at: tanımsız önceki sınıf '${c.evolvesFrom}'"
                    if (c.grade == null || c.grade == Grade.F) problems += "$at: evrim kademesi E ya da üstü olmalı"
                }
            }
            checkCondition(c.unlock, at)
        }
        if (content.classes.isNotEmpty()) {
            Affinity.entries.forEach { a ->
                val n = content.classes.values.count { it.kind == ClassKind.BASE && it.affinity == a }
                if (n != 1) problems += "sınıflar: '${a.key}' eğilimi için tam 1 temel sınıf olmalı ($n var)"
            }
            content.classes.values.filter { it.kind == ClassKind.BASE || it.kind == ClassKind.HIDDEN }.forEach { c ->
                val n = content.classes.values.count { it.kind == ClassKind.EVOLVED && it.evolvesFrom == c.id && it.grade == Grade.E }
                if (n != 2) problems += "sınıf ${c.id}: E kademesi için 2 evrim seçeneği olmalı ($n var)"
            }
        }

        // ---- Eşyalar, meslekler, Öz, Günlük Görev ----
        content.items.values.forEach { i ->
            checkText(i.name, "eşya ${i.id}.ad")
            checkText(i.description, "eşya ${i.id}.aciklama")
            if (i.type == ItemType.CONSUMABLE && i.use == null) problems += "eşya ${i.id}: tüketim eşyasının 'kullan'ı yok"
        }
        content.professions.values.forEach { checkText(it.name, "meslek ${it.id}.ad"); checkText(it.description, "meslek ${it.id}.aciklama") }
        content.recipes.values.forEach { r ->
            val at = "tarif ${r.id}"
            if (r.profession !in content.professions) problems += "$at: tanımsız meslek '${r.profession}'"
            if (r.output !in content.items) problems += "$at: tanımsız ürün '${r.output}'"
            r.inputs.keys.forEach { if (it !in content.items) problems += "$at: tanımsız malzeme '$it'" }
            if (r.inputs.isEmpty()) problems += "$at: malzeme yok"
        }
        content.essences.values.forEach { checkText(it.name, "Öz ${it.id}.ad"); checkText(it.description, "Öz ${it.id}.aciklama") }
        content.dailyTemplates.values.forEach { d ->
            checkText(d.title, "günlük ${d.id}.baslik")
            checkText(d.description, "günlük ${d.id}.aciklama")
            if (d.kind !in DAILY_KINDS) problems += "günlük ${d.id}: bilinmeyen tür '${d.kind}'"
            if (d.steps < 1) problems += "günlük ${d.id}: adim en az 1"
        }

        // ---- Sahneler ----
        content.scenes.values.forEach { scene ->
            val at = "sahne ${scene.id}"
            if (scene.place != null && content.names.find(NameEntry.Kind.PLACE, scene.place) == null) {
                problems += "$at: tanımsız yer '${scene.place}'"
            }
            if (scene.paragraphs.isEmpty()) problems += "$at: paragraf yok"
            scene.paragraphs.forEachIndexed { i, p ->
                checkText(p.text, "$at paragraf ${i + 1}")
                checkCondition(p.condition, "$at paragraf ${i + 1}")
            }
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
                val where = "$at seçenek ${c.id}"
                checkText(c.label, where)
                checkCondition(c.requires, where)
                checkCondition(c.showWhen, where)
                when {
                    c.next == null -> problems += "$where: 'sonraki' eksik"
                    c.next == Content.RETURN -> if (scene.id != Content.PENALTY_SCENE) problems += "$where: '@geri' yalnızca kesinti sahnelerinde kullanılabilir"
                    c.next !in content.scenes -> problems += "$where: tanımsız sonraki sahne '${c.next}'"
                }
                c.effects.forEach { checkEffect(it, where) }
            }
        }

        // Başlangıçtan (ve kesinti kancalarından) ulaşılamayan sahneler
        val roots = listOf(content.startScene) + listOf(Content.PENALTY_SCENE).filter { it in content.scenes }
        val reachable = mutableSetOf<String>()
        val queue = ArrayDeque(roots)
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!reachable.add(id)) continue
            content.scenes[id]?.choices?.mapNotNull { it.next }?.filter { it != Content.RETURN }?.forEach { queue.addLast(it) }
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
