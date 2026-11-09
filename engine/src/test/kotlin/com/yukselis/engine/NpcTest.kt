package com.yukselis.engine

import com.yukselis.engine.combat.CombatAction
import com.yukselis.engine.combat.CombatSpec
import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.MoralAxis
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.npc.Memory
import com.yukselis.engine.npc.NpcEngine
import com.yukselis.engine.npc.NpcStatus
import com.yukselis.engine.npc.RelAxis
import com.yukselis.engine.npc.Relation
import com.yukselis.engine.npc.ThresholdKind
import com.yukselis.engine.save.SaveCodec
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Stat
import com.yukselis.engine.world.HubAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Aşama 5: NPC sistemi (model, gündem, eşikler, ölüm, yoldaşlar, sıralama, OP MC tepkileri, kayıt). */
class NpcTest {
    private val engine = StoryEngine(ContentLoader.load(DirectoryContentSource(File("../content"))))
    private val npc = engine.npcEngine
    private val lang = Lang.TR

    private fun atHub(seed: Long = 1): WorldState {
        var s = engine.newGame("T", GameMode.NORMAL, seed)
        s = s.copy(sceneId = "ilk_sabah", day = 1)
        return engine.throughBriefing(engine.choose(s, "gune_basla", lang).state)
    }

    private fun apply(s: WorldState, vararg e: Effect) = engine.applyEffects(s, e.toList(), lang).state

    private fun withRelation(s: WorldState, id: String, r: Relation) =
        s.copy(npcs = s.npcs + (id to s.npcs.getValue(id).copy(relation = r, known = true)))

    /** Gün atlatır (NPC günlük adımı dahil). */
    private fun nextDay(s: WorldState) = engine.world.startDay(s, advance = true).state

    // ---- Model ve büyüme ----

    @Test
    fun `yeni oyunda NPCler icerikten kurulur`() {
        val s = engine.newGame("T", GameMode.NORMAL, 1)
        val deniz = s.npcs.getValue("deniz_arman")
        assertEquals(Relation(50, 20, 0, 40), deniz.relation)
        assertTrue(deniz.known)
        assertEquals(2, deniz.level)
        // Süresiz hedef arka plan amacıdır; ilerleyen (süreli) hedef yok.
        assertNull(deniz.goal)
        assertEquals("Kız kardeşini bulmak", npc.goalName(s, "deniz_arman")?.tr)
        assertEquals(8, s.npcs.size)
        assertFalse(s.npcs.getValue("aras_kurt").known)
    }

    @Test
    fun `NPC buyume egrisi GDD sayilari ve oyuncudan yavas`() {
        assertEquals(36, Progression.npcDailyGrowth(1.0, 1))
        assertEquals(20, Progression.npcDailyGrowth(1.0, 10))
        fun levelAt(days: Int, rate: Double = 1.0): Int {
            var lv = 1
            var pr = 0
            repeat(days) {
                pr += Progression.npcDailyGrowth(rate, lv)
                while (pr >= 100) { pr -= 100; lv++ }
            }
            return lv
        }
        assertTrue(levelAt(30) in 7..10)
        assertTrue(levelAt(180) in 27..32)
        assertTrue(levelAt(365) in 42..48)
        for (d in listOf(30, 90, 180, 365)) assertTrue(levelAt(d, 1.6) < Progression.expectedLevel(d))
    }

    @Test
    fun `her gun NPCler bir adim atar ve hafiza solar`() {
        var s = atHub()
        s = npc.remember(s, "deniz_arman", "kucuk", 3)
        s = npc.remember(s, "deniz_arman", "buyuk", 60)
        s = apply(s, Effect.TellLie("deniz_arman", "deniz_plan_yalani"))
        val before = s.npcs.getValue("aras_kurt")
        repeat(3) { s = nextDay(s) }
        val after = s.npcs.getValue("aras_kurt")
        assertTrue(after.level > before.level || after.progress > before.progress)
        val mem = s.npcs.getValue("deniz_arman").memories.map { it.id }
        assertFalse("kucuk" in mem)
        assertTrue("buyuk" in mem)
    }

