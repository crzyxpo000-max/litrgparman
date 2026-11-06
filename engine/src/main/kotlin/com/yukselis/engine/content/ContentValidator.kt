package com.yukselis.engine.content

import com.yukselis.engine.combat.CombatEngine
import com.yukselis.engine.combat.StatusEffects
import com.yukselis.engine.core.WorldState
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

        fun npcRef(id: String, where: String) {
            if (id == Content.FOCUS) return
            if (id !in content.npcs) problems += "$where: tanımsız NPC '$id'"
        }

        fun checkCondition(c: Condition?, where: String) {
            when (c) {
                null -> Unit
                is Condition.NpcAlive -> npcRef(c.npc, where)
                is Condition.NpcDead -> npcRef(c.npc, where)
                is Condition.IsCompanion -> npcRef(c.npc, where)
                is Condition.RelationAtLeast -> npcRef(c.npc, where)
                is Condition.RelationAtMost -> npcRef(c.npc, where)
                is Condition.NpcMarked -> npcRef(c.npc, where)
                is Condition.NpcLevelAtLeast -> npcRef(c.npc, where)
                is Condition.Remembers -> npcRef(c.npc, where)
                is Condition.NpcInFaction -> {
                    npcRef(c.npc, where)
                    if (c.faction !in content.factions) problems += "$where: tanımsız fraksiyon '${c.faction}'"
                }
                is Condition.NpcCanFight -> npcRef(c.npc, where)
                is Condition.Knows -> if (c.knowledge !in content.knowledge) problems += "$where: tanımsız bilgi '${c.knowledge}'"
                is Condition.HasTrait -> if (c.trait !in content.traits) problems += "$where: tanımsız iz '${c.trait}'"
                is Condition.All -> c.conditions.forEach { checkCondition(it, where) }
                is Condition.HasSkill -> if (c.skillId !in content.skills) problems += "$where: koşulda tanımsız yetenek '${c.skillId}'"
                is Condition.HasTitle -> if (c.titleId !in content.titles) problems += "$where: koşulda tanımsız unvan '${c.titleId}'"
                is Condition.HasClass -> if (c.classId !in content.classes) problems += "$where: koşulda tanımsız sınıf '${c.classId}'"
                is Condition.HasItem -> if (c.itemId !in content.items) problems += "$where: koşulda tanımsız eşya '${c.itemId}'"
                is Condition.InRegion -> if (c.region !in content.regions) problems += "$where: koşulda tanımsız bölge '${c.region}'"
                is Condition.QuestIs -> if (c.quest !in content.quests) problems += "$where: koşulda tanımsız görev '${c.quest}'"
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
                is Effect.StartCombat -> {
                    e.spec.enemies.forEach { id ->
                        val npcId = id.removePrefix(com.yukselis.engine.npc.NpcEngine.ENEMY_PREFIX)
                        when {
                            id.startsWith(com.yukselis.engine.npc.NpcEngine.ENEMY_PREFIX) ->
                                if (npcId != Content.FOCUS && content.npcs[npcId]?.combat == null) problems += "$where: '$id' için NPC ya da savaş bloğu yok"
                            id !in content.enemies -> problems += "$where: tanımsız düşman '$id'"
                        }
                    }
                    listOfNotNull(e.spec.win, e.spec.lose, e.spec.flee).forEach {
                        if (it != WorldState.HUB && it !in content.scenes) problems += "$where: savaş sahnesi tanımsız '$it'"
                    }
                    e.spec.environment.forEach { if (it !in CombatEngine.ENVIRONMENTS) problems += "$where: bilinmeyen ortam '$it'" }
                }
                is Effect.PlantSeed -> {
                    if (e.eventId !in content.events) problems += "$where: tohumun hedeflediği olay yok '${e.eventId}'"
                    if (e.maxDelay < e.minDelay || e.minDelay < 0) problems += "$where: tohum gecikmesi geçersiz"
                }
                is Effect.Feat -> if (e.featId !in content.feats) problems += "$where: tanımsız başarı '${e.featId}'"
                is Effect.SetQuest -> if (e.questId !in content.quests) problems += "$where: tanımsız görev '${e.questId}'"
                is Effect.MoveTo -> if (e.region !in content.regions) problems += "$where: tanımsız bölge '${e.region}'"
                is Effect.ShiftDanger -> if (e.region !in content.regions) problems += "$where: tanımsız bölge '${e.region}'"
                is Effect.SetControl -> {
                    if (e.region !in content.regions) problems += "$where: tanımsız bölge '${e.region}'"
                    if (e.control != "canavar" && e.control !in content.factions) problems += "$where: tanımsız kontrol '${e.control}'"
                }
                is Effect.ShiftReputation -> if (content.factions.isNotEmpty() && e.faction !in content.factions) problems += "$where: tanımsız fraksiyon '${e.faction}'"
                is Effect.UnlockTab -> if (e.tab !in com.yukselis.engine.core.WorldState.TABS) problems += "$where: bilinmeyen sekme '${e.tab}'"
                // ---- Aşama 5 ----
                is Effect.ShiftRelation -> npcRef(e.npc, where)
                is Effect.AddMemory -> npcRef(e.npc, where)
                is Effect.KillNpc -> npcRef(e.npc, where)
                is Effect.LeaveParty -> npcRef(e.npc, where)
                is Effect.NpcGone -> npcRef(e.npc, where)
                is Effect.MeetNpc -> npcRef(e.npc, where)
                is Effect.NpcLevel -> npcRef(e.npc, where)
                is Effect.NpcMark -> npcRef(e.npc, where)
                is Effect.NpcReward -> npcRef(e.npc, where)
                is Effect.PersonalQuest -> npcRef(e.npc, where)
                is Effect.TellLie -> npcRef(e.npc, where)
                is Effect.JoinParty -> {
                    npcRef(e.npc, where)
                    content.npcs[e.npc]?.let { if (!it.canJoin) problems += "$where: '${e.npc}' yoldaş olamaz (yoldas: true değil)" }
                }
                is Effect.SetNpcFaction -> {
                    npcRef(e.npc, where)
                    if (e.faction !in content.factions) problems += "$where: tanımsız fraksiyon '${e.faction}'"
                }
                is Effect.NpcTrait -> {
                    npcRef(e.npc, where)
                    val t = content.traits[e.trait]
                    if (t == null) problems += "$where: tanımsız iz '${e.trait}'" else if (t.onPlayer) problems += "$where: '${e.trait}' oyuncu izi, NPC'ye verilemez"
                }
                is Effect.PlayerTrait -> {
                    val t = content.traits[e.trait]
                    if (t == null) problems += "$where: tanımsız iz '${e.trait}'" else if (!t.onPlayer) problems += "$where: '${e.trait}' NPC izi, oyuncuya verilemez"
                }
                is Effect.Learn -> if (e.knowledge !in content.knowledge) problems += "$where: tanımsız bilgi '${e.knowledge}'"
                is Effect.AddArmy -> if (e.unit !in content.army) problems += "$where: tanımsız birlik '${e.unit}'"
                is Effect.GrowArmy -> if (content.army.isEmpty()) problems += "$where: ordu birliği tanımlı değil"
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
        // OP MC: her oyun tarzı için tam bir Benzersiz yetenek.
        if (content.skills.isNotEmpty()) {
            Affinity.entries.forEach { a ->
                val n = content.skills.values.count { it.rarity == com.yukselis.engine.system.Rarity.UNIQUE && it.affinity == a }
                if (n != 1) problems += "yetenekler: '${a.key}' eğilimi için tam 1 Benzersiz yetenek olmalı ($n var)"
            }
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
            content.classes.values.filter { it.kind == ClassKind.HIDDEN && it.affinities.isEmpty() }.forEach {
                problems += "sınıf ${it.id}: gizli sınıfın 'egilimler' listesi yok (Yankı'nın önerisi için)"
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
            checkCondition(d.condition, "günlük ${d.id}")
        }

        // ---- Düşmanlar ve uyanışlar ----
        content.enemies.values.forEach { d ->
            val at = "düşman ${d.id}"
            checkText(d.name, "$at.ad")
            checkText(d.description, "$at.aciklama")
            checkText(d.hook, "$at.kanca")
            d.weaknessText?.let { checkText(it, "$at.zaaf_metin") }
            if (d.attacks.isEmpty()) problems += "$at: saldırı yok"
            if (d.attacks.none { it.cooldown == 0 }) problems += "$at: en az bir beklemesiz saldırı olmalı"
            d.weakness?.let { if (it !in CombatEngine.WEAKNESS_TAGS) problems += "$at: bilinmeyen zaaf '$it'" }
            (d.attacks + d.phases.mapNotNull { it.attack }).forEach { a ->
                checkText(a.name, "$at.${a.id}.ad")
                checkText(a.prose, "$at.${a.id}.metin")
                a.status?.let { if (it.effect !in StatusEffects.ALL) problems += "$at.${a.id}: bilinmeyen durum '${it.effect}'" }
            }
            d.loot.keys.forEach { if (it !in content.items) problems += "$at: tanımsız ganimet '$it'" }
            if (d.boss && d.phases.map { it.threshold } != listOf(70, 40, 15)) problems += "$at: boss fazları 70/40/15 olmalı"
            d.phases.forEach { checkText(it.text, "$at faz ${it.threshold}") }
            if (d.maxHp <= 0) problems += "$at: can pozitif olmalı"
        }
        content.skills.values.forEach { s -> s.status?.let { if (it.effect !in StatusEffects.ALL) problems += "yetenek ${s.id}: bilinmeyen durum '${it.effect}'" } }
        content.awakenings.forEach { a ->
            checkText(a.title, "uyanış ${a.id}.baslik")
            a.lines.forEach { checkText(it, "uyanış ${a.id}.satir") }
            checkCondition(a.condition, "uyanış ${a.id}")
            a.effects.forEach { checkEffect(it, "uyanış ${a.id}") }
        }

        // ---- Olaylar, bölgeler, fraksiyonlar, başarılar, görevler ----
        content.events.values.forEach { ev ->
            val at = "olay ${ev.id}"
            checkText(ev.card, "$at.kart")
            if (ev.entry !in content.scenes) problems += "$at: giriş sahnesi yok '${ev.entry}'"
            ev.regions.forEach { if (it !in content.regions) problems += "$at: tanımsız bölge '$it'" }
            ev.npcs.forEach { if (content.names.find(NameEntry.Kind.NPC, it) == null) problems += "$at: tanımsız NPC '$it'" }
            if (ev.tension !in 1..4) problems += "$at: gerilim 1–4 olmalı"
            if (ev.weight <= 0) problems += "$at: ağırlık pozitif olmalı"
            ev.acts.forEach { if (it !in 1..4) problems += "$at: perde 1–4 olmalı" }
            checkCondition(ev.condition, at)
        }
        content.startRegion?.let { if (it !in content.regions) problems += "regions.yaml: başlangıç bölgesi tanımsız '$it'" }
        content.regions.values.forEach { r ->
            val at = "bölge ${r.id}"
            if (content.names.find(NameEntry.Kind.PLACE, r.id) == null) problems += "$at: names.yaml'da yer adı yok"
            (r.enemies + r.gateEnemies).forEach { if (it !in content.enemies) problems += "$at: tanımsız düşman '$it'" }
            r.neighbors.forEach { if (it !in content.regions) problems += "$at: tanımsız komşu '$it'" }
            r.forage.forEach { if (it !in content.items) problems += "$at: tanımsız toplama eşyası '$it'" }
            if (r.control != "canavar" && r.control !in content.factions) problems += "$at: tanımsız kontrol '${r.control}'"
        }
        content.factions.values.forEach { f ->
            if (content.names.find(NameEntry.Kind.FACTION, f.id) == null) problems += "fraksiyon ${f.id}: names.yaml'da ad yok"
            f.relations.forEach { (o, v) ->
                if (o !in content.factions) problems += "fraksiyon ${f.id}: tanımsız ilişki '$o'"
                if (v !in -100..100) problems += "fraksiyon ${f.id}: ilişki −100…100 olmalı"
            }
        }
        content.feats.values.forEach { f ->
            checkText(f.name, "başarı ${f.id}.ad")
            f.lines.values.forEach { checkText(it, "başarı ${f.id}.satir") }
            f.reactions.keys.forEach { if (content.factions.isNotEmpty() && it !in content.factions) problems += "başarı ${f.id}: tanımsız fraksiyon '$it'" }
            f.title?.let { if (it !in content.titles) problems += "başarı ${f.id}: tanımsız unvan '$it'" }
        }
        content.quests.values.forEach { q -> checkText(q.name, "görev ${q.id}.ad"); checkText(q.description, "görev ${q.id}.aciklama") }

        // ---- NPC'ler (Aşama 5) ----
        content.npcs.values.forEach { d ->
            val at = "NPC ${d.id}"
            if (content.names.find(NameEntry.Kind.NPC, d.id) == null) problems += "$at: names.yaml'da adı yok"
            checkText(d.description, "$at.aciklama")
            if (d.goals.isEmpty()) problems += "$at: en az bir hedef olmalı"
            d.goals.forEach { g ->
                checkText(g.name, "$at.${g.id}.ad")
                checkCondition(g.condition, "$at.${g.id}")
                g.effects.forEach { checkEffect(it, "$at.${g.id}") }
            }
            d.faction?.let { if (it !in content.factions) problems += "$at: tanımsız fraksiyon '$it'" }
            if (d.startLevel < 1) problems += "$at: seviye en az 1"
            if (d.growthRate <= 0) problems += "$at: hiz pozitif olmalı"
            checkCondition(d.presentWhen, "$at.yaninda")
            d.combat?.let { c ->
                checkText(c.attackName, "$at.savas.saldiri")
                checkText(c.prose, "$at.savas.metin")
                checkText(c.duelProse, "$at.savas.duello_metin")
                if ("{npc}" !in c.prose.tr || "{npc}" !in c.prose.en || "{dusman}" !in c.prose.tr || "{dusman}" !in c.prose.en) {
                    problems += "$at.savas.metin: '{npc}' ve '{dusman}' iki dilde de olmalı"
                }
                if ("{dusman}" !in c.duelProse.tr || "{dusman}" !in c.duelProse.en) problems += "$at.savas.duello_metin: '{dusman}' iki dilde de olmalı"
            }
            if (d.canJoin && d.combat == null) problems += "$at: yoldaş olabilen NPC'nin 'savas' bloğu olmalı"
            d.firstClaims.forEach { fc ->
                val t = content.titles[fc.title]
                if (t == null) problems += "$at: tanımsız unvan '${fc.title}'" else if (!t.first) problems += "$at: '${fc.title}' bir 'İlk' unvanı değil"
            }
            val opinionKeys = com.yukselis.engine.core.MoralAxis.entries.flatMap { listOf(com.yukselis.engine.npc.NpcDef.opinionKey(it, true), com.yukselis.engine.npc.NpcDef.opinionKey(it, false)) }
            d.opinions.forEach { (k, lines) ->
                if (k !in opinionKeys) problems += "$at.gorusler: bilinmeyen anahtar '$k' (ör. merhamet+, duzen-)"
                lines.forEach { checkText(it, "$at.gorusler.$k") }
            }
            d.powerLines.forEach { (k, lines) -> lines.forEach { checkText(it, "$at.guc_tepkileri.${k.key}") } }
            d.memories.forEach { (k, t) -> checkText(t, "$at.hafizalar.$k") }
            d.personalReward.forEach { checkEffect(it, "$at.kisisel_odul") }
        }
        if (content.npcs.isNotEmpty()) {
            // Eşik olayları motor tarafından tohumlanır: genel olaylar var, tekrarlanabilir ve yalnızca tohumla gelir.
            com.yukselis.engine.npc.ThresholdKind.entries.forEach { k ->
                val ev = content.events[k.genericEvent]
                when {
                    ev == null -> problems += "eşik olayı '${k.genericEvent}' tanımlı değil"
                    !ev.repeatable -> problems += "olay ${ev.id}: genel eşik olayı 'tekrar: true' olmalı"
                    !ev.seedOnly -> problems += "olay ${ev.id}: eşik olayı 'yalniz_tohum: true' olmalı"
                }
            }
            content.events.values.forEach { ev ->
                ev.npcs.forEach { if (it !in content.npcs) problems += "olay ${ev.id}: '$it' için npcs/ dosyası yok" }
            }
        }
        content.traits.values.forEach { t -> checkText(t.name, "iz ${t.id}.ad"); checkText(t.description, "iz ${t.id}.aciklama") }
        content.knowledge.values.forEach { k -> checkText(k.name, "bilgi ${k.id}.ad"); checkText(k.description, "bilgi ${k.id}.aciklama") }
        content.army.values.forEach { a ->
            checkText(a.name, "birlik ${a.id}.ad"); checkText(a.description, "birlik ${a.id}.aciklama")
            if (a.affinities.isEmpty()) problems += "birlik ${a.id}: 'egilim' listesi boş"
        }
        content.ranking.let { r ->
            (r.firstNames + r.lastNames).forEach { checkText(it, "ranking.yaml ad") }
            if (r.hunters > 0 && (r.firstNames.isEmpty() || r.lastNames.isEmpty())) problems += "ranking.yaml: avcı adları boş"
            r.reactions.forEach { re -> re.effects.forEach { checkEffect(it, "ranking.yaml sira ${re.rank}") } }
        }
        content.factionValues.keys.forEach { if (it !in content.factions) problems += "fraksiyon $it: değerler tanımsız fraksiyon için" }

        // ---- Bayrak kaydı: her bayrak flags.yaml'da tanımlı olmalı ----
        val usage = flagUsage(content)
        (usage.read + usage.written).filterNot { isEngineFlag(it) }.toSortedSet().forEach { f ->
            if (f !in content.flags) problems += "bayrak '$f': flags.yaml'da tanımlı değil"
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
            scene.insights.forEachIndexed { i, ins ->
                checkText(ins.text, "$at sezgi ${i + 1}")
                checkCondition(ins.condition, "$at sezgi ${i + 1}")
            }
            scene.companionLines.forEachIndexed { i, l ->
                npcRef(l.npc, "$at yoldaş tepkisi ${i + 1}")
                checkText(l.text, "$at yoldaş tepkisi ${i + 1}")
                checkCondition(l.condition, "$at yoldaş tepkisi ${i + 1}")
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
                val fights = c.effects.any { it is Effect.StartCombat }
                c.check?.let { chk ->
                    chk.failNext?.let { n -> if (n != WorldState.HUB && n != Content.RETURN && n !in content.scenes) problems += "$where: zar başarısız sahnesi tanımsız '$n'" }
                    chk.failEffects.forEach { checkEffect(it, "$where (başarısız)") }
                    if (chk.difficulty !in 2..40) problems += "$where: zar zorluğu 2–40 olmalı"
                }
                when {
                    c.next == WorldState.HUB -> Unit
                    c.next == null && fights -> Unit
                    c.next == null -> problems += "$where: 'sonraki' eksik"
                    c.next == Content.RETURN -> if (scene.id != Content.PENALTY_SCENE) problems += "$where: '@geri' yalnızca kesinti sahnelerinde kullanılabilir"
                    c.next !in content.scenes -> problems += "$where: tanımsız sonraki sahne '${c.next}'"
                }
                c.effects.forEach { checkEffect(it, where) }
            }
        }

        // Başlangıçtan (ve kesinti kancalarından) ulaşılamayan sahneler
        val roots = listOf(content.startScene) + listOf(Content.PENALTY_SCENE, Content.HARVEST_SCENE).filter { it in content.scenes } +
            content.events.values.map { it.entry }
        val reachable = mutableSetOf<String>()
        val queue = ArrayDeque(roots)
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!reachable.add(id)) continue
            content.scenes[id]?.choices?.forEach { c ->
                c.next?.takeIf { it != Content.RETURN && it != WorldState.HUB }?.let { queue.addLast(it) }
                c.check?.failNext?.takeIf { it != Content.RETURN && it != WorldState.HUB }?.let { queue.addLast(it) }
                (c.effects + (c.check?.failEffects ?: emptyList())).filterIsInstance<Effect.StartCombat>().forEach { f ->
                    listOfNotNull(f.spec.win, f.spec.lose, f.spec.flee).filter { it != WorldState.HUB }.forEach { queue.addLast(it) }
                }
            }
        }
        (content.scenes.keys - reachable).sorted().forEach { problems += "sahne $it: başlangıçtan ulaşılamıyor" }

        return problems
    }
}

