package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.PromptKind
import com.yukselis.engine.core.SkillState
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.SaveCodec
import com.yukselis.engine.story.ActionRejected
import com.yukselis.engine.story.Content
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Affinity
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Stat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/** Aşama 2: Sistem mekanikleri, gerçek içerikle. */
class SystemMechanicsTest {
    private val engine = StoryEngine(ContentLoader.load(DirectoryContentSource(File("../content"))))
    private val content = engine.content
    private val lang = Lang.TR

    private fun fresh(): WorldState = engine.newGame("Test", GameMode.NORMAL, seed = 42)

    private fun apply(state: WorldState, vararg effects: Effect): WorldState = engine.applyEffects(state, effects.toList(), lang).state

    /** Seviye L'ye ulaşmak için gereken toplam XP. */
    private fun xpForLevel(level: Int): Long = (1 until level).sumOf { Progression.xpToNext(it) }

    @Test
    fun `yetenek ve meslek formulleri`() {
        assertEquals(4, Progression.skillXpToNext(1))
        assertEquals(36, Progression.skillXpToNext(9))
        assertEquals(180, (1 until 10).sumOf { Progression.skillXpToNext(it) })
        assertEquals(50, Progression.professionXpToNext(1))
        assertEquals(0, Progression.essenceSlots(Grade.E))
        assertEquals(1, Progression.essenceSlots(Grade.D))
        assertEquals(2, Progression.essenceSlots(Grade.A))
    }

    @Test
    fun `egilim etkisi vektoru gunceller ve siralama esitlikte enum sirasini kullanir`() {
        val s = apply(fresh(), Effect.ShiftAffinity(Affinity.GUARD, 3), Effect.ShiftAffinity(Affinity.STEALTH, 3))
        assertEquals(3, s.player.affinity[Affinity.GUARD])
        assertEquals(listOf(Affinity.STEALTH, Affinity.GUARD), s.player.affinity.ranked().take(2))
    }