    // ---- Etkiler ----

    @Test
    fun `iliski etkisi dort ekseni oynatir ve bildirir`() {
        val s0 = atHub()
        val o = engine.applyEffects(s0, listOf(Effect.ShiftRelation("aras_kurt", mapOf(RelAxis.RESPECT to 10, RelAxis.FEAR to 5))), Lang.EN)
        val a = o.state.npcs.getValue("aras_kurt")
        assertEquals(30, a.relation.respect)
        assertEquals(5, a.relation.fear)
        assertTrue(a.known)
        // Paket 2: sayısız tek satır ("Ash: respects you more, is a little more afraid of you").
        val line = o.notices.first { it.title.contains("respects you more") }
        assertTrue(line.title.contains("afraid"))
        assertTrue(o.notices.none { it.title.contains("+10") })
    }

    @Test
    fun `odak NPC belirteci ve etkileri cozulur`() {
        val s = atHub().copy(focusNpc = "aras_kurt")
        val r = apply(s, Effect.ShiftRelation("@", mapOf(RelAxis.TRUST to 7)))
        assertEquals(-3, r.npcs.getValue("aras_kurt").relation.trust)
        assertTrue(com.yukselis.engine.story.Condition.RelationAtLeast("@", RelAxis.TRUST, -3).test(r))
    }

    // ---- Eşik olayları ----

    @Test
    fun `ihanet esigi 5-15 gun sonraya tohum eker`() {
        var s = withRelation(atHub(), "aras_kurt", Relation(trust = -50))
        s = nextDay(s)
        val seed = s.seeds.single { it.npc == "aras_kurt" }
        assertEquals(ThresholdKind.BETRAYAL.genericEvent, seed.eventId)
        assertTrue(seed.dueDay - s.day in 5..15)
        assertTrue("esik_ihanet" in s.npcs.getValue("aras_kurt").marks)
        // Bir kez: ikinci gün yeni tohum yok.
        assertEquals(1, nextDay(s).seeds.count { it.npc == "aras_kurt" })
    }

    @Test
    fun `kisisel gorev esigi odakli olay acar`() {
        var s = withRelation(apply(atHub(), Effect.JoinParty("deniz_arman")), "deniz_arman", Relation(trust = 60, bond = 75))
        s = nextDay(s)
        val seed = s.seeds.single { it.npc == "deniz_arman" }
        assertEquals("esik_kisisel", seed.eventId)
        // Vadesinde zorunlu olay olarak gelir, odak NPC Deniz'dir; metin Deniz'in hedefini anar.
        s = s.copy(day = seed.dueDay)
        s = engine.world.enterHub(s).state
        assertEquals("esik_kisisel_1", s.sceneId)
        assertEquals("deniz_arman", s.focusNpc)
        val text = engine.view(s, Lang.EN).current.paragraphs.joinToString(" ")
        assertTrue(text.contains("Devon"))
        assertTrue(text.contains("Find their little sister"))
        assertTrue(text.contains("they finally say it out loud"))
        s = engine.choose(s, "kabul", lang).state
        assertEquals(com.yukselis.engine.world.QuestStatus.ACTIVE, s.quests["kisisel_deniz_arman"])
        assertTrue(s.seeds.any { it.eventId == "esik_kisisel_son" && it.npc == "deniz_arman" })
    }

    @Test
    fun `korkak ve sadakatsiz yoldas kacar, rakip duello ister`() {
        var s = atHub()
        s = s.copy(player = s.player.copy(stats = s.player.stats.plus(Stat.CHA, 5)))
        s = withRelation(apply(s, Effect.JoinParty("mert_kilic")), "mert_kilic", Relation(fear = 70))
        // Yoldaş olmayan korkak NPC'de kaçış sahnesi açılmaz.
        s = withRelation(s, "vahit", Relation(fear = 70))
        s = withRelation(s, "aras_kurt", Relation(respect = 70, trust = -5))
        s = nextDay(s)
        assertEquals("esik_kacis", s.seeds.single { it.npc == "mert_kilic" }.eventId)
        assertTrue(s.seeds.none { it.npc == "vahit" })
        assertEquals("esik_rakip", s.seeds.single { it.npc == "aras_kurt" }.eventId)
        assertTrue(NpcEngine.MARK_RIVAL in s.npcs.getValue("aras_kurt").marks)
    }