/** Bayrakların okunduğu ve yazıldığı yerler. */
data class FlagUsage(val read: Set<String>, val written: Set<String>)

/** Motorun kendisinin koyduğu bayraklar (kayıtta tanım gerekmez). */
fun isEngineFlag(flag: String): Boolean =
    ENGINE_FLAG_PREFIXES.any { flag.startsWith(it) } || flag in ENGINE_FLAGS || flag in ENGINE_FLAGS_EXTRA

private val ENGINE_FLAG_PREFIXES = listOf("yenildi_", "uyanis_", "olay_", "dikkat_esik_", "_red_", "_zar_", "_sira_")
private val ENGINE_FLAGS_EXTRA = setOf(com.yukselis.engine.npc.NpcEngine.FLAG_MOURNED)
private val ENGINE_FLAGS = setOf(com.yukselis.engine.world.WorldEngine.DAILY_WIDE_FLAG)

fun flagUsage(content: Content): FlagUsage {
    val read = mutableSetOf<String>()
    val written = mutableSetOf<String>()
    fun cond(c: Condition?) {
        when (c) {
            is Condition.HasFlag -> read += c.flag
            is Condition.LacksFlag -> read += c.flag
            is Condition.All -> c.conditions.forEach { cond(it) }
            else -> Unit
        }
    }
    fun eff(e: Effect) {
        when (e) {
            is Effect.SetFlag -> written += e.flag
            is Effect.ClearFlag -> written += e.flag
            is Effect.PlantSeed -> e.condition?.let { raw ->
                runCatching { cond(ContentParser.condition(YamlTree.parseObject(raw, "tohum"))) }
            }
            else -> Unit
        }
    }
    content.scenes.values.forEach { s ->
        s.paragraphs.forEach { cond(it.condition) }
        s.notices.forEach { cond(it.condition) }
        s.insights.forEach { cond(it.condition) }
        s.choices.forEach { c ->
            cond(c.requires); cond(c.showWhen)
            (c.effects + (c.check?.failEffects ?: emptyList())).forEach { eff(it) }
        }
    }
    content.classes.values.forEach { cond(it.unlock) }
    content.fusions.forEach { cond(it.condition) }
    content.events.values.forEach { cond(it.condition) }
    content.dailyTemplates.values.forEach { cond(it.condition) }
    content.awakenings.forEach { a -> cond(a.condition); a.effects.forEach { eff(it) } }
    content.npcs.values.forEach { d ->
        cond(d.presentWhen)
        d.goals.forEach { g -> cond(g.condition); g.effects.forEach { eff(it) } }
        d.personalReward.forEach { eff(it) }
    }
    content.ranking.reactions.forEach { r -> r.effects.forEach { eff(it) } }
    return FlagUsage(read, written)
}

