package com.yukselis.engine.content

import com.yukselis.engine.combat.AwakeningDef
import com.yukselis.engine.combat.Behavior
import com.yukselis.engine.combat.BossPhase
import com.yukselis.engine.combat.CombatSpec
import com.yukselis.engine.combat.EnemyAttack
import com.yukselis.engine.combat.EnemyDef
import com.yukselis.engine.core.MoralAxis
import com.yukselis.engine.world.ChapterDef
import com.yukselis.engine.world.ChapterTable
import com.yukselis.engine.world.ChoiceKind
import com.yukselis.engine.world.OperationDef
import com.yukselis.engine.world.DiceCheck
import com.yukselis.engine.world.EventDef
import com.yukselis.engine.world.FactionDef
import com.yukselis.engine.world.FeatDef
import com.yukselis.engine.world.QuestDef
import com.yukselis.engine.world.QuestStatus
import com.yukselis.engine.world.RegionDef
import com.yukselis.engine.world.TowerBand
import com.yukselis.engine.world.TowerDef
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.NameEntry
import com.yukselis.engine.i18n.NameRegistry
import com.yukselis.engine.story.ArchetypeDef
import com.yukselis.engine.story.ChoiceDef
import com.yukselis.engine.story.EndingDef
import com.yukselis.engine.story.EndingTable
import com.yukselis.engine.story.EpilogueLine
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
import com.yukselis.engine.system.PathDef
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
    const val TRAITS = "traits.yaml"
    const val KNOWLEDGE = "knowledge.yaml"
    const val ARMY = "army.yaml"
    const val RANKING = "ranking.yaml"
    const val ENDINGS = "endings.yaml"
    const val TOWER = "tower.yaml"
    const val CHAPTERS = "chapters.yaml"

    data class StoryFile(val start: String?, val scenes: List<Scene>)

    /** Sırayı koruyan paralel dönüşüm; hata, ayrıştırıcının kendi istisnasıyla çıkar. */
    internal fun <T, R> parallelMap(items: List<T>, f: (T) -> R): List<R> {
        val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, MAX_PARSE_THREADS)
        if (items.size < 2 || threads < 2) return items.map(f)
        val pool = java.util.concurrent.Executors.newFixedThreadPool(threads)
        try {
            val futures = items.map { item -> pool.submit(java.util.concurrent.Callable { f(item) }) }
            return futures.map { fut ->
                try {
                    fut.get()
                } catch (e: java.util.concurrent.ExecutionException) {
                    throw e.cause ?: e
                }
            }
        } finally {
            pool.shutdown()
        }
    }

    private const val MAX_PARSE_THREADS = 4

    fun load(
        storyFiles: List<String>,
        namesJson: String,
        titlesJson: String,
        files: Map<String, String> = emptyMap(),
        eventFiles: List<String> = emptyList(),
        npcFiles: List<String> = emptyList(),
    ): Content {
        val bundle = ContentBundle(
            stories = parallelMap(storyFiles) { YamlTree.parseObject(it, "hikaye") },
            names = YamlTree.parseObject(namesJson, "names.yaml"),
            titles = YamlTree.parseObject(titlesJson, "titles.yaml"),
            files = files.mapValues { (name, text) -> parse(text, name) },
            events = parallelMap(eventFiles) { YamlTree.parseObject(it, "olaylar") },
            npcs = parallelMap(npcFiles) { YamlTree.parseObject(it, "npc") },
        )
        return loadTrees(bundle)
    }

    /** Önceden ayrıştırılmış [JsonObject] ağaçlarından (YAML ya da [ContentCache]) [Content] kurar. */
    fun loadTrees(bundle: ContentBundle): Content {
        // Sonlar tablosu olaylardan önce kurulur: `son_uygun` koşulları ona bağlanır, tablo sonra doldurulur.
        val endingTable = EndingTable()
        EndingTable.current = endingTable
        val stories = parallelMap(bundle.stories) { parseStory(it) }
        val eventPacks = parallelMap(bundle.events) { parseEvents(it) }
        val scenes = stories.flatMap { it.scenes } + eventPacks.flatMap { it.second }
        val duplicates = scenes.groupBy { it.id }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) throw ContentFormatException("Aynı kimliğe sahip sahneler: $duplicates")
        val start = stories.mapNotNull { it.start }.distinct().singleOrNull()
            ?: throw ContentFormatException("Tam olarak bir 'baslangic' sahnesi tanımlanmalı")
        fun obj(name: String): JsonObject? = bundle.files[name]

        val skillsRoot = obj(SKILLS)
        val profRoot = obj(PROFESSIONS)
        val names = NameRegistry(parseNames(bundle.names))
        val texts = SystemTexts(obj(SYSTEM)?.map { (k, v) -> k to ltext(v, "system.$k") }?.toMap() ?: emptyMap())
        val npcs = parallelMap(bundle.npcs) { parseNpc(it) }.also { list ->
            val dup = list.groupBy { it.id }.filterValues { it.size > 1 }.keys
            if (dup.isNotEmpty()) throw ContentFormatException("Aynı kimliğe sahip NPC'ler: $dup")
        }.associateBy { it.id }
        // Her NPC'nin kişisel görevi (Bağlılık ≥ 70 eşiği) Günlük'te görünür.
        val personalQuests = npcs.values.associate { d ->
            val short = names.find(NameEntry.Kind.NPC, d.id)?.short ?: LText(d.id, d.id)
            val goal = d.goals.maxByOrNull { it.priority }?.name ?: LText.EMPTY
            val qid = com.yukselis.engine.npc.NpcEngine.PERSONAL_QUEST_PREFIX + d.id
            qid to QuestDef(qid, texts.formatL("kisisel_gorev_ad", "npc" to short), goal)
        }
        val factionsRoot = obj(FACTIONS)?.objectOf("fraksiyonlar")
        obj(ENDINGS)?.let { fillEndings(endingTable, it) }
        return Content(
            endings = endingTable,
            tower = obj(TOWER)?.let { (it["kule"] as? JsonObject)?.let(::parseTower) },
            chapters = obj(CHAPTERS)?.let(::parseChapters) ?: ChapterTable(),
            paths = obj(ESSENCES)?.objectOf("yollar")?.map { (id, v) ->
                val o = v.jsonObject
                PathDef(id, ltext(o["ad"], "$id.ad"), ltext(o["aciklama"], "$id.aciklama"), o.strings("ozler"))
            }?.associateBy { it.id } ?: emptyMap(),
            scenes = scenes.associateBy { it.id },
            names = names,
            titles = parseTitles(bundle.titles).associateBy { it.id },
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
            quests = personalQuests + (obj(QUESTS)?.objectOf("gorevler")?.map { (id, v) ->
                val o = v.jsonObject
                QuestDef(id, ltext(o["ad"], "$id.ad"), ltext(o["aciklama"], "$id.aciklama"))
            }?.associateBy { it.id } ?: emptyMap()),
            factionValues = factionsRoot?.mapNotNull { (id, v) -> (v.jsonObject["degerler"] as? JsonObject)?.let { id to morality(it) } }?.toMap() ?: emptyMap(),
            npcs = npcs,
            traits = obj(TRAITS)?.objectOf("izler")?.map { (id, v) -> parseTrait(id, v.jsonObject) }?.associateBy { it.id } ?: emptyMap(),
            knowledge = obj(KNOWLEDGE)?.objectOf("bilgiler")?.map { (id, v) ->
                val o = v.jsonObject
                com.yukselis.engine.npc.KnowledgeDef(id, ltext(o["ad"], "$id.ad"), ltext(o["aciklama"], "$id.aciklama"), o.string("tur") ?: "soylenti")
            }?.associateBy { it.id } ?: emptyMap(),
            army = obj(ARMY)?.objectOf("birlikler")?.map { (id, v) ->
                val o = v.jsonObject
                com.yukselis.engine.npc.ArmyUnitDef(id, ltext(o["ad"], "$id.ad"), ltext(o["aciklama"], "$id.aciklama"), o.strings("egilim").map { affinity(it, "$id.egilim") }, o.int("guc") ?: 1)
            }?.associateBy { it.id } ?: emptyMap(),
            ranking = obj(RANKING)?.let { parseRanking(it) } ?: com.yukselis.engine.npc.RankingDef(),
            flags = obj(FLAGS)?.objectOf("bayraklar")?.map { (id, v) -> id to (v.jsonPrimitive.contentOrNull ?: "") }?.toMap() ?: emptyMap(),
            texts = texts,
        )
    }

    // ---------------------------------------------------------------- Aşama 5: NPC'ler

    private fun morality(o: JsonObject) = com.yukselis.engine.core.Morality(
        mercy = o.int("merhamet") ?: 0,
        order = o.int("duzen") ?: 0,
        humanity = o.int("insanlik") ?: 0,
    )

    private fun relDeltas(o: JsonObject, where: String): Map<com.yukselis.engine.npc.RelAxis, Int> =
        o.map { (k, v) ->
            (com.yukselis.engine.npc.RelAxis.fromKey(k) ?: throw ContentFormatException("$where: bilinmeyen ilişki ekseni '$k' (guven/saygi/korku/baglilik)")) to v.int("$where.$k")
        }.toMap()

    private fun ltexts(e: JsonElement?, where: String): List<LText> = when (e) {
        null -> emptyList()
        is JsonArray -> e.map { ltext(it, where) }
        else -> listOf(ltext(e, where))
    }

    fun parseNpc(o: JsonObject): com.yukselis.engine.npc.NpcDef {
        val id = o.string("id") ?: throw ContentFormatException("NPC'de 'id' eksik")
        val k = o["kisilik"]?.jsonObject ?: throw ContentFormatException("NPC $id: 'kisilik' eksik")
        val personality = com.yukselis.engine.npc.Personality(
            com.yukselis.engine.npc.Trait.entries.associateWith { t -> k.int(t.key) ?: throw ContentFormatException("NPC $id: kisilik.${t.key} eksik") },
        )
        val combat = (o["savas"] as? JsonObject)?.let { c ->
            val st = c["statlar"]?.jsonObject ?: throw ContentFormatException("NPC $id: savas.statlar eksik")
            com.yukselis.engine.npc.NpcCombat(
                stats = Stats(Stat.entries.associateWith { s -> st.int(s.key) ?: Stats.BASE_VALUE }),
                growth = statMap(c["artis"], "$id.savas.artis"),
                role = com.yukselis.engine.npc.CombatRole.fromKey(c.string("rol")),
                attackStat = c.string("stat")?.let { stat(it) } ?: Stat.STR,
                attackName = ltext(c["saldiri"], "$id.savas.saldiri"),
                prose = ltext(c["metin"], "$id.savas.metin"),
                duelProse = ltext(c["duello_metin"], "$id.savas.duello_metin"),
            )
        }
        return com.yukselis.engine.npc.NpcDef(
            id = id,
            description = ltext(o["aciklama"], "$id.aciklama"),
            personality = personality,
            values = (o["degerler"] as? JsonObject)?.let { morality(it) } ?: com.yukselis.engine.core.Morality(),
            startRelation = (o["baslangic_iliski"] as? JsonObject)?.let { com.yukselis.engine.npc.Relation().plus(relDeltas(it, "$id.baslangic_iliski")) }
                ?: com.yukselis.engine.npc.Relation(),
            goals = o.array("hedefler").map { e ->
                val g = e.jsonObject
                val gid = g.string("id") ?: throw ContentFormatException("NPC $id: hedefte 'id' eksik")
                com.yukselis.engine.npc.GoalDef(
                    id = gid,
                    priority = g.int("oncelik") ?: 1,
                    name = ltext(g["ad"], "$id.$gid.ad"),
                    days = g.int("gun") ?: 0,
                    condition = g["kosul"]?.let { condition(it.jsonObject) },
                    effects = g.array("etkiler").flatMap { effects(it.jsonObject, "NPC $id.$gid") },
                )
            },
            affinity = o["egilim"]?.jsonObject?.map { (a, v) -> affinity(a, "$id.egilim") to v.int("$id.egilim.$a") }?.toMap() ?: emptyMap(),
            startLevel = o.int("seviye") ?: 1,
            growthRate = o.double("hiz") ?: 1.0,
            faction = o.string("fraksiyon"),
            canJoin = o.bool("yoldas"),
            ranked = o.bool("siralama"),
            knownAtStart = o.bool("bilinen"),
            presentWhen = o["yaninda"]?.let { condition(it.jsonObject) },
            combat = combat,
            arcNodes = o.strings("yay_dugumleri"),
            firstClaims = o.array("ilk_unvanlar").map { e ->
                val f = e.jsonObject
                com.yukselis.engine.npc.FirstClaimDef(
                    title = f.string("unvan") ?: throw ContentFormatException("NPC $id: ilk_unvanlar.unvan eksik"),
                    level = f.int("seviye") ?: throw ContentFormatException("NPC $id: ilk_unvanlar.seviye eksik"),
                )
            },
            opinions = o["gorusler"]?.jsonObject?.map { (key, v) -> key to ltexts(v, "$id.gorusler.$key") }?.toMap() ?: emptyMap(),
            powerLines = o["guc_tepkileri"]?.jsonObject?.map { (key, v) ->
                (com.yukselis.engine.npc.PowerReaction.fromKey(key) ?: throw ContentFormatException("NPC $id: bilinmeyen güç tepkisi '$key'")) to
                    ltexts(v, "$id.guc_tepkileri.$key")
            }?.toMap() ?: emptyMap(),
            memories = o["hafizalar"]?.jsonObject?.map { (key, v) -> key to ltext(v, "$id.hafizalar.$key") }?.toMap() ?: emptyMap(),
            personalReward = o.array("kisisel_odul").flatMap { effects(it.jsonObject, "NPC $id.kisisel_odul") },
        )
    }

    private fun parseTrait(id: String, o: JsonObject) = com.yukselis.engine.npc.TraitDef(
        id = id,
        name = ltext(o["ad"], "$id.ad"),
        description = ltext(o["aciklama"], "$id.aciklama"),
        onPlayer = (o.string("hedef") ?: "oyuncu") == "oyuncu",
        statBonus = statMap(o["bonus"], "$id.bonus"),
        statPenalty = statMap(o["ceza"], "$id.ceza"),
        personality = o["kisilik"]?.jsonObject?.map { (k, v) ->
            (com.yukselis.engine.npc.Trait.fromKey(k) ?: throw ContentFormatException("iz $id: bilinmeyen kişilik '$k'")) to v.int("$id.kisilik.$k")
        }?.toMap() ?: emptyMap(),
        corruption = o.int("dikkat") ?: o.int("yozlasma") ?: 0, // Paket 4: iz Dikkat artırır; eski anahtar da okunur
    )

    private fun parseRanking(o: JsonObject): com.yukselis.engine.npc.RankingDef {
        val h = o["avcilar"] as? JsonObject
        val lv = h?.get("seviye") as? JsonObject
        val rate = h?.get("hiz") as? JsonObject
        return com.yukselis.engine.npc.RankingDef(
            firstNames = h?.array("adlar")?.map { ltext(it, "ranking.adlar") } ?: emptyList(),
            lastNames = h?.array("soyadlar")?.map { ltext(it, "ranking.soyadlar") } ?: emptyList(),
            hunters = h?.int("sayi") ?: 0,
            levelMin = lv?.int("min") ?: 1,
            levelMax = lv?.int("max") ?: 3,
            rateMin = rate?.double("min") ?: 0.6,
            rateMax = rate?.double("max") ?: 1.2,
            reactions = o.array("tepkiler").map { e ->
                val t = e.jsonObject
                com.yukselis.engine.npc.RankReaction(
                    rank = t.int("sira") ?: throw ContentFormatException("ranking.tepkiler: 'sira' eksik"),
                    effects = t.array("etkiler").flatMap { effects(it.jsonObject, "ranking.tepkiler") },
                )
            },
        )
    }

    private fun parse(text: String, name: String): JsonObject = YamlTree.parseObject(text, name)

    /** Olay dosyası: `olaylar` listesi; her olay kendi `sahneler`ini taşır. */
    fun parseEvents(text: String): Pair<List<EventDef>, List<Scene>> =
        parseEvents(YamlTree.parseObject(text, "olaylar"))

    internal fun parseEvents(root: JsonObject): Pair<List<EventDef>, List<Scene>> {
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
                camp = o.bool("kamp"),
                entry = o.string("sahne") ?: own.firstOrNull()?.id ?: throw ContentFormatException("olay $id: 'sahne' eksik"),
                location = o.strings("konum"),
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
        lastAct = o.int("son_perde") ?: 4,
        condition = o["kosul"]?.let { condition(it.jsonObject) },
        environment = o.string("ortam"),
    )

    /** `chapters.yaml`: `uslar` (perde → üs bölgesi) ve `bolumler` listesi (DECISIONS #130). */
    private fun parseChapters(o: JsonObject): ChapterTable {
        val bases = (o["uslar"] as? JsonObject)?.map { (k, v) ->
            (k.toIntOrNull() ?: throw ContentFormatException("chapters.uslar: '$k' perde numarası olmalı")) to v.str(k)
        }?.toMap() ?: emptyMap()
        val list = o.array("bolumler").mapIndexed { i, e ->
            val c = e.jsonObject
            val no = c.int("no") ?: throw ContentFormatException("chapters.bolumler[$i]: 'no' eksik")
            val where = "bolum $no"
            fun req(k: String) = c.int(k) ?: throw ContentFormatException("$where: '$k' eksik")
            val ops = c.array("operasyon").map { x ->
                when (x) {
                    is JsonPrimitive -> OperationDef(LText.EMPTY, null, listOf(x.content))
                    else -> x.jsonObject.let { op ->
                        OperationDef(op["ad"]?.let { ltext(it, "$where.operasyon.ad") } ?: LText.EMPTY, op.string("bolge"), op.strings("olaylar"))
                    }
                }
            }.let { raw ->
                // Düz liste ("operasyon: [a, b]") tek operasyondur; nesneler ayrı seçeneklerdir.
                if (raw.all { it.title == LText.EMPTY && it.region == null && it.events.size == 1 } && raw.size > 1) {
                    listOf(OperationDef(LText.EMPTY, c.string("operasyon_bolge"), raw.flatMap { it.events }))
                } else raw.map { if (it.region == null && c.string("operasyon_bolge") != null) it.copy(region = c.string("operasyon_bolge")) else it }
            }
            ChapterDef(
                no = no,
                act = req("perde"),
                open = req("acilis"),
                deadline = req("son_tarih"),
                end = req("bitis"),
                title = ltext(c["baslik"], "$where.baslik"),
                goal = ltext(c["hedef"], "$where.hedef"),
                threat = c["tehdit"]?.let { ltext(it, "$where.tehdit") } ?: LText.EMPTY,
                brief = c.strings("brifing"),
                operations = ops,
                bottleneck = c.strings("darbogaz"),
                bottleneckDay = c.int("darbogaz_gun"),
                overflowEffects = c.array("tasma_etkiler").flatMap { effects(it.jsonObject, "$where.tasma_etkiler") },
                overflowText = c["tasma"]?.let { ltext(it, "$where.tasma") } ?: LText.EMPTY,
                earlyEntry = (c["erken"] as? JsonPrimitive)?.booleanOrNull ?: true,
                countdown = c["geri_sayim"]?.let { ltext(it, "$where.geri_sayim") },
                preparations = c.array("hazirlik").map { x ->
                    val h = x.jsonObject
                    val hid = h.string("id") ?: throw ContentFormatException("$where.hazirlik: 'id' eksik")
                    com.yukselis.engine.world.PreparationDef(
                        id = hid,
                        name = ltext(h["ad"], "$where.hazirlik.$hid.ad"),
                        description = ltext(h["aciklama"], "$where.hazirlik.$hid.aciklama"),
                        result = ltext(h["sonuc"], "$where.hazirlik.$hid.sonuc"),
                        cost = h.int("bedel") ?: 1,
                        condition = h["kosul"]?.let { condition(it.jsonObject) },
                        operationEffects = h.array("operasyonda").flatMap { effects(it.jsonObject, "$where.hazirlik.$hid.operasyonda") },
                    )
                },
            )
        }
        return ChapterTable(list.sortedBy { it.no }, bases)
    }

    private fun parseTower(o: JsonObject): TowerDef {
        val lvl = o["seviye"] as? JsonObject
        return TowerDef(
            region = o.string("bolge") ?: throw ContentFormatException("kule: 'bolge' eksik"),
            condition = o["kosul"]?.let { condition(it.jsonObject) },
            baseLevel = lvl?.double("taban") ?: 20.0,
            growth = lvl?.double("carpan") ?: 1.0385,
            floors = o.int("kat_sayisi") ?: 100,
            bands = o.array("bantlar").mapIndexed { i, e ->
                val b = e.jsonObject
                TowerBand(
                    min = b.int("min") ?: throw ContentFormatException("kule.bantlar[$i]: 'min' eksik"),
                    max = b.int("max") ?: throw ContentFormatException("kule.bantlar[$i]: 'max' eksik"),
                    enemies = b.strings("dusmanlar"),
                    environment = b.strings("ortam"),
                    lines = b.array("metinler").map { ltext(it, "kule.bantlar[$i].metin") },
                )
            },
            storyFloors = (o["hikaye_katlari"] as? JsonObject)?.map { (k, v) ->
                (k.toIntOrNull() ?: throw ContentFormatException("kule.hikaye_katlari: '$k' kat numarası olmalı")) to v.str(k)
            }?.toMap() ?: emptyMap(),
        )
    }

    /** `endings.yaml`: sonlar, OP MC türleri, NPC ve fraksiyon epilogları. */
    private fun fillEndings(table: EndingTable, o: JsonObject) {
        o.objectOf("sonlar")?.forEach { (id, v) ->
            val e = v.jsonObject
            table.endings[id] = EndingDef(
                id = id,
                name = ltext(e["ad"], "son.$id.ad"),
                summary = ltext(e["ozet"], "son.$id.ozet"),
                condition = e["kosul"]?.let { condition(it.jsonObject) },
                scenes = e.strings("sahne").ifEmpty { throw ContentFormatException("son $id: 'sahne' eksik") },
                order = e.int("sira") ?: 0,
                hidden = e.bool("gizli"),
                fallback = e.bool("yedek"),
            )
        }
        o.objectOf("arketipler")?.forEach { (id, v) ->
            val a = v.jsonObject
            table.archetypes += ArchetypeDef(id, ltext(a["ad"], "arketip.$id.ad"), ltext(a["aciklama"], "arketip.$id.aciklama"), a["kosul"]?.let { condition(it.jsonObject) })
        }
        fun lines(x: JsonObject?, kind: String) = x?.map { (id, v) ->
            id to v.jsonArray.map { l ->
                val lo = l.jsonObject
                EpilogueLine(lo["kosul"]?.let { condition(it.jsonObject) }, ltext(lo["metin"], "epilog.$kind.$id"))
            }
        }?.toMap() ?: emptyMap()
        val ep = o["epilog"] as? JsonObject
        table.npcFates += lines(ep?.get("npc") as? JsonObject, "npc")
        table.factionFates += lines(ep?.get("fraksiyon") as? JsonObject, "fraksiyon")
    }

    private fun parseFeat(id: String, o: JsonObject) = FeatDef(
        id = id,
        name = ltext(o["ad"], "$id.ad"),
        reactions = o["tepkiler"]?.jsonObject?.map { (k, v) -> k to v.int("$id.tepkiler.$k") }?.toMap() ?: emptyMap(),
        lines = o["satirlar"]?.jsonObject?.map { (k, v) -> k to ltext(v, "$id.satirlar.$k") }?.toMap() ?: emptyMap(),
        titleAt = (o["unvan"] as? JsonObject)?.int("esik"),
        title = (o["unvan"] as? JsonObject)?.string("id"),
    )

    fun parseStory(text: String): StoryFile = parseStory(YamlTree.parseObject(text, "hikaye"))

    internal fun parseStory(root: JsonObject): StoryFile {
        val scenes = root.array("sahneler").map { parseScene(it.jsonObject) }
        return StoryFile(root.string("baslangic"), scenes)
    }

    fun parseNames(text: String): List<NameEntry> = parseNames(YamlTree.parseObject(text, "names.yaml"))

    internal fun parseNames(root: JsonObject): List<NameEntry> {
        return NameEntry.Kind.entries.flatMap { kind ->
            val group = root[kind.tokenKey]?.jsonObject ?: JsonObject(emptyMap())
            group.map { (id, value) ->
                val o = value.jsonObject
                val full = ltext(o["ad"], "$id.ad")
                NameEntry(
                    id, kind, full, o["kisa_ad"]?.let { ltext(it, "$id.kisa_ad") } ?: full, o.string("zamir") ?: "they",
                    possessive = o.bool("iyelik"),
                )
            }
        }
    }

    fun parseTitles(text: String): List<TitleDef> = parseTitles(YamlTree.parseObject(text, "titles.yaml"))

    internal fun parseTitles(root: JsonObject): List<TitleDef> {
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
                    narration = i.bool("anlati"),
                    text = ltext(i["metin"], "$id.sezgi"),
                    condition = i["kosul"]?.let { condition(it.jsonObject) } ?: throw ContentFormatException("$id: sezgide 'kosul' eksik"),
                )
            },
            companionLines = o.array("yoldas_tepkileri").map { e ->
                val c = e.jsonObject
                com.yukselis.engine.story.CompanionLine(
                    npc = c.string("npc") ?: throw ContentFormatException("$id: yoldas_tepkileri.npc eksik"),
                    text = ltext(c["metin"], "$id.yoldas_tepkisi"),
                    condition = c["kosul"]?.let { condition(it.jsonObject) },
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
            chronicle = o["kronik"]?.let { ltext(it, "$sceneId.$id.kronik") },
            chronicleWeight = o.int("kronik_agirlik"),
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

    fun condition(o: JsonObject, endings: EndingTable = EndingTable.current): Condition {
        val parts = o.flatMap { (key, value) ->
            when (key) {
                "stat" -> value.jsonObject.map { (k, v) -> Condition.StatAtLeast(stat(k), v.int(k)) }
                // Tek bayrak ya da liste (hepsi): `bayrak_yok: [a, b]`.
                "bayrak" -> flagList(value, key).map { Condition.HasFlag(it) }
                "bayrak_yok" -> flagList(value, key).map { Condition.LacksFlag(it) }
                "herhangi" -> listOf(Condition.AnyOf((value as? kotlinx.serialization.json.JsonArray ?: throw ContentFormatException("herhangi: liste bekleniyordu"))
                    .map { condition(it.jsonObject, endings) }))
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
                "oz_kristali_min" -> listOf(Condition.CrystalsAtLeast(value.int(key)))
                "gizli_fark_min" -> listOf(Condition.HiddenGapAtLeast(value.int(key)))
                "bolge" -> listOf(Condition.InRegion(value.str(key)))
                "bolum" -> value.int(key).let { listOf(Condition.ChapterAtLeast(it), Condition.ChapterAtMost(it)) }
                "bolum_min" -> listOf(Condition.ChapterAtLeast(value.int(key)))
                "bolum_max" -> listOf(Condition.ChapterAtMost(value.int(key)))
                "gorevde" -> listOf(Condition.OnMission(value.jsonPrimitive.booleanOrNull ?: true))
                "gorev_turu" -> listOf(Condition.OnMission(true, value.str(key)))
                "perde" -> listOf(Condition.InAct(value.int(key)))
                "gun_min" -> listOf(Condition.DayAtLeast(value.int(key)))
                "gun_max" -> listOf(Condition.DayAtMost(value.int(key)))
                "gorev" -> value.jsonObject.map { (k, v) ->
                    Condition.QuestIs(k, QuestStatus.fromKey(v.str(k)) ?: throw ContentFormatException("görev $k: durum basla/tamam/basarisiz olmalı"))
                }
                // ---- Aşama 5 ----
                "npc_hayatta" -> listOf(Condition.NpcAlive(value.str(key)))
                "npc_olu" -> listOf(Condition.NpcDead(value.str(key)))
                "npc_gitti" -> listOf(Condition.NpcGoneCond(value.str(key)))
                "yoldas" -> listOf(Condition.IsCompanion(value.str(key)))
                "iliski_min" -> value.jsonObject.flatMap { (npc, v) -> relDeltas(v.jsonObject, "iliski_min.$npc").map { (a, n) -> Condition.RelationAtLeast(npc, a, n) } }
                "iliski_max" -> value.jsonObject.flatMap { (npc, v) -> relDeltas(v.jsonObject, "iliski_max.$npc").map { (a, n) -> Condition.RelationAtMost(npc, a, n) } }
                "npc_fraksiyon" -> value.jsonObject.map { (npc, v) -> Condition.NpcInFaction(npc, v.str(npc)) }
                "npc_isaret" -> value.jsonObject.map { (npc, v) -> Condition.NpcMarked(npc, v.str(npc)) }
                "npc_seviye_min" -> value.jsonObject.map { (npc, v) -> Condition.NpcLevelAtLeast(npc, v.int(npc)) }
                "hafiza" -> value.jsonObject.map { (npc, v) -> Condition.Remembers(npc, v.str(npc)) }
                "bilgi" -> listOf(Condition.Knows(value.str(key)))
                "npc_savasabilir" -> listOf(Condition.NpcCanFight(value.str(key)))
                "lider_fraksiyon" -> listOf(Condition.TopReputation(value.str(key), LEADER_POOL))
                "yoldas_yer" -> listOf(if ((value as? kotlinx.serialization.json.JsonPrimitive)?.content == "false") Condition.NoPartyRoom else Condition.PartyRoom)
                "gorunen_seviye_min" -> listOf(Condition.PublicLevelAtLeast(value.int(key)))
                "gorunen_seviye_max" -> listOf(Condition.PublicLevelAtMost(value.int(key)))
                "iz" -> listOf(Condition.HasTrait(value.str(key)))
                // ---- Aşama 7 ----
                "kademe_min" -> listOf(Condition.GradeAtLeast(Grade.fromKey(value.str(key)) ?: throw ContentFormatException("kademe_min: F–S olmalı")))
                "ordu_min" -> listOf(Condition.ArmyAtLeast(value.int(key)))
                "yoldas_sayisi_max" -> listOf(Condition.CompanionsAtMost(value.int(key)))
                "oz" -> listOf(Condition.HasEssence(value.str(key)))
                "yol" -> listOf(Condition.HasPath(value.str(key)))
                "son_uygun" -> listOf(Condition.EndingEligible(value.str(key), endings))
                "hicbir_son" -> listOf(Condition.NoEnding(endings))
                "son" -> listOf(Condition.EndingIs(value.str(key)))
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
                        npc = t.string("npc"),
                        reminder = t["hatirlatma"]?.let { ltext(it, "$where.tohum.hatirlatma") },
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
            "kontrol" -> value.jsonObject.map { (k, v) -> Effect.SetControl(k, v.str(k)) }
            "mana" -> listOf(Effect.AddMp(value.int(key)))
            // ---- Aşama 5 ----
            "iliski" -> value.jsonObject.map { (npc, v) -> Effect.ShiftRelation(npc, relDeltas(v.jsonObject, "$where.iliski.$npc")) }
            "hafiza" -> listOf(
                value.jsonObject.let { h ->
                    Effect.AddMemory(
                        npc = h.string("npc") ?: throw ContentFormatException("$where: hafiza.npc eksik"),
                        memory = h.string("id") ?: throw ContentFormatException("$where: hafiza.id eksik"),
                        weight = h.int("agirlik") ?: 10,
                    )
                },
            )
            "npc_ol" -> listOf(Effect.KillNpc(value.str(key)))
            "yoldas_katil" -> listOf(Effect.JoinParty(value.str(key)))
            "yoldas_ayril" -> listOf(Effect.LeaveParty(value.str(key)))
            "npc_gitti" -> listOf(Effect.NpcGone(value.str(key)))
            "npc_fraksiyon" -> value.jsonObject.map { (npc, v) -> Effect.SetNpcFaction(npc, v.str(npc)) }
            "tanis" -> listOf(Effect.MeetNpc(value.str(key)))
            "npc_seviye" -> value.jsonObject.map { (npc, v) -> Effect.NpcLevel(npc, v.int(npc)) }
            "npc_iz" -> value.jsonObject.map { (npc, v) -> Effect.NpcTrait(npc, v.str(npc)) }
            "npc_isaret" -> value.jsonObject.map { (npc, v) -> Effect.NpcMark(npc, v.str(npc)) }
            "iz" -> listOf(Effect.PlayerTrait(value.str(key)))
            "bilgi" -> listOf(Effect.Learn(value.str(key)))
            "yalan" -> listOf(
                value.jsonObject.let { y ->
                    Effect.TellLie(
                        npc = y.string("npc") ?: throw ContentFormatException("$where: yalan.npc eksik"),
                        topic = y.string("konu") ?: throw ContentFormatException("$where: yalan.konu eksik"),
                    )
                },
            )
            "kisisel_gorev" -> value.jsonObject.map { (npc, v) ->
                Effect.PersonalQuest(npc, QuestStatus.fromKey(v.str(npc)) ?: throw ContentFormatException("$where: kisisel_gorev durumu basla/tamam/basarisiz"))
            }
            "npc_odul" -> listOf(Effect.NpcReward(value.str(key)))
            "ordu" -> value.jsonObject.map { (u, v) -> Effect.AddArmy(u, v.int(u)) }
            "ordu_kazan" -> listOf(Effect.GrowArmy(value.int(key)))
            // ---- Aşama 7 ----
            "xp_seviye" -> listOf(Effect.GainLevelXp(value.int(key)))
            "kule_kat" -> listOf(Effect.TowerFloor(value.int(key)))
            "yol" -> listOf(Effect.SetPath(value.str(key)))
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
        // Satır düz {tr, en} ya da koşullu {metin: {tr, en}, kosul: {…}} olabilir.
        lines = o.array("satirlar").map { e -> (e as? JsonObject)?.get("metin")?.let { ltext(it, "$id.satir") } ?: ltext(e, "$id.satir") },
        condition = o["kosul"]?.let { condition(it.jsonObject) } ?: throw ContentFormatException("uyanış $id: 'kosul' eksik"),
        effects = o.array("etkiler").flatMap { effects(it.jsonObject, "uyanış $id") },
        lineConditions = o.array("satirlar").map { e -> (e as? JsonObject)?.get("kosul")?.let { condition(it.jsonObject) } },
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

    /** "İlk fraksiyon eğilimi" yarışan fraksiyonlar (Kadimler insan kampı değildir). */
    private val LEADER_POOL = listOf("lonca", "sessiz_ag", "siginak", "yukselenler")

    private fun flagList(value: JsonElement, where: String): List<String> =
        (value as? JsonArray)?.map { it.str(where) } ?: listOf(value.str(where))

    private fun JsonElement.str(where: String): String =
        jsonPrimitive.contentOrNull ?: throw ContentFormatException("$where: metin bekleniyordu")
}