    @Test
    fun `korkak ama baglanmis yoldas itaat eder`() {
        // QA P1-2: esik_itaat'a hiçbir yoldaş ulaşamıyordu (tek korkak yoldaş Mert'in sadakati 5).
        var s = atHub()
        s = withRelation(apply(s, Effect.JoinParty("mert_kilic")), "mert_kilic", Relation(fear = 70, bond = NpcEngine.OBEY_BOND + 5))
        s = nextDay(s)
        assertEquals("esik_itaat", s.seeds.single { it.npc == "mert_kilic" }.eventId)
    }

    @Test
    fun `degerleri uzaklasan yoldas ayrilir, digeri fraksiyon degistirir`() {
        var s = apply(atHub(), Effect.JoinParty("deniz_arman"))
        s = s.copy(morality = com.yukselis.engine.core.Morality(-60, 10, -60))
        s = withRelation(s, "kaya_demir", s.npcs.getValue("kaya_demir").relation)
        s = nextDay(s)
        assertEquals("esik_ayrilik", s.seeds.single { it.npc == "deniz_arman" }.eventId)
        val kaya = engine.content.npcs.getValue("kaya_demir")
        if (npc.valueDrift(kaya.values, s.morality) > NpcEngine.DEPARTURE_DISTANCE) {
            assertEquals(npc.nearestFaction(kaya.values, exclude = "lonca"), s.npcs.getValue("kaya_demir").faction)
        }
    }

    @Test
    fun `notr oyuncu kimseyi kacirmaz ve Yanki genel esik almaz`() {
        var s = apply(atHub(), Effect.JoinParty("deniz_arman"))
        s = withRelation(s, "yanki", Relation(trust = -60, respect = 70, bond = 80))
        repeat(2) { s = nextDay(s) }
        assertTrue(s.seeds.none { it.npc == "deniz_arman" || it.npc == "yanki" })
        assertEquals(engine.content.npcs.getValue("elif_sari").faction, s.npcs.getValue("elif_sari").faction)
    }

    @Test
    fun `sureli hedef ilerler ve tamamlaninca etkisi uygulanir`() {
        var s = atHub()
        val kaya = engine.content.npcs.getValue("kaya_demir")
        val timed = kaya.goals.filter { it.days > 0 }.maxByOrNull { it.priority }!!
        assertEquals(timed.id, s.npcs.getValue("kaya_demir").goal)
        val before = s.npcs.getValue("kaya_demir").level
        repeat(timed.days + 1) { s = nextDay(s) }
        assertTrue(timed.id in s.npcs.getValue("kaya_demir").doneGoals)
        assertTrue(s.npcs.getValue("kaya_demir").level > before)
    }

    @Test
    fun `ingilizce zamir ve fiil uyumu`() {
        val s = atHub().copy(focusNpc = "aras_kurt")
        val r = com.yukselis.engine.i18n.TokenResolver(engine.content.names)
        assertEquals("he says", r.resolve("{zamir:aras_kurt|o} say{zamir:aras_kurt|s}", Lang.EN, "T"))
        assertEquals("They say", r.resolve("{zamir:deniz_arman|o|b} say{zamir:deniz_arman|s}", Lang.EN, "T"))
        assertEquals("her", r.resolve("{zamir:elif_sari|onun}", Lang.EN, "T"))
        assertEquals("", r.resolve("{zamir:elif_sari|onun}", Lang.TR, "T"))
        assertNotNull(s)
    }

    @Test
    fun `gecmis pasajlar kendi odak NPCsiyle cozulur`() {
        var s = withRelation(apply(atHub(), Effect.JoinParty("deniz_arman")), "deniz_arman", Relation(trust = 60, bond = 75))
        s = nextDay(s)
        s = engine.world.enterHub(s.copy(day = s.seeds.single { it.npc == "deniz_arman" }.dueDay)).state
        s = engine.choose(s, "sonra", lang).state
        // Odak başka birine geçse de geçmiş Deniz'i anar.
        val later = s.copy(focusNpc = "aras_kurt")
        val past = engine.view(later.copy(sceneId = "esik_kisisel_son_kayip"), Lang.EN).history.last().paragraphs.joinToString(" ")
        assertTrue(past.contains("Devon"))
    }

