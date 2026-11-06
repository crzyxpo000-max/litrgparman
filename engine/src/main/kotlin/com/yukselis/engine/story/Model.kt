package com.yukselis.engine.story

import com.yukselis.engine.combat.AwakeningDef
import com.yukselis.engine.combat.CombatSpec
import com.yukselis.engine.combat.EnemyDef
import com.yukselis.engine.core.MoralAxis
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.NameRegistry
import com.yukselis.engine.system.Affinity
import com.yukselis.engine.system.ClassDef
import com.yukselis.engine.system.DailyTemplate
import com.yukselis.engine.system.EssenceDef
import com.yukselis.engine.system.FusionRecipe
import com.yukselis.engine.system.ItemDef
import com.yukselis.engine.system.ProfessionDef
import com.yukselis.engine.system.Rarity
import com.yukselis.engine.system.RecipeDef
import com.yukselis.engine.system.SkillDef
import com.yukselis.engine.system.Stat

/** Sistem bildirimi türleri; arayüz her türe ayrı renk ve ikon verir. */
enum class NoticeKind(val key: String) {
    SYSTEM("sistem"),
    STATUS("durum"),
    ANALYZE("analiz"),
    LEVEL_UP("seviye"),
    TITLE("unvan"),
    QUEST("gorev"),
    WARNING("uyari"),
    SKILL("yetenek"),
    EVOLUTION("evrim"),
    CLASS("sinif"),
    /** Yankı'nın bozuk, yalnızca oyuncuya görünen bildirimleri (glitchli yazı). */
    ECHO("yanki"),
    /** NPC'lerin sesi: yoldaş görüşü, güce tepki, ilişki değişimi. */
    COMPANION("yoldas");

