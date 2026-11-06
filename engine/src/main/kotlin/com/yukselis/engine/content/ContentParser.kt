package com.yukselis.engine.content

import com.yukselis.engine.combat.AwakeningDef
import com.yukselis.engine.combat.Behavior
import com.yukselis.engine.combat.BossPhase
import com.yukselis.engine.combat.CombatSpec
import com.yukselis.engine.combat.EnemyAttack
import com.yukselis.engine.combat.EnemyDef
import com.yukselis.engine.core.MoralAxis
import com.yukselis.engine.world.ChoiceKind
import com.yukselis.engine.world.DiceCheck
import com.yukselis.engine.world.EventDef
import com.yukselis.engine.world.FactionDef
import com.yukselis.engine.world.FeatDef
import com.yukselis.engine.world.QuestDef
import com.yukselis.engine.world.QuestStatus
import com.yukselis.engine.world.RegionDef
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.NameEntry
import com.yukselis.engine.i18n.NameRegistry
import com.yukselis.engine.story.ChoiceDef
import com.yukselis.engine.story.Condition
import com.yukselis.engine.story.Content
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.Insight
import com.yukselis.engine.story.Notice
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.story.Paragraph
import com.yukselis.engine.story.Scene
import com.yukselis.engine.story.SceneNotice
import com.yukselis.engine.story.SystemTexts
import com.yukselis.engine.story.TitleDef
import com.yukselis.engine.system.Affinity
import com.yukselis.engine.system.ClassDef
import com.yukselis.engine.system.ClassKind
import com.yukselis.engine.system.DailyTemplate
import com.yukselis.engine.system.EssenceDef
import com.yukselis.engine.system.FusionRecipe
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.ItemDef
import com.yukselis.engine.system.ItemType
import com.yukselis.engine.system.ItemUse
import com.yukselis.engine.system.ProfessionDef
import com.yukselis.engine.system.Rarity
import com.yukselis.engine.system.RecipeDef
import com.yukselis.engine.system.SkillDef
import com.yukselis.engine.system.SkillTarget
import com.yukselis.engine.system.SkillType
import com.yukselis.engine.system.Stat
import com.yukselis.engine.system.Stats
import com.yukselis.engine.system.StatusApplication
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class ContentFormatException(message: String) : RuntimeException(message)

/**
 * content/ altındaki YAML dosyalarını okur. Anahtarlar Türkçe ASCII'dir (GDD: Teknik mimari).
 * Hatalı içerik açık bir mesajla reddedilir; sessizce yutulmaz.
 */
object ContentParser {
    const val SYSTEM = "system.yaml"
    const val CLASSES = "classes.yaml"
    const val SKILLS = "skills.yaml"
    const val ITEMS = "items.yaml"
    const val PROFESSIONS = "professions.yaml"
    const val ESSENCES = "essences.yaml"
    const val DAILY = "daily.yaml"
    const val ENEMIES = "enemies.yaml"
    const val AWAKENINGS = "awakenings.yaml"
    const val REGIONS = "regions.yaml"
    const val FACTIONS = "factions.yaml"
    const val FEATS = "feats.yaml"
    const val QUESTS = "quests.yaml"
    const val FLAGS = "flags.yaml"

    data class StoryFile(val start: String?, val scenes: List<Scene>)

    fun load(
        storyFiles: List<String>,
        namesJson: String,
        titlesJson: String,
        files: Map<String, String> = emptyMap(),
        eventFiles: List<String> = emptyList(),
    ): Content {
        val stories = storyFiles.map { parseStory(it) }
        val eventPacks = eventFiles.map { parseEvents(it) }
        val scenes = stories.flatMap { it.scenes } + eventPacks.flatMap { it.second }
        val duplicates = scenes.groupBy { it.id }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) throw ContentFormatException("Aynı kimliğe sahip sahneler: $duplicates")
        val start = stories.mapNotNull { it.start }.distinct().singleOrNull()
            ?: throw ContentFormatException("Tam olarak bir 'baslangic' sahnesi tanımlanmalı")
        fun obj(name: String): JsonObject? = files[name]?.let { parse(it, name) }