    // ---- Ölüm ----

    @Test
    fun `olum kalicidir ve yay dugumleri kapanir`() {
        var s = apply(atHub(), Effect.JoinParty("deniz_arman"), Effect.PlantSeed("deniz_hesap", 1, 1, null))
        s = apply(s, Effect.KillNpc("deniz_arman"))
        val d = s.npcs.getValue("deniz_arman")
        assertEquals(NpcStatus.DEAD, d.status)
        assertFalse(d.companion)
        assertTrue(s.seeds.none { it.eventId == "deniz_hesap" })
        assertFalse(engine.world.director.eligible(s.copy(region = "ogretici_orman", day = 5), engine.content.events.getValue("deniz_bulusma")))
        // Ölü NPC'ye etki işlemez.
        assertEquals(d.relation, apply(s, Effect.ShiftRelation("deniz_arman", mapOf(RelAxis.TRUST to 50))).npcs.getValue("deniz_arman").relation)
    }

    @Test
    fun `yoldas olunce uc gun savasmamak yas tutmaktir`() {
        var s = apply(atHub(), Effect.JoinParty("deniz_arman"), Effect.KillNpc("deniz_arman"))
        repeat(3) { s = nextDay(s) }
        assertTrue(s.hasFlag(NpcEngine.FLAG_MOURNED))
    }

    // ---- Yoldaşlar ----

    @Test
    fun `yoldas kapasitesi KAR ve kademeye bagli`() {
        val s = atHub()
        assertEquals(1, npc.capacity(s))
        var full = apply(s, Effect.JoinParty("deniz_arman"), Effect.JoinParty("elif_sari"))
        assertEquals(listOf("deniz_arman"), npc.companions(full).map { it.id })
        full = full.copy(player = full.player.copy(stats = full.player.stats.plus(Stat.CHA, 5)))
        assertEquals(2, npc.capacity(full))
        assertEquals(2, npc.companions(apply(full, Effect.JoinParty("elif_sari"))).size)
    }

    @Test
    fun `yoldaslar savasa katilir ve ayni tohum ayni savas`() {
        val s = apply(atHub(), Effect.JoinParty("deniz_arman"))
        val spec = CombatSpec(listOf("sis_tazisi", "sis_tazisi"), win = WorldState.HUB, flee = WorldState.HUB)
        val start = engine.startCombat(s, spec, lang).state
        assertEquals(1, start.combat!!.allies.size)
        val a = fightOut(engine, start).first
        val b = fightOut(engine, engine.startCombat(s, spec, lang).state).first
        assertEquals(a, b)
        val log = generateSequence(start) { st -> st.combat?.let { engine.combatAction(st, engine.autoCombatAction(st), lang).state } }
            .take(6).mapNotNull { it.combat }.flatMap { it.log }.map { it.prose.tr }
        assertTrue(log.any { "Deniz" in it })
    }

    @Test
    fun `NPC ile duello ve acimasiz infaz kalici olumdur`() {
        var s = atHub().copy(player = atHub().player.copy(level = 30, stats = atHub().player.stats.plus(Stat.STR, 40)))
        s = engine.startCombat(s, CombatSpec(listOf("npc:aras_kurt"), win = WorldState.HUB), lang).state
        var guard = 0
        while (s.combat?.pendingExecution == null && s.combat != null && guard++ < 100) {
            s = engine.combatAction(s, CombatAction.Attack(0), lang).state
        }
        if (s.combat != null) s = engine.combatAction(s, CombatAction.Execute("acimasiz"), lang).state
        assertNull(s.combat)
        assertEquals(NpcStatus.DEAD, s.npcs.getValue("aras_kurt").status)
        // Düelloda yoldaş yok, adı artikelsiz.
        val duel = engine.startCombat(apply(atHub(), Effect.JoinParty("deniz_arman")), CombatSpec(listOf("npc:aras_kurt"), win = WorldState.HUB), Lang.EN).state
        assertTrue(duel.combat!!.allies.isEmpty())
        assertTrue(engine.combatView(duel, Lang.EN)!!.log.none { it.prose.contains("The Ash") || it.prose.contains("the Ash") })
    }