    companion object {
        fun fromKey(key: String?): NoticeKind = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** Çözülmemiş (iki dilli) bildirim. */
data class Notice(
    val kind: NoticeKind,
    val title: LText,
    val lines: List<LText> = emptyList(),
    val rarity: Rarity? = null,
)

/** Güvenli koşul dili; betik çalıştırma yoktur. */
sealed interface Condition {
    fun test(state: WorldState): Boolean

    data class StatAtLeast(val stat: Stat, val value: Int) : Condition {
        override fun test(state: WorldState) = state.player.stats[stat] >= value
    }

    data class HasFlag(val flag: String) : Condition {
        override fun test(state: WorldState) = state.hasFlag(flag)
    }

    data class LacksFlag(val flag: String) : Condition {
        override fun test(state: WorldState) = !state.hasFlag(flag)
    }

    data class VarAtLeast(val key: String, val value: Int) : Condition {
        override fun test(state: WorldState) = state.variable(key) >= value
    }

    data class VarAtMost(val key: String, val value: Int) : Condition {
        override fun test(state: WorldState) = state.variable(key) <= value
    }

    data class MoralityAtLeast(val axis: MoralAxis, val value: Int) : Condition {
        override fun test(state: WorldState) = state.morality[axis] >= value
    }

    data class MoralityAtMost(val axis: MoralAxis, val value: Int) : Condition {
        override fun test(state: WorldState) = state.morality[axis] <= value
    }

    data class LevelAtLeast(val value: Int) : Condition {
        override fun test(state: WorldState) = state.player.level >= value
    }

    data class AffinityAtLeast(val affinity: Affinity, val value: Int) : Condition {
        override fun test(state: WorldState) = state.player.affinity[affinity] >= value
    }

    data class HasSkill(val skillId: String) : Condition {
        override fun test(state: WorldState) = state.player.skill(skillId) != null
    }

    data class HasTitle(val titleId: String) : Condition {
        override fun test(state: WorldState) = titleId in state.player.titles
    }

    data class HasClass(val classId: String) : Condition {
        override fun test(state: WorldState) = state.player.classId == classId
    }

    data class HasItem(val itemId: String, val count: Int) : Condition {
        override fun test(state: WorldState) = state.player.itemCount(itemId) >= count
    }

    data class ReputationAtLeast(val faction: String, val value: Int) : Condition {
        override fun test(state: WorldState) = (state.reputation[faction] ?: 0) >= value
    }

    data class AttentionAtLeast(val value: Int) : Condition {
        override fun test(state: WorldState) = state.attention >= value
    }

    data class AttentionAtMost(val value: Int) : Condition {
        override fun test(state: WorldState) = state.attention <= value
    }

    data class CorruptionAtLeast(val value: Int) : Condition {
        override fun test(state: WorldState) = state.corruption >= value
    }

    /** Gerçek seviye − görünen seviye ≥ n ("beni hafife aldılar"). */
    data class HiddenGapAtLeast(val value: Int) : Condition {
        override fun test(state: WorldState) = state.player.level - state.publicLevel >= value
    }

    data class InRegion(val region: String) : Condition {
        override fun test(state: WorldState) = state.region == region
    }

    data class InAct(val act: Int) : Condition {
        override fun test(state: WorldState) = state.act == act
    }

    data class DayAtLeast(val day: Int) : Condition {
        override fun test(state: WorldState) = state.day >= day
    }

    data class DayAtMost(val day: Int) : Condition {
        override fun test(state: WorldState) = state.day <= day
    }

    data class QuestIs(val quest: String, val status: com.yukselis.engine.world.QuestStatus) : Condition {
        override fun test(state: WorldState) = state.quests[quest] == status
    }

    data class ReputationAtMost(val faction: String, val value: Int) : Condition {
        override fun test(state: WorldState) = (state.reputation[faction] ?: 0) <= value
    }

    // ---- Aşama 5: NPC'ler. NPC kimliği "@" ise olayın odak NPC'sidir. ----

    data class NpcAlive(val npc: String) : Condition {
        override fun test(state: WorldState) = state.npc(npc)?.alive == true
    }

    data class NpcDead(val npc: String) : Condition {
        override fun test(state: WorldState) = state.npc(npc)?.alive == false
    }

    /** NPC gruptan ve sahneden çıktı (kaçtı, ayrıldı): yaşıyor ama "gitti". */
    data class NpcGoneCond(val npc: String) : Condition {
        override fun test(state: WorldState) = state.npc(npc)?.status == com.yukselis.engine.npc.NpcStatus.GONE
    }

    data class IsCompanion(val npc: String) : Condition {
        override fun test(state: WorldState) = state.npc(npc)?.let { it.companion && it.alive } == true
    }

    data class RelationAtLeast(val npc: String, val axis: com.yukselis.engine.npc.RelAxis, val value: Int) : Condition {
        override fun test(state: WorldState) = (state.npc(npc)?.relation?.get(axis) ?: -999) >= value
    }

    data class RelationAtMost(val npc: String, val axis: com.yukselis.engine.npc.RelAxis, val value: Int) : Condition {
        override fun test(state: WorldState) = (state.npc(npc)?.relation?.get(axis) ?: 999) <= value
    }

    data class NpcInFaction(val npc: String, val faction: String) : Condition {
        override fun test(state: WorldState) = state.npc(npc)?.faction == faction
    }

    data class NpcMarked(val npc: String, val mark: String) : Condition {
        override fun test(state: WorldState) = state.npc(npc)?.marks?.contains(mark) == true
    }

    data class NpcLevelAtLeast(val npc: String, val value: Int) : Condition {
        override fun test(state: WorldState) = (state.npc(npc)?.level ?: 0) >= value
    }

    data class Remembers(val npc: String, val memory: String) : Condition {
        override fun test(state: WorldState) = state.npc(npc)?.remembers(memory) == true
    }

    /** NPC'nin savaş bloğu var (düello/ihanet savaşı açılabilir). */
    data class NpcCanFight(val npc: String) : Condition {
        override fun test(state: WorldState) =
            state.npc(npc)?.let { it.alive && com.yukselis.engine.npc.NpcEngine.MARK_FIGHTER in it.marks } == true
    }

    data class Knows(val knowledge: String) : Condition {
        override fun test(state: WorldState) = knowledge in state.knowledge
    }

    /** Yoldaş kapasitesinde yer var. */
    data object PartyRoom : Condition {
        override fun test(state: WorldState) =
            state.npcs.values.count { it.companion && it.alive } <
                com.yukselis.engine.system.Progression.companionCapacity(state.player.stats, state.player.grade)
    }

    data class PublicLevelAtLeast(val value: Int) : Condition {
        override fun test(state: WorldState) = state.publicLevel >= value
    }

    data class PublicLevelAtMost(val value: Int) : Condition {
        override fun test(state: WorldState) = state.publicLevel <= value
    }

    data class HasTrait(val trait: String) : Condition {
        override fun test(state: WorldState) = trait in state.player.traits
    }

    data class All(val conditions: List<Condition>) : Condition {
        override fun test(state: WorldState) = conditions.all { it.test(state) }
    }
}

/** Dünya durumunu değiştirmenin tek yolu. */
sealed interface Effect {
    data class SetFlag(val flag: String) : Effect
    data class ClearFlag(val flag: String) : Effect
    data class AddVar(val key: String, val delta: Int) : Effect
    data class ShiftMorality(val axis: MoralAxis, val delta: Int) : Effect
    data class ShiftReputation(val faction: String, val delta: Int) : Effect
    data class GainXp(val amount: Long) : Effect
    data class AdvanceDays(val days: Int) : Effect
    data class AddStat(val stat: Stat, val delta: Int) : Effect
    data class GrantTitle(val titleId: String) : Effect
    data class ShiftAffinity(val affinity: Affinity, val delta: Int) : Effect
    data class GrantSkill(val skillId: String) : Effect
    data class SkillXp(val skillId: String, val amount: Int) : Effect
    data class AddItem(val itemId: String, val count: Int) : Effect
    data class AddCredits(val amount: Int) : Effect
    data class AddCrystals(val amount: Int) : Effect
    data class EssenceInsight(val essenceId: String, val amount: Int) : Effect
    /** Aynı türdeki (antrenman, kesif, savas, kapi) Günlük Görevi ilerletir. */
    data class DailyProgress(val kind: String, val amount: Int) : Effect
    /** Can/mana değişimi. Savaş dışında can 1'in altına düşmez (savaşta ölüm Aşama 3). */
    data class AddHp(val delta: Int) : Effect
    data class AddMp(val delta: Int) : Effect
    /** Ertelenmiş sonuç: `min..max` gün sonra, koşul (ham metin) hâlâ doğruysa olay tetiklenir. */
    data class PlantSeed(val eventId: String, val minDelay: Int, val maxDelay: Int, val condition: String?, val npc: String? = null) : Effect
    data class ShiftAttention(val delta: Int) : Effect
    data class ShiftCorruption(val delta: Int) : Effect
    /** [Açığa vur]: görünen seviye gerçek seviyeye çekilir; aradaki fark kadar Dikkat artar. */
    data object Reveal : Effect
    /** Tanıklı başarı: söylenti, fraksiyon tepkileri, eşikte unvan. */
    data class Feat(val featId: String) : Effect
    data class SetQuest(val questId: String, val status: com.yukselis.engine.world.QuestStatus) : Effect
    /** Bölge değiştirir (olay içinden yolculuk). */
    data class MoveTo(val region: String) : Effect
    data class ShiftDanger(val region: String, val delta: Int) : Effect
    /** Bölgenin kontrolü bir fraksiyona (ya da "canavar") geçer. */
    data class SetControl(val region: String, val control: String) : Effect
    /** Savaş başlatır; seçenek `sonraki` yerine savaşın kazan/kaybet/kaç sahnelerine gider. */
    data class StartCombat(val spec: CombatSpec) : Effect
    /** Kendini açan arayüz: bir sekmeyi verir (yetenekler, canta, harita). */
    data class UnlockTab(val tab: String) : Effect
    /** Meslek seçimi istemini açar. */
    data object OfferProfession : Effect

    // ---- Aşama 5: NPC'ler ("@" = olayın odak NPC'si) ----
    data class ShiftRelation(val npc: String, val deltas: Map<com.yukselis.engine.npc.RelAxis, Int>) : Effect
    data class AddMemory(val npc: String, val memory: String, val weight: Int) : Effect
    data class KillNpc(val npc: String) : Effect
    data class JoinParty(val npc: String) : Effect
    data class LeaveParty(val npc: String) : Effect
    /** NPC gruptan ve sahneden çıkar (kaçtı, ayrıldı): durumu "gitti", eşikleri kapanır. */
    data class NpcGone(val npc: String) : Effect
    data class SetNpcFaction(val npc: String, val faction: String) : Effect
    data class MeetNpc(val npc: String) : Effect
    data class NpcLevel(val npc: String, val delta: Int) : Effect
    data class NpcTrait(val npc: String, val trait: String) : Effect
    data class NpcMark(val npc: String, val mark: String) : Effect
    data class PlayerTrait(val trait: String) : Effect
    data class Learn(val knowledge: String) : Effect
    /** NPC'ye yalan: hafızaya girer; şüpheciliğe göre bir gün ortaya çıkar. */
    data class TellLie(val npc: String, val topic: String) : Effect
    data class PersonalQuest(val npc: String, val status: com.yukselis.engine.world.QuestStatus) : Effect
    /** NPC'nin kişisel görev ödülü (ortak yetenek ya da unvan). */
    data class NpcReward(val npc: String) : Effect
    data class AddArmy(val unit: String, val count: Int) : Effect
    /** Sınıfa/eğilime uygun birlikten n tane. */
    data class GrowArmy(val count: Int) : Effect
}

data class Paragraph(val text: LText, val condition: Condition? = null)

data class SceneNotice(val notice: Notice, val condition: Condition? = null)

/**
 * Pasif Sistem sezgisi: koşulu (çoğunlukla yüksek stat) sağlanınca sahneye yalnızca oyuncunun
 * okuduğu özel bir satır eklenir. Stat koşulunda satırın başına "[ALG]" gibi önek gelir.
 */
data class Insight(val text: LText, val condition: Condition)

/** Sahneye yazılmış yoldaş görüşü; NPC yanındaysa genel görüş satırının yerine geçer. */
data class CompanionLine(val npc: String, val text: LText, val condition: Condition? = null)

data class ChoiceDef(
    val id: String,
    val label: LText,
    /** Karşılanmazsa seçenek görünür ama kilitlidir ("[ALG 25]"). */
    val requires: Condition? = null,
    /** Karşılanmazsa seçenek hiç gösterilmez. */
    val showWhen: Condition? = null,
    val effects: List<Effect> = emptyList(),
    val next: String? = null,
    /** OP MC: [Gizle] / [Açığa vur] / gösteriş seçeneği. */
    val kind: com.yukselis.engine.world.ChoiceKind = com.yukselis.engine.world.ChoiceKind.NORMAL,
    /** Görünür olasılıklı zar kontrolü; başarısızlıkta `failNext` ve `failEffects`. */
    val check: com.yukselis.engine.world.DiceCheck? = null,
)

data class Scene(
    val id: String,
    val place: String?,
    val paragraphs: List<Paragraph>,
    val notices: List<SceneNotice> = emptyList(),
    val choices: List<ChoiceDef> = emptyList(),
    val isEnding: Boolean = false,
    val insights: List<Insight> = emptyList(),
    val companionLines: List<CompanionLine> = emptyList(),
)

data class TitleDef(
    val id: String,
    val name: LText,
    val description: LText,
    val rarity: Rarity,
    val statBonus: Map<Stat, Int> = emptyMap(),
    /** "İlk" unvanı: dünyada yalnızca bir sahibi olur. */
    val first: Boolean = false,
    /** Fraksiyon itibarı tavanı (Kardeş Katili: Sığınak en fazla −50). */
    val reputationCap: Map<String, Int> = emptyMap(),
)

/** Yüklenmiş oyun içeriğinin tamamı. */
data class Content(
    val scenes: Map<String, Scene>,
    val names: NameRegistry,
    val titles: Map<String, TitleDef>,
    val startScene: String,
    val classes: Map<String, ClassDef> = emptyMap(),
    val skills: Map<String, SkillDef> = emptyMap(),
    val fusions: List<FusionRecipe> = emptyList(),
    val items: Map<String, ItemDef> = emptyMap(),
    val professions: Map<String, ProfessionDef> = emptyMap(),
    val recipes: Map<String, RecipeDef> = emptyMap(),
    val essences: Map<String, EssenceDef> = emptyMap(),
    val dailyTemplates: Map<String, DailyTemplate> = emptyMap(),
    val texts: SystemTexts = SystemTexts.EMPTY,
    val enemies: Map<String, EnemyDef> = emptyMap(),
    val awakenings: List<AwakeningDef> = emptyList(),
    val events: Map<String, com.yukselis.engine.world.EventDef> = emptyMap(),
    val regions: Map<String, com.yukselis.engine.world.RegionDef> = emptyMap(),
    val factions: Map<String, com.yukselis.engine.world.FactionDef> = emptyMap(),
    val feats: Map<String, com.yukselis.engine.world.FeatDef> = emptyMap(),
    val quests: Map<String, com.yukselis.engine.world.QuestDef> = emptyMap(),
    /** Bayrak kaydı: kimlik → açıklama (geliştirici için). */
    val flags: Map<String, String> = emptyMap(),
    /** Gün akışının başladığı bölge. */
    val startRegion: String? = null,
    // ---- Aşama 5 ----
    val npcs: Map<String, com.yukselis.engine.npc.NpcDef> = emptyMap(),
    val traits: Map<String, com.yukselis.engine.npc.TraitDef> = emptyMap(),
    val knowledge: Map<String, com.yukselis.engine.npc.KnowledgeDef> = emptyMap(),
    val army: Map<String, com.yukselis.engine.npc.ArmyUnitDef> = emptyMap(),
    val ranking: com.yukselis.engine.npc.RankingDef = com.yukselis.engine.npc.RankingDef(),
    /** Fraksiyonların ahlak profili (NPC fraksiyon değiştirirken en yakını seçilir). */
    val factionValues: Map<String, com.yukselis.engine.core.Morality> = emptyMap(),
) {
    companion object {
        /** Kesinti sahnesinden önceki sahneye dönüş. */
        const val RETURN = "@geri"

        /** Günlük Görev cezası kancası: bu kimlikte bir sahne varsa oraya ışınlanılır. */
        const val PENALTY_SCENE = "ceza_bolgesi"

        /** Demir Mod ölümü bu sahneye (Hasat sonu) bağlanır. */
        const val HARVEST_SCENE = "son_hasat"

        /** Olayın odak NPC'si (eşik olayları, genel sahneler). */
        const val FOCUS = "@"
    }
}