    @Test
    fun `seviye 10 sinif teklifi iki egilim ve ucuncu secenek getirir`() {
        var s = apply(fresh(), Effect.ShiftAffinity(Affinity.MELEE, 5), Effect.ShiftAffinity(Affinity.GUARD, 3), Effect.ShiftAffinity(Affinity.MAGIC, 1))
        s = apply(s, Effect.GainXp(xpForLevel(10)))
        assertEquals(10, s.player.level)
        val prompt = s.prompts.single { it.kind == PromptKind.CLASS_OFFER }
        assertEquals(3, prompt.options.size)
        assertEquals("yakin_muharip", prompt.options[0])
        assertEquals("siper", prompt.options[1])
        // OP MC: gizli koşul yok → yine de bir gizli sınıf ("Yankı'nın önerisi").
        assertEquals(com.yukselis.engine.system.ClassKind.HIDDEN, content.classes.getValue(prompt.options[2]).kind)
        // İstem varken hikaye seçimi yapılamaz.
        try {
            engine.choose(s, s.let { engine.view(it, lang).choices.first().id }, lang)
            fail("İstem bekliyorken seçim yapılmamalı")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `gizli sinif kosulu saglaninca ucuncu secenek gizli sinif olur`() {
        var s = apply(fresh(), Effect.AddVar("ilk_vurus_kritik_oldurme", 50))
        s = apply(s, Effect.GainXp(xpForLevel(10)))
        assertEquals("avcinin_gozu", s.prompts.first { it.kind == PromptKind.CLASS_OFFER }.options.last())
    }

    @Test
    fun `sinif secimi yetenek verir ve sonraki seviyelerde otomatik stat artisi uygulanir`() {
        var s = apply(fresh(), Effect.ShiftAffinity(Affinity.MELEE, 5), Effect.GainXp(xpForLevel(10)))
        s = engine.resolvePrompt(s, "yakin_muharip", lang).state
        assertEquals("yakin_muharip", s.player.classId)
        assertNotNull(s.player.skill(content.classes.getValue("yakin_muharip").skill!!))
        val growth = content.classes.getValue("yakin_muharip").growth
        val before = s.player.stats
        s = apply(s, Effect.GainXp(Progression.xpToNext(10)))
        assertEquals(11, s.player.level)
        growth.forEach { (stat, d) -> assertEquals(before[stat] + d, s.player.stats[stat]) }
    }

    @Test
    fun `kademe esiginde seviye durur ve evrim gorevi acilir`() {
        var s = apply(fresh(), Effect.ShiftAffinity(Affinity.STEALTH, 5), Effect.GainXp(xpForLevel(10)))
        s = engine.resolvePrompt(s, "izsiz", lang).state
        s = s.copy(prompts = emptyList()) // meslek teklifini bu testte atla
        s = apply(s, Effect.GainXp(xpForLevel(30)))
        assertEquals(24, s.player.level)
        assertEquals(Progression.xpToNext(24), s.player.xp) // tampon: tam bir seviye
        val evo = s.prompts.first { it.kind == PromptKind.EVOLUTION }
        assertEquals("E", evo.subject)
        assertEquals(setOf("gece_bicagi", "sis_kacagi"), evo.options.toSet())

        // Evrim öncesinde başka istemler (meslek) varsa önce onlar; sırayla çöz.
        while (s.prompts.first().kind != PromptKind.EVOLUTION) s = engine.resolvePrompt(s, s.prompts.first().options.first(), lang).state
        s = engine.resolvePrompt(s, "gece_bicagi", lang).state
        assertEquals("gece_bicagi", s.player.classId)
        assertEquals(Grade.E, s.player.unlockedGrade)
        assertEquals(25, s.player.level)
        assertNotNull(s.player.skill(content.classes.getValue("gece_bicagi").skill!!))
    }

    @Test
    fun `tanimsiz evrimde genel evrim tum statlara bonus verir`() {
        // "İlk Evrilen" başkasında: unvan bonusu hesabı karıştırmasın.
        var s = fresh().copy(player = fresh().player.copy(level = 49, classId = "kirici", unlockedGrade = Grade.E), firstClaims = mapOf("ilk_evrilen" to "aras_kurt"))
        s = apply(s, Effect.GainXp(Progression.xpToNext(49)))
        val evo = s.prompts.first { it.kind == PromptKind.EVOLUTION }
        assertEquals(listOf("genel"), evo.options)
        val before = s.player.stats
        s = s.copy(prompts = listOf(evo))
        s = engine.resolvePrompt(s, "genel", lang).state
        val growth = content.classes.getValue("kirici").growth
        val mult = Progression.growthMultiplier(Grade.D)
        Stat.entries.forEach { assertEquals(before[it] + 1 + (growth[it] ?: 0) * mult, s.player.stats[it]) }
        assertEquals(50, s.player.level)
    }

    @Test
    fun `serbest stat dagitimi dogrulanir ve uygulanir`() {
        var s = fresh().let { it.copy(player = it.player.copy(freePoints = 3)) }
        try {
            engine.allocate(s, mapOf(Stat.STR to 4), lang)
            fail("Fazla puan dağıtılmamalı")
        } catch (_: ActionRejected) {
        }
        val hpBefore = s.player.hp
        s = engine.allocate(s, mapOf(Stat.STR to 1, Stat.END to 2), lang).state
        assertEquals(0, s.player.freePoints)
        assertEquals(6, s.player.stats[Stat.STR])
        assertEquals(7, s.player.stats[Stat.END])
        assertEquals(hpBefore + 20, s.player.hp)
    }

    @Test
    fun `yetenek kullanimla seviye alir ve seviye 10da evrim istemi gelir`() {
        var s = apply(fresh(), Effect.GrantSkill("golge_adimi"), Effect.SkillXp("golge_adimi", 5))
        assertEquals(2, s.player.skill("golge_adimi")!!.level)
        s = apply(s, Effect.SkillXp("golge_adimi", 1000))
        assertEquals(10, s.player.skill("golge_adimi")!!.level)
        val prompt = s.prompts.single { it.kind == PromptKind.SKILL_EVOLUTION }
        assertEquals(listOf("bosluk_adimi", "golge_avi", PromptKind.DECLINE), prompt.options)
        s = s.copy(prompts = listOf(prompt))
        s = engine.resolvePrompt(s, "bosluk_adimi", lang).state
        assertNull(s.player.skill("golge_adimi"))
        assertEquals(SkillState("bosluk_adimi"), s.player.skill("bosluk_adimi"))
    }

    @Test
    fun `kaynastirma iki maksimum yetenek ve kosulla acilir`() {
        var s = apply(fresh(), Effect.GrantSkill("alev_oku"), Effect.GrantSkill("ruzgar_adimi"))
        s = s.copy(player = s.player.copy(skills = s.player.skills.map { it.copy(level = 10) }))
        s = apply(s, Effect.AddVar("x", 0))
        assertTrue(s.prompts.none { it.kind == PromptKind.FUSION })
        s = apply(s, Effect.AddVar("hareketli_atis", 100))
        val fusion = s.prompts.first { it.kind == PromptKind.FUSION }
        s = s.copy(prompts = listOf(fusion))
        s = engine.resolvePrompt(s, PromptKind.ACCEPT, lang).state
        assertNotNull(s.player.skill("kuyruklu_yildiz_atisi"))
        assertNull(s.player.skill("alev_oku"))
    }

    @Test
    fun `reddedilen kaynastirma tekrar sorulmaz`() {
        var s = apply(fresh(), Effect.GrantSkill("alev_oku"), Effect.GrantSkill("ruzgar_adimi"), Effect.AddVar("hareketli_atis", 100))
        s = s.copy(player = s.player.copy(skills = s.player.skills.map { it.copy(level = 10) }), prompts = emptyList())
        s = apply(s, Effect.AddVar("x", 0))
        s = s.copy(prompts = s.prompts.filter { it.kind == PromptKind.FUSION })
        s = engine.resolvePrompt(s, PromptKind.DECLINE, lang).state
        s = apply(s, Effect.AddVar("x", 1))
        assertTrue(s.prompts.none { it.kind == PromptKind.FUSION })
    }

    @Test
    fun `ceza yetenegi kazanilinca ve her kademede stat dusurur`() {
        val cha = fresh().player.stats[Stat.CHA]
        val s = apply(fresh(), Effect.GrantSkill("ruh_emme"))
        assertEquals(cha - 1, s.player.stats[Stat.CHA])
    }

    @Test
    fun `ilk unvani npc kaparsa oyuncu alamaz`() {
        val claimed = fresh().copy(firstClaims = mapOf("ilk_kapi_kapatan" to "aras_kurt"))
        val s = apply(claimed, Effect.GrantTitle("ilk_kapi_kapatan"))
        assertFalse("ilk_kapi_kapatan" in s.player.titles)
        val mine = apply(fresh(), Effect.GrantTitle("ilk_kapi_kapatan"))
        assertEquals(WorldState.PLAYER_CLAIM, mine.firstClaims["ilk_kapi_kapatan"])
    }

    @Test
    fun `kardes katili siginak itibarina tavan koyar`() {
        var s = apply(fresh(), Effect.ShiftReputation("siginak", 40), Effect.GrantTitle("kardes_katili"))
        assertEquals(-50, s.reputation["siginak"])
        s = apply(s, Effect.ShiftReputation("siginak", 80))
        assertEquals(-50, s.reputation["siginak"])
    }

    @Test
    fun `meslek teklifi seviye 5te gelir ve simyaci iksir uretir`() {
        var s = apply(fresh(), Effect.GainXp(xpForLevel(5)))
        assertTrue(s.prompts.any { it.kind == PromptKind.PROFESSION })
        s = engine.resolvePrompt(s, "simyaci", lang).state
        assertEquals("simyaci", s.player.profession?.id)
        try {
            engine.craft(s, "kucuk_iyilesme_iksiri", lang)
            fail("Malzeme yokken üretim olmamalı")
        } catch (_: ActionRejected) {
        }
        s = apply(s, Effect.AddItem("kirmizi_ot", 2), Effect.AddItem("kaynak_suyu", 1), Effect.AddItem("cam_sise", 1))
        s = engine.craft(s, "kucuk_iyilesme_iksiri", lang).state
        assertEquals(1, s.player.itemCount("kucuk_iyilesme_iksiri"))
        assertEquals(0, s.player.itemCount("kirmizi_ot"))
        assertEquals(10, s.player.profession!!.xp)
        assertEquals(5, engine.bag(s, lang).recipes.count { it.id.isNotEmpty() && content.recipes[it.id]?.profession == "simyaci" && it.minLevel <= 2 })

        s = s.copy(player = s.player.copy(hp = 10))
        s = engine.useItem(s, "kucuk_iyilesme_iksiri", lang).state
        assertEquals(50, s.player.hp)
        assertEquals(0, s.player.itemCount("kucuk_iyilesme_iksiri"))
    }

    @Test
    fun `yeni oyunda gunluk gorev var ve tamamlaninca serbest puan verir`() {
        var s = fresh()
        val daily = assertNotNullAndGet(s.daily)
        val template = content.dailyTemplates.getValue(daily.templateId)
        s = apply(s, Effect.DailyProgress(template.kind, daily.steps))
        assertTrue(s.daily!!.completed)
        assertEquals(1, s.dailyStreak)
        assertEquals(1, s.player.freePoints)
    }

    @Test
    fun `gun gecince yeni gunluk uretilir ve ihmal seri sifirlar`() {
        var s = fresh().copy(dailyStreak = 4)
        s = apply(s, Effect.AdvanceDays(1))
        assertEquals(1, s.daily!!.day)
        assertEquals(0, s.dailyStreak)
    }

    @Test
    fun `ceza kancasi ceza bolgesine goturur ve geri doner`() {
        val start = fresh()
        val firstChoice = engine.view(start, lang).choices.first { it.enabled }.id
        val pending = start.copy(penaltyPending = true)
        var s = engine.choose(pending, firstChoice, lang).state
        assertEquals(Content.PENALTY_SCENE, s.sceneId)
        assertFalse(s.penaltyPending)
        val returnTo = s.returnStack.last()
        s = engine.choose(s, "dayan", lang).state
        assertEquals(returnTo, s.sceneId)
        assertTrue(s.returnStack.isEmpty())
        assertTrue(s.player.hp >= 1)
    }

    @Test
    fun `oz kavrayisi D kademesinde baglama istemi acar`() {
        var s = apply(fresh(), Effect.EssenceInsight("ates", 100))
        assertTrue(s.prompts.none { it.kind == PromptKind.ESSENCE_BIND })
        s = s.copy(player = s.player.copy(level = 50, unlockedGrade = Grade.D))
        s = apply(s, Effect.AddVar("x", 0))
        val p = s.prompts.first { it.kind == PromptKind.ESSENCE_BIND }
        s = s.copy(prompts = listOf(p))
        s = engine.resolvePrompt(s, PromptKind.ACCEPT, lang).state
        assertEquals(listOf("ates"), s.player.essences)
    }

    @Test
    fun `kayit v2 tum yeni alanlari korur`() {
        var s = apply(fresh(), Effect.ShiftAffinity(Affinity.DARK, 4), Effect.GrantSkill("golge_adimi"), Effect.AddItem("kirmizi_ot", 3),
            Effect.AddCredits(25), Effect.EssenceInsight("zaman", 40), Effect.GrantTitle("ilk_kapi_kapatan"), Effect.GainXp(xpForLevel(10)))
        s = s.copy(returnStack = listOf("ilk_gece"), penaltyPending = true)
        val back = SaveCodec.decode(SaveCodec.encode(s, 1L)).state
        assertEquals(s, back)
    }

    @Test
    fun `v1 kaydi acilir ve egilim degiskenleri vektore tasinir`() {
        val v1 = """{"v":1,"savedAt":5,"seed":9,"rng":9,"name":"Ece","mode":"NORMAL","scene":"ilk_canavar","day":0,"ended":false,
            "flags":["sistem_okundu"],"vars":{"egilim_algi":3,"egilim_yakin":1,"iliski_deniz_guven":10},"rep":{},
            "morality":{"merhamet":5,"duzen":0,"insanlik":0},
            "player":{"level":3,"xp":12,"free":6,"hp":100,"mp":85,"stats":{"str":5,"agi":5,"end":5,"int":5,"wis":5,"per":6,"wil":5,"cha":5},"titles":[]},
            "history":[]}"""
        val s = SaveCodec.decode(v1).state
        assertEquals(3, s.player.affinity[Affinity.RANGED])
        assertEquals(1, s.player.affinity[Affinity.MELEE])
        // v2 → v3: ilişki değişkeni Deniz'in Güven eksenine taşındı.
        assertEquals(emptyMap<String, Int>(), s.vars)
        assertEquals(10, s.npcs.getValue("deniz_arman").relation.trust)
        assertEquals(Grade.F, s.player.unlockedGrade)
        assertNull(s.player.classId)
        assertNull(s.daily)
    }

    @Test
    fun `istem gorunumu dili cozulmus secenekler verir`() {
        val s = apply(fresh(), Effect.GainXp(xpForLevel(10)))
        val view = engine.view(s, Lang.EN).prompt!!
        assertEquals(PromptKind.CLASS_OFFER, view.kind)
        assertEquals("Class offer", view.title)
        assertTrue(view.options.all { it.name.isNotBlank() && it.description.isNotBlank() })
    }

    // ---- OP MC (DECISIONS #19) ----

    @Test
    fun `yankinin onerisi en guclu egilime yakin gizli sinifi getirir`() {
        var s = apply(fresh(), Effect.ShiftAffinity(Affinity.MELEE, 5), Effect.GainXp(xpForLevel(10)))
        val offer = s.prompts.first { it.kind == PromptKind.CLASS_OFFER }
        assertEquals("kapi_yiyen", offer.options[2])
        s = s.copy(prompts = listOf(offer))
        val option = engine.view(s, Lang.EN).prompt!!.options.first { it.id == "kapi_yiyen" }
        assertTrue(option.echoNote!!.startsWith("[Echo's suggestion]"))
    }

    @Test
    fun `sinif seciminde oyun tarzina bagli benzersiz yetenek verilir ve dunyada tek sahiplidir`() {
        var s = apply(fresh(), Effect.ShiftAffinity(Affinity.GUARD, 5), Effect.GainXp(xpForLevel(10)))
        s = s.copy(prompts = s.prompts.filter { it.kind == PromptKind.CLASS_OFFER })
        s = engine.resolvePrompt(s, "siper", lang).state
        assertNotNull(s.player.skill("son_hat"))
        assertEquals(WorldState.PLAYER_CLAIM, s.firstClaims[WorldState.UNIQUE_CLAIM_PREFIX + "son_hat"])
        // Bir NPC almışsa sıradaki eğilimin Benzersiz yeteneği gelir.
        var t = apply(fresh(), Effect.ShiftAffinity(Affinity.GUARD, 5), Effect.ShiftAffinity(Affinity.SUMMON, 2), Effect.GainXp(xpForLevel(10)))
        t = t.copy(prompts = t.prompts.filter { it.kind == PromptKind.CLASS_OFFER }, firstClaims = mapOf(WorldState.UNIQUE_CLAIM_PREFIX + "son_hat" to "aras_kurt"))
        t = engine.resolvePrompt(t, "siper", lang).state
        assertNull(t.player.skill("son_hat"))
        assertNotNull(t.player.skill("yanki_lejyonu"))
    }

    @Test
    fun `sekmeler sistem verene kadar gizlidir`() {
        var s = fresh()
        assertTrue(s.unlockedTabs.isEmpty())
        val r = engine.applyEffects(s, listOf(Effect.GrantSkill("golge_adimi")), lang)
        s = r.state
        assertEquals(setOf(WorldState.TAB_SKILLS), s.unlockedTabs)
        assertTrue(r.notices.any { it.title == "[Yetenek penceresi verildi]" })
        s = apply(s, Effect.AddItem("kirmizi_ot", 1))
        assertTrue(WorldState.TAB_BAG in s.unlockedTabs)
        s = apply(s, Effect.UnlockTab(WorldState.TAB_MAP))
        assertEquals(WorldState.TABS.toSet(), s.unlockedTabs)
    }

    @Test
    fun `pasif sezgi yuksek statta onekiyle gorunur`() {
        val base = fresh().copy(sceneId = "ilk_canavar")
        assertTrue(engine.view(base, lang).current.insights.isEmpty())
        val sharp = apply(base, Effect.AddStat(Stat.PER, 1), Effect.SetFlag("yanki_bagi"))
        val insights = engine.view(sharp, Lang.EN).current.insights
        assertEquals(2, insights.size)
        assertTrue(insights[0].text.startsWith("[PER] "))
        assertFalse(insights[0].echo)
        assertTrue(insights[1].text.startsWith("[Sewer Rat"))
        assertTrue(insights[1].echo)
    }

    @Test
    fun `insanlik yuzdeligi seviyeyle duser ve sinirlidir`() {
        val low = Progression.humanityTopPercent(1, 0)
        val mid = Progression.humanityTopPercent(10, 0)
        val high = Progression.humanityTopPercent(200, 30)
        assertEquals(100.0, low, 1e-9)
        assertTrue(mid < 1.0)
        assertEquals(100.0 / Progression.HUMANITY, high, 1e-15)
        // Dünya güçlendikçe aynı seviye daha az etkileyici.
        assertTrue(Progression.humanityTopPercent(10, 100) > mid)
    }

    @Test
    fun `yedi gunluk seri ek serbest puan verir ve sinif artisi kademeyle carpilir`() {
        assertEquals(1, Progression.dailyReward(6))
        assertEquals(2, Progression.dailyReward(7))
        assertEquals(2, Progression.dailyReward(14))
        assertEquals(1, Progression.growthMultiplier(Grade.F))
        assertEquals(2, Progression.growthMultiplier(Grade.E))
        assertEquals(7, Progression.growthMultiplier(Grade.S))
    }

    @Test
    fun `prologda yanki bagi kurulur`() {
        val s = fresh()
        val choice = engine.view(s, lang).choices.first().id
        // İskeleden sonra aktarım sırasında Yankı'yla bağ sahnesi gelir; bağ orada kurulur.
        val transfer = engine.choose(s, choice, lang).state
        assertEquals("giris_yanki", transfer.sceneId)
        assertTrue(engine.view(transfer, lang).current.notices.any { it.kind == com.yukselis.engine.story.NoticeKind.ECHO })
        val next = engine.choose(transfer, "sor", lang).state
        assertTrue(next.hasFlag("yanki_bagi"))
        assertTrue(engine.view(s, lang).current.notices.any { it.kind == com.yukselis.engine.story.NoticeKind.ECHO })
    }

    // ---- Bağımsız inceleme düzeltmeleri ----

    @Test
    fun `kaynastirma girdileri artik yoksa reddedilir`() {
        var s = apply(fresh(), Effect.GrantSkill("alev_oku"), Effect.GrantSkill("ruzgar_adimi"), Effect.AddVar("hareketli_atis", 100))
        s = s.copy(player = s.player.copy(skills = s.player.skills.map { it.copy(level = 10) }), prompts = emptyList())
        s = apply(s, Effect.AddVar("x", 0))
        val fusion = s.prompts.first { it.kind == PromptKind.FUSION }
        // Önce yetenek evrimi: alev_oku gider.
        s = s.copy(prompts = listOf(s.prompts.first { it.kind == PromptKind.SKILL_EVOLUTION && it.subject == "alev_oku" }, fusion))
        s = engine.resolvePrompt(s, content.skills.getValue("alev_oku").evolutions.first(), lang).state
        try {
            engine.resolvePrompt(s, PromptKind.ACCEPT, lang)
            fail("Girdisi kalmayan kaynaştırma reddedilmeli")
        } catch (_: ActionRejected) {
        }
    }

    @Test
    fun `yetenek evrimi ertelenebilir ve kademe evriminde yeniden sorulur`() {
        var s = apply(fresh(), Effect.GrantSkill("golge_adimi"), Effect.SkillXp("golge_adimi", 1000))
        s = s.copy(prompts = s.prompts.filter { it.kind == PromptKind.SKILL_EVOLUTION })
        s = engine.resolvePrompt(s, PromptKind.DECLINE, lang).state
        s = apply(s, Effect.AddVar("x", 0))
        assertTrue(s.prompts.none { it.kind == PromptKind.SKILL_EVOLUTION })
        assertEquals(10, s.player.skill("golge_adimi")!!.level)
    }

    @Test
    fun `degisen istem eski karttan gelen yanitla cozulmez`() {
        var s = apply(fresh(), Effect.GainXp(xpForLevel(10)))
        val first = s.prompts.first()
        s = engine.resolvePrompt(s, first.options.first(), lang, first.kind, first.subject).state
        val next = s.prompts.firstOrNull() ?: return
        try {
            engine.resolvePrompt(s, next.options.first(), lang, first.kind, first.subject)
            fail("Eski istemin türüyle yeni istem çözülmemeli")
        } catch (_: ActionRejected) {
        }
    }

    @Test
    fun `yedi gunluk seri unvan verir ve gun atlamak seriyi koparir`() {
        var s = fresh().copy(dailyStreak = 6)
        val q = s.daily!!
        s = apply(s, Effect.DailyProgress(content.dailyTemplates.getValue(q.templateId).kind, q.steps))
        assertEquals(7, s.dailyStreak)
        assertTrue("disiplinli" in s.player.titles)
        assertEquals(2, s.player.freePoints)
        s = apply(s, Effect.AdvanceDays(3))
        assertEquals(0, s.dailyStreak)
    }

    @Test
    fun `sistem olay unvanlari verilir`() {
        var s = apply(fresh(), Effect.ShiftAffinity(Affinity.MELEE, 5), Effect.GainXp(xpForLevel(10)))
        s = s.copy(prompts = s.prompts.filter { it.kind == PromptKind.CLASS_OFFER })
        s = engine.resolvePrompt(s, "yakin_muharip", lang).state
        assertTrue("ilk_siniflanan" in s.player.titles)
    }

    @Test
    fun `gunluk sablon kosulu kesif ve kapi gorevleri simdilik cikmaz`() {
        repeat(30) { i ->
            val s = engine.newGame("T", GameMode.NORMAL, seed = i.toLong())
            assertEquals("antrenman", content.dailyTemplates.getValue(s.daily!!.templateId).kind)
        }
    }

    @Test
    fun `sanitize eski icerige isaret eden istemleri atar ve gunluk uretir`() {
        val broken = fresh().copy(
            daily = null,
            prompts = listOf(com.yukselis.engine.core.SystemPrompt(PromptKind.SKILL_EVOLUTION, listOf("yok_boyle"), "yok")),
        )
        val s = engine.sanitize(broken)
        assertTrue(s.prompts.isEmpty())
        assertNotNull(s.daily)
    }

    private fun <T> assertNotNullAndGet(v: T?): T {
        assertNotNull(v)
        return v!!
    }
}