/** Hata olmayan ama bakılması gereken içerik sorunları. */
fun contentWarnings(content: Content): List<String> {
    val warnings = mutableListOf<String>()
    val usage = flagUsage(content)
    content.flags.keys.sorted().forEach { f ->
        if (f !in usage.written && !isEngineFlag(f)) warnings += "bayrak '$f': hiçbir yerde set edilmiyor"
        if (f !in usage.read && f !in usage.written) warnings += "bayrak '$f': hiç kullanılmıyor"
    }
    // Ulaşılamayan olay: yalnızca tohumla gelen ama hiçbir yerde tohumlanmayan olaylar.
    val seeded = content.scenes.values.flatMap { s -> s.choices.flatMap { c -> c.effects + (c.check?.failEffects ?: emptyList()) } }
        .plus(content.awakenings.flatMap { it.effects })
        .filterIsInstance<Effect.PlantSeed>().map { it.eventId }.toSet()
    content.events.values.filter { it.seedOnly && it.id !in seeded && !it.id.startsWith("dikkat_") && !it.id.startsWith("esik_") }.forEach {
        warnings += "olay ${it.id}: yalnızca tohumla gelir ama hiçbir yerde tohumlanmıyor (ulaşılamaz)"
    }
    content.events.values.filter { it.repeatable.not() && it.dayMax < it.dayMin }.forEach { warnings += "olay ${it.id}: gün aralığı boş" }
    // NPC yay düğümleri: henüz yazılmamış düğümler (Aşama 6–7 içeriği) uyarıdır.
    content.npcs.values.forEach { d ->
        d.arcNodes.forEach { n ->
            val ev = content.events[n]
            if (ev == null) warnings += "NPC ${d.id}: yay düğümü '$n' henüz yazılmadı"
            else if (d.id !in ev.npcs) warnings += "NPC ${d.id}: yay düğümü '$n' olayının 'npc' listesinde yok (ölünce kapanmaz)"
        }
    }
    // OP MC: önemli olaylarda her seçenek en az bir ahlak eksenini oynatmalı.
    val important = content.events.values.filter { it.important }
    for (ev in important) {
        val scenes = eventScenes(content, ev.entry)
        for (sid in scenes) {
            val scene = content.scenes[sid] ?: continue
            scene.choices.filter { c -> (c.effects + (c.check?.failEffects ?: emptyList())).none { it is Effect.ShiftMorality } }.forEach { c ->
                warnings += "olay ${ev.id} / ${scene.id}.${c.id}: önemli seçim ahlak eksenini oynatmıyor"
            }
        }
    }
    return warnings
}