    // ---- Tanıklar ve yoldaş görüşleri ----

    @Test
    fun `tanik NPC acimasizligi degerlerine gore yorumlar`() {
        val s = apply(atHub(), Effect.JoinParty("deniz_arman"))
        val r = npc.interpret(s, mapOf(MoralAxis.MERCY to -8), null).state
        assertTrue(r.npcs.getValue("deniz_arman").relation.trust < s.npcs.getValue("deniz_arman").relation.trust)
        val withAras = s.copy(npcs = s.npcs + ("aras_kurt" to s.npcs.getValue("aras_kurt").copy(companion = true, known = true)))
        val r2 = npc.interpret(withAras, mapOf(MoralAxis.MERCY to -8), null).state
        assertTrue(r2.npcs.getValue("aras_kurt").relation.respect > withAras.npcs.getValue("aras_kurt").relation.respect)
    }

    @Test
    fun `onemli sahnede yoldas gorus bildirir`() {
        var s = apply(atHub(), Effect.JoinParty("deniz_arman"))
        s = engine.world.startEvent(s.copy(region = "ogretici_orman"), "yarali_avci", consumeSlot = false).state
        val view = engine.view(s, Lang.EN)
        val names = view.opinions.map { it.npcName }
        assertTrue("Devon" in names || "Echo" in names)
    }

    @Test
    fun `guce tepki ve aciga vurmada sayginlik`() {
        var s = apply(atHub(), Effect.JoinParty("deniz_arman"))
        s = s.copy(player = s.player.copy(level = 20), publicLevel = 2)
        val o = engine.applyEffects(s, listOf(Effect.Reveal), lang)
        assertTrue(o.state.npcs.getValue("deniz_arman").relation.respect > s.npcs.getValue("deniz_arman").relation.respect)
        assertTrue(o.notices.any { it.kind == com.yukselis.engine.story.NoticeKind.COMPANION })
    }

    // ---- Sıralama ----

    @Test
    fun `kuresel siralama gorunen ve gercek sira, rakip ilk unvani kapar`() {
        var s = atHub()
        assertEquals(14, s.hunters.size)
        s = s.copy(player = s.player.copy(level = 50))
        assertEquals(1, npc.playerRank(s, public = false))
        assertTrue(npc.playerRank(s, public = true) > 1)
        assertTrue(engine.ranking(s, lang).note.contains("#1"))
        // Aras seviye 10'a gelince oyuncudan önce "İlk Sınıflanan"ı kapar.
        s = s.copy(npcs = s.npcs + ("aras_kurt" to s.npcs.getValue("aras_kurt").copy(level = 10)))
        val o = engine.world.startDay(s, advance = true)
        assertEquals("aras_kurt", o.state.firstClaims["ilk_siniflanan"])
        // Oyuncu artık alamaz.
        val after = apply(o.state, Effect.GrantTitle("ilk_siniflanan"))
        assertFalse("ilk_siniflanan" in after.player.titles)
    }

    @Test
    fun `NPC seni gecince Sistem duyurur`() {
        var s = atHub()
        s = s.copy(npcs = s.npcs + ("aras_kurt" to s.npcs.getValue("aras_kurt").copy(level = 1)), publicLevel = 3)
        s = npc.updateRanking(s, announce = true).state
        s = s.copy(npcs = s.npcs + ("aras_kurt" to s.npcs.getValue("aras_kurt").copy(level = 9)))
        val o = npc.updateRanking(s, announce = true)
        assertTrue(o.notices.any { it.title.tr.contains("seni geçti") })
    }

    // ---- Bilgi, yalan, izler, ordu ----