        val skillsRoot = obj(SKILLS)
        val profRoot = obj(PROFESSIONS)
        return Content(
            scenes = scenes.associateBy { it.id },
            names = NameRegistry(parseNames(namesJson)),
            titles = parseTitles(titlesJson).associateBy { it.id },
            startScene = start,
            classes = obj(CLASSES)?.objectOf("siniflar")?.map { (id, v) -> parseClass(id, v.jsonObject) }?.associateBy { it.id } ?: emptyMap(),
            skills = skillsRoot?.objectOf("yetenekler")?.map { (id, v) -> parseSkill(id, v.jsonObject) }?.associateBy { it.id } ?: emptyMap(),
            fusions = skillsRoot?.array("kaynastirma")?.map { parseFusion(it.jsonObject) } ?: emptyList(),
            items = obj(ITEMS)?.objectOf("esyalar")?.map { (id, v) -> parseItem(id, v.jsonObject) }?.associateBy { it.id } ?: emptyMap(),
            professions = profRoot?.objectOf("meslekler")?.map { (id, v) ->
                val o = v.jsonObject
                ProfessionDef(id, ltext(o["ad"], "$id.ad"), ltext(o["aciklama"], "$id.aciklama"))
            }?.associateBy { it.id } ?: emptyMap(),
            recipes = profRoot?.objectOf("tarifler")?.map { (id, v) -> parseRecipe(id, v.jsonObject) }?.associateBy { it.id } ?: emptyMap(),
            essences = obj(ESSENCES)?.objectOf("ozler")?.map { (id, v) ->
                val o = v.jsonObject
                EssenceDef(id, ltext(o["ad"], "$id.ad"), ltext(o["aciklama"], "$id.aciklama"))
            }?.associateBy { it.id } ?: emptyMap(),
            dailyTemplates = obj(DAILY)?.objectOf("gunluk")?.map { (id, v) -> parseDaily(id, v.jsonObject) }?.associateBy { it.id } ?: emptyMap(),
            enemies = obj(ENEMIES)?.objectOf("dusmanlar")?.map { (id, v) -> parseEnemy(id, v.jsonObject) }?.associateBy { it.id } ?: emptyMap(),
            awakenings = obj(AWAKENINGS)?.objectOf("uyanislar")?.map { (id, v) -> parseAwakening(id, v.jsonObject) } ?: emptyList(),
            events = eventPacks.flatMap { it.first }.also { list ->
                val dup = list.groupBy { it.id }.filterValues { it.size > 1 }.keys
                if (dup.isNotEmpty()) throw ContentFormatException("Aynı kimliğe sahip olaylar: $dup")
            }.associateBy { it.id },
            regions = obj(REGIONS)?.objectOf("bolgeler")?.map { (id, v) -> parseRegion(id, v.jsonObject) }?.associateBy { it.id } ?: emptyMap(),
            startRegion = obj(REGIONS)?.string("baslangic"),
            factions = obj(FACTIONS)?.objectOf("fraksiyonlar")?.map { (id, v) ->
                FactionDef(id, v.jsonObject["iliski"]?.jsonObject?.map { (k, x) -> k to x.int("$id.iliski.$k") }?.toMap() ?: emptyMap())
            }?.associateBy { it.id } ?: emptyMap(),
            feats = obj(FEATS)?.objectOf("basarilar")?.map { (id, v) -> parseFeat(id, v.jsonObject) }?.associateBy { it.id } ?: emptyMap(),
            quests = obj(QUESTS)?.objectOf("gorevler")?.map { (id, v) ->
                val o = v.jsonObject
                QuestDef(id, ltext(o["ad"], "$id.ad"), ltext(o["aciklama"], "$id.aciklama"))
            }?.associateBy { it.id } ?: emptyMap(),
            flags = obj(FLAGS)?.objectOf("bayraklar")?.map { (id, v) -> id to (v.jsonPrimitive.contentOrNull ?: "") }?.toMap() ?: emptyMap(),
            texts = SystemTexts(obj(SYSTEM)?.map { (k, v) -> k to ltext(v, "system.$k") }?.toMap() ?: emptyMap()),
        )
    }

    private fun parse(text: String, name: String): JsonObject = YamlTree.parseObject(text, name)

    /** Olay dosyası: `olaylar` listesi; her olay kendi `sahneler`ini taşır. */
    fun parseEvents(text: String): Pair<List<EventDef>, List<Scene>> {
        val root = YamlTree.parseObject(text, "olaylar")
        val events = mutableListOf<EventDef>()
        val scenes = mutableListOf<Scene>()
        root.array("olaylar").forEach { e ->
            val o = e.jsonObject
            val id = o.string("id") ?: throw ContentFormatException("olayda 'id' eksik")
            val own = o.array("sahneler").map { parseScene(it.jsonObject) }
            scenes += own
            val gun = o["gun"] as? JsonObject
            events += EventDef(
                id = id,
                acts = o.ints("perde"),
                regions = o.strings("bolge"),
                dayMin = gun?.int("min") ?: 0,
                dayMax = gun?.int("max") ?: Int.MAX_VALUE,
                condition = o["kosul"]?.let { condition(it.jsonObject) },
                weight = o.double("agirlik") ?: 1.0,
                tags = o.strings("etiketler"),
                tension = o.int("gerilim") ?: 2,
                repeatable = o.bool("tekrar"),
                npcs = o.strings("npc"),
                mainArcDay = (o["ana_yay"] as? JsonObject)?.int("gun"),
                card = ltext(o["kart"], "$id.kart"),
                important = o.bool("onemli"),
                seedOnly = o.bool("yalniz_tohum"),
                entry = o.string("sahne") ?: own.firstOrNull()?.id ?: throw ContentFormatException("olay $id: 'sahne' eksik"),
            )
        }
        return events to scenes
    }

    private fun parseRegion(id: String, o: JsonObject) = RegionDef(
        id = id,
        act = o.int("perde") ?: 1,
        baseDanger = o.int("tehlike") ?: 0,
        control = o.string("kontrol") ?: "canavar",
        enemies = o.strings("dusmanlar"),
        neighbors = o.strings("komsu"),
        gateChance = o.double("kapi_sansi") ?: 0.0,
        gateEnemies = o.strings("kapi_dusmanlari"),
        forage = o.strings("toplama"),
    )

    private fun parseFeat(id: String, o: JsonObject) = FeatDef(
        id = id,
        name = ltext(o["ad"], "$id.ad"),
        reactions = o["tepkiler"]?.jsonObject?.map { (k, v) -> k to v.int("$id.tepkiler.$k") }?.toMap() ?: emptyMap(),
        lines = o["satirlar"]?.jsonObject?.map { (k, v) -> k to ltext(v, "$id.satirlar.$k") }?.toMap() ?: emptyMap(),
        titleAt = (o["unvan"] as? JsonObject)?.int("esik"),
        title = (o["unvan"] as? JsonObject)?.string("id"),
    )

    fun parseStory(text: String): StoryFile {
        val root = YamlTree.parseObject(text, "hikaye")
        val scenes = root.array("sahneler").map { parseScene(it.jsonObject) }
        return StoryFile(root.string("baslangic"), scenes)
    }

    fun parseNames(text: String): List<NameEntry> {
        val root = YamlTree.parseObject(text, "names.yaml")
        return NameEntry.Kind.entries.flatMap { kind ->
            val group = root[kind.tokenKey]?.jsonObject ?: JsonObject(emptyMap())
            group.map { (id, value) ->
                val o = value.jsonObject
                val full = ltext(o["ad"], "$id.ad")
                NameEntry(id, kind, full, o["kisa_ad"]?.let { ltext(it, "$id.kisa_ad") } ?: full)
            }
        }
    }

    fun parseTitles(text: String): List<TitleDef> {
        val root = YamlTree.parseObject(text, "titles.yaml")
        return root.map { (id, value) ->
            val o = value.jsonObject
            TitleDef(
                id = id,
                name = ltext(o["ad"], "$id.ad"),
                description = ltext(o["aciklama"], "$id.aciklama"),
                rarity = Rarity.fromKey(o.string("nadirlik")) ?: Rarity.COMMON,
                statBonus = statMap(o["bonus"], id),
                first = o.bool("ilk"),
                reputationCap = o["itibar_tavani"]?.jsonObject?.map { (k, v) -> k to v.int("$id.itibar_tavani.$k") }?.toMap() ?: emptyMap(),
            )
        }
    }

    private fun parseClass(id: String, o: JsonObject): ClassDef {
        val kind = ClassKind.fromKey(o.string("tur")) ?: throw ContentFormatException("sınıf $id: 'tur' temel/gizli/nadir/evrim olmalı")
        return ClassDef(
            id = id,
            name = ltext(o["ad"], "$id.ad"),
            description = ltext(o["aciklama"], "$id.aciklama"),
            kind = kind,
            affinity = o.string("egilim")?.let { affinity(it, id) },
            affinities = o.strings("egilimler").map { affinity(it, "$id.egilimler") },
            unlock = o["kosul"]?.let { condition(it.jsonObject) },
            evolvesFrom = o.string("onceki"),
            grade = o.string("kademe")?.let { Grade.fromKey(it) ?: throw ContentFormatException("sınıf $id: bilinmeyen kademe $it") },
            growth = statMap(o["artis"], "$id.artis"),
            skill = o.string("yetenek"),
            bonus = statMap(o["bonus"], "$id.bonus"),
        )
    }

    private fun parseSkill(id: String, o: JsonObject): SkillDef {
        val cost = o["maliyet"]?.jsonObject
        val status = o["durum"]?.jsonObject?.let { d ->
            StatusApplication(
                effect = d.string("etki") ?: throw ContentFormatException("yetenek $id: durum.etki eksik"),
                chance = d.double("sans") ?: 1.0,
                duration = d.int("sure") ?: 1,
            )
        }
        return SkillDef(
            id = id,
            name = ltext(o["ad"], "$id.ad"),
            description = ltext(o["aciklama"], "$id.aciklama"),
            type = SkillType.fromKey(o.string("tur")),
            rarity = Rarity.fromKey(o.string("nadirlik")) ?: Rarity.COMMON,
            costMp = cost?.int("mp") ?: 0,
            costStamina = cost?.int("dayanim") ?: 0,
            cooldown = o.int("bekleme") ?: 0,
            stat = o.string("stat")?.let { stat(it) },
            base = o.int("taban") ?: 0,
            multiplier = o.double("kat") ?: 0.0,
            target = SkillTarget.fromKey(o.string("hedef")),
            heals = o.bool("iyilestirme"),
            status = status,
            evolutions = o.strings("evrim"),
            penalty = statMap(o["ceza"], "$id.ceza"),
            tags = o.strings("etiketler"),
            affinity = o.string("egilim")?.let { affinity(it, "$id.egilim") },
        )
    }

    private fun parseFusion(o: JsonObject): FusionRecipe {
        val id = o.string("id") ?: throw ContentFormatException("kaynaştırma: 'id' eksik")
        return FusionRecipe(
            id = id,
            inputs = o.strings("girdiler"),
            condition = o["kosul"]?.let { condition(it.jsonObject) },
            result = o.string("sonuc") ?: throw ContentFormatException("kaynaştırma $id: 'sonuc' eksik"),
        )
    }

    private fun parseItem(id: String, o: JsonObject): ItemDef {
        val use = o["kullan"]?.jsonObject?.let { u -> ItemUse(
                hp = u.int("hp") ?: 0,
                mp = u.int("mp") ?: 0,
                cures = u.strings("temizle"),
                damage = u.int("hasar") ?: 0,
                inflicts = u.string("durum"),
            )
        }
        return ItemDef(
            id = id,
            name = ltext(o["ad"], "$id.ad"),
            description = ltext(o["aciklama"], "$id.aciklama"),
            type = ItemType.fromKey(o.string("tur")),
            rarity = Rarity.fromKey(o.string("nadirlik")) ?: Rarity.COMMON,
            value = o.int("deger") ?: 0,
            use = use,
        )
    }

    private fun parseRecipe(id: String, o: JsonObject) = RecipeDef(
        id = id,
        profession = o.string("meslek") ?: throw ContentFormatException("tarif $id: 'meslek' eksik"),
        inputs = o["girdiler"]?.jsonObject?.map { (k, v) -> k to v.int("$id.girdiler.$k") }?.toMap() ?: emptyMap(),
        output = o.string("sonuc") ?: throw ContentFormatException("tarif $id: 'sonuc' eksik"),
        count = o.int("adet") ?: 1,
        minLevel = o.int("seviye") ?: 1,
        xp = o.int("xp") ?: 10,
    )

    private fun parseDaily(id: String, o: JsonObject) = DailyTemplate(
        id = id,
        title = ltext(o["baslik"], "$id.baslik"),
        description = ltext(o["aciklama"], "$id.aciklama"),
        kind = o.string("tur") ?: throw ContentFormatException("günlük $id: 'tur' eksik"),
        steps = o.int("adim") ?: 1,
        xp = o.int("xp") ?: 0,
        condition = o["kosul"]?.let { condition(it.jsonObject) },
    )

    private fun parseScene(o: JsonObject): Scene {
        val id = o.string("id") ?: throw ContentFormatException("Sahnede 'id' eksik")
        return Scene(
            id = id,
            place = o.string("yer"),
            paragraphs = o.array("paragraflar").map { parseParagraph(it, id) },
            notices = o.array("bildirimler").map { parseNotice(it.jsonObject, id) },
            choices = o.array("secenekler").map { parseChoice(it.jsonObject, id) },
            isEnding = o.bool("son"),
            insights = o.array("sezgiler").map { e ->
                val i = e.jsonObject
                Insight(
                    text = ltext(i["metin"], "$id.sezgi"),
                    condition = i["kosul"]?.let { condition(it.jsonObject) } ?: throw ContentFormatException("$id: sezgide 'kosul' eksik"),
                )
            },
        )
    }

    private fun parseParagraph(e: JsonElement, sceneId: String): Paragraph {
        val o = e.jsonObject
        // Kısa biçim: {"tr": ..., "en": ...}; uzun biçim: {"metin": {...}, "kosul": {...}}
        return if ("metin" in o) {
            Paragraph(ltext(o["metin"], "$sceneId.paragraf"), o["kosul"]?.let { condition(it.jsonObject) })
        } else {
            Paragraph(ltext(o, "$sceneId.paragraf"))
        }
    }

    private fun parseNotice(o: JsonObject, sceneId: String): SceneNotice = SceneNotice(
        notice = Notice(
            kind = NoticeKind.fromKey(o.string("tur")),
            title = ltext(o["baslik"], "$sceneId.bildirim.baslik"),
            lines = o.array("satirlar").map { ltext(it, "$sceneId.bildirim.satir") },
            rarity = Rarity.fromKey(o.string("nadirlik")),
        ),
        condition = o["kosul"]?.let { condition(it.jsonObject) },
    )

    private fun parseChoice(o: JsonObject, sceneId: String): ChoiceDef {
        val id = o.string("id") ?: throw ContentFormatException("$sceneId: seçenekte 'id' eksik")
        return ChoiceDef(
            id = id,
            label = ltext(o["etiket"], "$sceneId.$id.etiket"),
            requires = o["gerekli"]?.let { condition(it.jsonObject) },
            showWhen = o["goster"]?.let { condition(it.jsonObject) },
            effects = o.array("etkiler").flatMap { effects(it.jsonObject, "$sceneId.$id") },
            next = o.string("sonraki"),
            kind = ChoiceKind.fromKey(o.string("tur")),
            check = (o["zar"] as? JsonObject)?.let { z ->
                DiceCheck(
                    stat = stat(z.string("stat") ?: throw ContentFormatException("$sceneId.$id: zar.stat eksik")),
                    difficulty = z.int("zorluk") ?: throw ContentFormatException("$sceneId.$id: zar.zorluk eksik"),
                    oneShot = z.bool("tek"),
                    failNext = z.string("basarisiz"),
                    failEffects = z.array("basarisiz_etkiler").flatMap { effects(it.jsonObject, "$sceneId.$id.basarisiz") },
                )
            },
        )
    }

    fun condition(o: JsonObject): Condition {
        val parts = o.flatMap { (key, value) ->
            when (key) {
                "stat" -> value.jsonObject.map { (k, v) -> Condition.StatAtLeast(stat(k), v.int(k)) }
                "bayrak" -> listOf(Condition.HasFlag(value.str(key)))
                "bayrak_yok" -> listOf(Condition.LacksFlag(value.str(key)))
                "degisken_min" -> value.jsonObject.map { (k, v) -> Condition.VarAtLeast(k, v.int(k)) }
                "degisken_max" -> value.jsonObject.map { (k, v) -> Condition.VarAtMost(k, v.int(k)) }
                "ahlak_min" -> value.jsonObject.map { (k, v) -> Condition.MoralityAtLeast(axis(k), v.int(k)) }
                "ahlak_max" -> value.jsonObject.map { (k, v) -> Condition.MoralityAtMost(axis(k), v.int(k)) }
                "seviye_min" -> listOf(Condition.LevelAtLeast(value.int(key)))
                "egilim_min" -> value.jsonObject.map { (k, v) -> Condition.AffinityAtLeast(affinity(k, key), v.int(k)) }
                "yetenek" -> listOf(Condition.HasSkill(value.str(key)))
                "unvan" -> listOf(Condition.HasTitle(value.str(key)))
                "sinif" -> listOf(Condition.HasClass(value.str(key)))
                "esya" -> value.jsonObject.map { (k, v) -> Condition.HasItem(k, v.int(k)) }
                "itibar_min" -> value.jsonObject.map { (k, v) -> Condition.ReputationAtLeast(k, v.int(k)) }
                "itibar_max" -> value.jsonObject.map { (k, v) -> Condition.ReputationAtMost(k, v.int(k)) }
                "dikkat_min" -> listOf(Condition.AttentionAtLeast(value.int(key)))
                "dikkat_max" -> listOf(Condition.AttentionAtMost(value.int(key)))
                "yozlasma_min" -> listOf(Condition.CorruptionAtLeast(value.int(key)))
                "gizli_fark_min" -> listOf(Condition.HiddenGapAtLeast(value.int(key)))
                "bolge" -> listOf(Condition.InRegion(value.str(key)))
                "perde" -> listOf(Condition.InAct(value.int(key)))
                "gun_min" -> listOf(Condition.DayAtLeast(value.int(key)))
                "gun_max" -> listOf(Condition.DayAtMost(value.int(key)))
                "gorev" -> value.jsonObject.map { (k, v) ->
                    Condition.QuestIs(k, QuestStatus.fromKey(v.str(k)) ?: throw ContentFormatException("görev $k: durum basla/tamam/basarisiz olmalı"))
                }
                else -> throw ContentFormatException("Bilinmeyen koşul: $key")
            }
        }
        return parts.singleOrNull() ?: Condition.All(parts)
    }

    private fun effects(o: JsonObject, where: String): List<Effect> = o.flatMap { (key, value) ->
        when (key) {
            "bayrak" -> listOf(Effect.SetFlag(value.str(key)))
            "bayrak_kaldir" -> listOf(Effect.ClearFlag(value.str(key)))
            "degisken" -> value.jsonObject.map { (k, v) -> Effect.AddVar(k, v.int(k)) }
            "ahlak" -> value.jsonObject.map { (k, v) -> Effect.ShiftMorality(axis(k), v.int(k)) }
            "itibar" -> value.jsonObject.map { (k, v) -> Effect.ShiftReputation(k, v.int(k)) }
            "xp" -> listOf(Effect.GainXp(value.jsonPrimitive.longOrNull ?: throw ContentFormatException("$where: xp sayı olmalı")))
            "gun" -> listOf(Effect.AdvanceDays(value.int(key)))
            "stat" -> value.jsonObject.map { (k, v) -> Effect.AddStat(stat(k), v.int(k)) }
            "unvan" -> listOf(Effect.GrantTitle(value.str(key)))
            "egilim" -> value.jsonObject.map { (k, v) -> Effect.ShiftAffinity(affinity(k, where), v.int(k)) }
            "yetenek" -> listOf(Effect.GrantSkill(value.str(key)))
            "yetenek_xp" -> value.jsonObject.map { (k, v) -> Effect.SkillXp(k, v.int(k)) }
            "esya" -> value.jsonObject.map { (k, v) -> Effect.AddItem(k, v.int(k)) }
            "kredi" -> listOf(Effect.AddCredits(value.int(key)))
            "oz_kristali" -> listOf(Effect.AddCrystals(value.int(key)))
            "oz" -> value.jsonObject.map { (k, v) -> Effect.EssenceInsight(k, v.int(k)) }
            "gunluk" -> value.jsonObject.map { (k, v) -> Effect.DailyProgress(k, v.int(k)) }
            "meslek_teklifi" -> listOf(Effect.OfferProfession)
            "can" -> listOf(Effect.AddHp(value.int(key)))
            "arayuz" -> listOf(Effect.UnlockTab(value.str(key)))
            "savas" -> listOf(Effect.StartCombat(combatSpec(value.jsonObject, where)))
            "tohum" -> listOf(
                (value.jsonObject).let { t ->
                    val delay = t["gecikme"] as? JsonObject
                    Effect.PlantSeed(
                        eventId = t.string("olay") ?: throw ContentFormatException("$where: tohum.olay eksik"),
                        minDelay = delay?.int("min") ?: t.int("gecikme") ?: 1,
                        maxDelay = delay?.int("max") ?: delay?.int("min") ?: t.int("gecikme") ?: 1,
                        // Koşul ham metin olarak saklanır (kayda girer); vadesinde ayrıştırılır.
                        condition = t["kosul"]?.let { c -> condition(c.jsonObject); c.toString() },
                    )
                },
            )
            "dikkat" -> listOf(Effect.ShiftAttention(value.int(key)))
            "gizle" -> listOf(Effect.ShiftAttention(-value.int(key)))
            "yozlasma" -> listOf(Effect.ShiftCorruption(value.int(key)))
            "aciga_vur" -> listOf(Effect.Reveal)
            "efsane" -> listOf(Effect.Feat(value.str(key)))
            "gorev" -> value.jsonObject.map { (k, v) ->
                Effect.SetQuest(k, QuestStatus.fromKey(v.str(k)) ?: throw ContentFormatException("$where: görev $k durumu basla/tamam/basarisiz"))
            }
            "bolge" -> listOf(Effect.MoveTo(value.str(key)))
            "tehlike" -> value.jsonObject.map { (k, v) -> Effect.ShiftDanger(k, v.int(k)) }
            "mana" -> listOf(Effect.AddMp(value.int(key)))
            else -> throw ContentFormatException("$where: bilinmeyen etki '$key'")
        }
    }

    private fun combatSpec(o: JsonObject, where: String) = CombatSpec(
        enemies = o.strings("dusmanlar").ifEmpty { throw ContentFormatException("$where: savaşta 'dusmanlar' boş") },
        environment = o.strings("ortam"),
        win = o.string("kazan") ?: throw ContentFormatException("$where: savaşta 'kazan' sahnesi eksik"),
        lose = o.string("kaybet"),
        flee = o.string("kac"),
    )

    private fun parseAttack(o: JsonObject, where: String): EnemyAttack {
        val id = o.string("id") ?: throw ContentFormatException("$where: saldırıda 'id' eksik")
        return EnemyAttack(
            id = id,
            name = ltext(o["ad"], "$where.$id.ad"),
            stat = o.string("stat")?.let { stat(it) } ?: Stat.STR,
            base = o.int("taban") ?: 0,
            multiplier = o.double("kat") ?: 1.0,
            tags = o.strings("etiketler"),
            status = o["durum"]?.jsonObject?.let { d ->
                StatusApplication(d.string("etki") ?: throw ContentFormatException("$where.$id: durum.etki eksik"), d.double("sans") ?: 1.0, d.int("sure") ?: 1)
            },
            cooldown = o.int("bekleme") ?: 0,
            prose = ltext(o["metin"], "$where.$id.metin"),
        )
    }

    private fun parseEnemy(id: String, o: JsonObject): EnemyDef {
        val level = o.int("seviye") ?: throw ContentFormatException("düşman $id: 'seviye' eksik")
        val statsObj = o["statlar"]?.jsonObject ?: throw ContentFormatException("düşman $id: 'statlar' eksik")
        val stats = Stats(Stat.entries.associateWith { s -> statsObj.int(s.key) ?: throw ContentFormatException("düşman $id: statlar.${s.key} eksik") })
        return EnemyDef(
            id = id,
            name = ltext(o["ad"], "$id.ad"),
            description = ltext(o["aciklama"], "$id.aciklama"),
            hook = ltext(o["kanca"], "$id.kanca"),
            level = level,
            grade = o.string("kademe")?.let { Grade.fromKey(it) ?: throw ContentFormatException("düşman $id: bilinmeyen kademe") } ?: Grade.forLevel(level),
            maxHp = o.int("can") ?: throw ContentFormatException("düşman $id: 'can' eksik"),
            stats = stats,
            behavior = o.string("davranis")?.let { k ->
                Behavior.entries.firstOrNull { it.key == k } ?: throw ContentFormatException("düşman $id: bilinmeyen davranış '$k'")
            } ?: Behavior.AGGRESSIVE,
            intelligent = o.bool("zeki"),
            weakness = o.string("zaaf"),
            weaknessText = o["zaaf_metin"]?.let { ltext(it, "$id.zaaf_metin") },
            attacks = o.array("saldirilar").map { parseAttack(it.jsonObject, id) },
            xp = (o["xp"] as? JsonPrimitive)?.longOrNull ?: 0,
            credits = o.int("kredi") ?: 0,
            loot = o["ganimet"]?.jsonObject?.map { (k, v) -> k to (v.jsonPrimitive.doubleOrNull ?: 0.0) }?.toMap() ?: emptyMap(),
            tags = o.strings("etiketler"),
            boss = o.bool("boss"),
            phases = o.array("fazlar").map { e ->
                val f = e.jsonObject
                BossPhase(
                    threshold = f.int("esik") ?: throw ContentFormatException("düşman $id: fazda 'esik' eksik"),
                    text = ltext(f["metin"], "$id.faz.metin"),
                    buff = statMap(f["guclenme"], "$id.faz.guclenme"),
                    attack = f["saldiri"]?.jsonObject?.let { parseAttack(it, "$id.faz") },
                )
            },
        )
    }

    private fun parseAwakening(id: String, o: JsonObject) = AwakeningDef(
        id = id,
        title = ltext(o["baslik"], "$id.baslik"),
        lines = o.array("satirlar").map { ltext(it, "$id.satir") },
        condition = o["kosul"]?.let { condition(it.jsonObject) } ?: throw ContentFormatException("uyanış $id: 'kosul' eksik"),
        effects = o.array("etkiler").flatMap { effects(it.jsonObject, "uyanış $id") },
    )

    private fun ltext(e: JsonElement?, where: String): LText {
        val o = (e as? JsonObject) ?: throw ContentFormatException("$where: {tr, en} metni bekleniyordu")
        return LText(o.string("tr") ?: "", o.string("en") ?: "")
    }

    private fun statMap(e: JsonElement?, where: String): Map<Stat, Int> =
        (e as? JsonObject)?.map { (k, v) -> stat(k) to v.int("$where.$k") }?.toMap() ?: emptyMap()

    private fun stat(key: String): Stat = Stat.fromKey(key) ?: throw ContentFormatException("Bilinmeyen stat: $key")

    private fun axis(key: String): MoralAxis = MoralAxis.fromKey(key) ?: throw ContentFormatException("Bilinmeyen ahlak ekseni: $key")

    private fun affinity(key: String, where: String): Affinity =
        Affinity.fromKey(key) ?: throw ContentFormatException("$where: bilinmeyen eğilim '$key'")

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

    private fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

    private fun JsonObject.bool(key: String): Boolean = (this[key] as? JsonPrimitive)?.booleanOrNull ?: false

    private fun JsonObject.ints(key: String): List<Int> = when (val v = this[key]) {
        is JsonArray -> v.mapNotNull { it.jsonPrimitive.intOrNull }
        is JsonPrimitive -> listOfNotNull(v.intOrNull)
        else -> emptyList()
    }

    /** Tek değer ya da liste: `bolge: orman` ya da `bolge: [orman, carsi]`. */
    private fun JsonObject.strings(key: String): List<String> =
        (this[key] as? JsonPrimitive)?.contentOrNull?.let { listOf(it) } ?: stringList(key)

    private fun JsonObject.stringList(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

    private fun JsonObject.objectOf(key: String): JsonObject = this[key]?.jsonObject ?: JsonObject(emptyMap())

    private fun JsonObject.array(key: String): JsonArray = this[key]?.jsonArray ?: JsonArray(emptyList())

    private fun JsonElement.int(where: String): Int =
        jsonPrimitive.intOrNull ?: throw ContentFormatException("$where: tam sayı bekleniyordu")

    private fun JsonElement.str(where: String): String =
        jsonPrimitive.contentOrNull ?: throw ContentFormatException("$where: metin bekleniyordu")
}