/** Bir olayın giriş sahnesinden ulaşılan sahneleri (olay içi). */
private fun eventScenes(content: Content, entry: String): Set<String> {
    val seen = mutableSetOf<String>()
    val q = ArrayDeque(listOf(entry))
    while (q.isNotEmpty()) {
        val id = q.removeFirst()
        if (!seen.add(id)) continue
        content.scenes[id]?.choices?.forEach { c ->
            c.next?.takeIf { it in content.scenes }?.let { q.addLast(it) }
            c.check?.failNext?.takeIf { it in content.scenes }?.let { q.addLast(it) }
            c.effects.filterIsInstance<Effect.StartCombat>().forEach { f ->
                listOfNotNull(f.spec.win, f.spec.lose, f.spec.flee).filter { it in content.scenes }.forEach { q.addLast(it) }
            }
        }
    }
    return seen
}

/** Gradle: ./gradlew :engine:validateContent */
fun main(args: Array<String>) {
    val dir = File(args.firstOrNull() ?: "content")
    val problems = try {
        ContentValidator.validate(ContentLoader.load(DirectoryContentSource(dir)))
    } catch (e: ContentFormatException) {
        listOf("Biçim hatası: ${e.message}")
    }
    val warnings = if (problems.isEmpty()) contentWarnings(ContentLoader.load(DirectoryContentSource(dir))) else emptyList()
    warnings.forEach { println("⚠ $it") }
    if (problems.isEmpty()) {
        println("İçerik geçerli: ${dir.absolutePath}" + if (warnings.isNotEmpty()) " (${warnings.size} uyarı)" else "")
    } else {
        problems.forEach { System.err.println("✗ $it") }
        System.err.println("${problems.size} sorun bulundu.")
        exitProcess(1)
    }
}