    @Test
    fun `yalan supheciligine gore ortaya cikar`() {
        var s = apply(atHub(), Effect.TellLie("aras_kurt", "deniz_plan_yalani"), Effect.MeetNpc("aras_kurt"))
        var days = 0
        while (s.npcs.getValue("aras_kurt").memories.none { it.id.startsWith(Memory.LIE_REVEALED_PREFIX) } && days++ < 80) s = nextDay(s)
        assertTrue(days < 80)
        assertTrue(s.seeds.any { it.npc == "aras_kurt" && it.eventId == "esik_yalan" } || s.dayState.seen.contains("esik_yalan"))
    }

    @Test
    fun `bilgi gunlugu izler ve ordu`() {
        var s = apply(atHub(), Effect.Learn("yanki_parcasi"), Effect.PlayerTrait("kemik_kabuk"), Effect.NpcTrait("deniz_arman", "can_borcu"), Effect.GrowArmy(3))
        assertTrue(com.yukselis.engine.story.Condition.Knows("yanki_parcasi").test(s))
        assertEquals(atHub().player.stats[Stat.END] + 4, s.player.stats[Stat.END])
        // Deniz'in sadakati 9; Can Borcu +2 ama kişilik 10'da tavan yapar.
        assertEquals(10, npc.personality(s, "deniz_arman")[com.yukselis.engine.npc.Trait.LOYALTY])
        assertTrue(npc.armyPower(s) > 0)
        assertEquals(1, engine.journal(s, lang).knowledge.size)
    }

    // ---- Kayıt ----

    @Test
    fun `NPC durumu kayitla gidip gelir`() {
        var s = apply(atHub(), Effect.JoinParty("deniz_arman"), Effect.Learn("yanki_parcasi"), Effect.TellLie("deniz_arman", "deniz_plan_yalani"))
        s = npc.remember(s, "deniz_arman", "x", 50).copy(focusNpc = "deniz_arman")
        val back = SaveCodec.decode(SaveCodec.encode(s, 1)).state
        assertEquals(s.npcs, back.npcs)
        assertEquals(s.hunters, back.hunters)
        assertEquals(s.knowledge, back.knowledge)
        assertEquals("deniz_arman", back.focusNpc)
        // Savaş ortasındaki yoldaş da kayda girer.
        val fight = engine.startCombat(s, CombatSpec(listOf("sis_tazisi"), win = WorldState.HUB), lang).state
        if (fight.combat != null) assertEquals(fight.combat!!.allies, SaveCodec.decode(SaveCodec.encode(fight, 1)).state.combat!!.allies)
    }

    @Test
    fun `basliksiz oynanis yoldaslarla da cokmeden ilerler`() {
        repeat(40) { seed ->
            var s = apply(atHub(seed.toLong()), Effect.JoinParty("deniz_arman"))
            val rng = kotlin.random.Random(seed)
            var steps = 0
            while (!s.ended && s.day < 30 && steps++ < 400) {
                s = when {
                    s.dead -> break
                    s.prompts.isNotEmpty() -> {
                        val p = engine.view(s, lang).prompt!!
                        engine.resolvePrompt(s, p.options.first().id, lang, p.kind, p.subject).state
                    }
                    s.combat != null -> engine.combatAction(s, engine.autoCombatAction(s), lang).state
                    s.sceneId == WorldState.HUB -> {
                        val hub = engine.view(s, lang).hub!!
                        val action = when {
                            hub.mustCamp -> HubAction.Camp
                            hub.cards.isNotEmpty() && rng.nextBoolean() -> HubAction.Card(hub.cards[rng.nextInt(hub.cards.size)].id)
                            else -> listOf(HubAction.Explore, HubAction.Train, HubAction.Rest)[rng.nextInt(3)]
                        }
                        engine.hubAction(s, engine.legalOr(s, action, kotlin.random.Random(rng.nextLong())), lang).state
                    }
                    else -> {
                        val choices = engine.view(s, lang).choices.filter { it.enabled }
                        assertTrue("çıkmaz: ${s.sceneId}", choices.isNotEmpty())
                        engine.choose(s, choices[rng.nextInt(choices.size)].id, lang).state
                    }
                }
            }
            assertNotNull(s)
        }
    }
}
