package com.yukselis.engine.npc

import com.yukselis.engine.core.MoralAxis
import com.yukselis.engine.core.Morality
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.story.Condition
import com.yukselis.engine.story.Effect
import com.yukselis.engine.system.Affinity
import com.yukselis.engine.system.Stat
import com.yukselis.engine.system.Stats

// ---------------------------------------------------------------- kişilik ve ilişki

/** GDD: 5 kişilik özelliği, 0–10. */
enum class Trait(val key: String) {
    COURAGE("cesaret"), AMBITION("hirs"), CONSCIENCE("vicdan"), LOYALTY("sadakat"), SKEPTICISM("supheci");

    companion object {
        fun fromKey(key: String?): Trait? = entries.firstOrNull { it.key == key }
    }
}

data class Personality(val values: Map<Trait, Int> = Trait.entries.associateWith { 5 }) {
    operator fun get(t: Trait): Int = (values[t] ?: 5).coerceIn(0, 10)

    fun plus(deltas: Map<Trait, Int>): Personality =
        Personality(Trait.entries.associateWith { t -> (get(t) + (deltas[t] ?: 0)).coerceIn(0, 10) })
}

/** GDD: dört ilişki ekseni, −100…+100. */
enum class RelAxis(val key: String) {
    TRUST("guven"), RESPECT("saygi"), FEAR("korku"), BOND("baglilik");

    companion object {
        fun fromKey(key: String?): RelAxis? = entries.firstOrNull { it.key == key }
    }
}

data class Relation(val trust: Int = 0, val respect: Int = 0, val fear: Int = 0, val bond: Int = 0) {
    operator fun get(a: RelAxis): Int = when (a) {
        RelAxis.TRUST -> trust
        RelAxis.RESPECT -> respect
        RelAxis.FEAR -> fear
        RelAxis.BOND -> bond
    }

    fun shift(a: RelAxis, delta: Int): Relation {
        val v = (get(a) + delta).coerceIn(-100, 100)
        return when (a) {
            RelAxis.TRUST -> copy(trust = v)
            RelAxis.RESPECT -> copy(respect = v)
            RelAxis.FEAR -> copy(fear = v)
            RelAxis.BOND -> copy(bond = v)
        }
    }

    fun plus(deltas: Map<RelAxis, Int>): Relation = deltas.entries.fold(this) { r, (a, d) -> r.shift(a, d) }

    operator fun plus(other: Relation): Relation = plus(RelAxis.entries.associateWith { other[it] })
}

// ---------------------------------------------------------------- içerik tanımı

/** NPC'nin öncelikli hedefi: her gün bir adım ilerler; `gun` adımda tamamlanır ve etkileri uygulanır. */
data class GoalDef(
    val id: String,
    val priority: Int,
    val name: LText,
    val days: Int = 0,
    /** Hedef bu koşul tutmadığında "imkansızlaşmış" sayılır ve bırakılır. */
    val condition: Condition? = null,
    val effects: List<Effect> = emptyList(),
)

/** Yoldaş/düello savaş rolü. */
enum class CombatRole(val key: String) {
    STRIKER("saldiri"), GUARD("koruma"), HEALER("sifa"), RANGED("menzil");

    companion object {
        fun fromKey(key: String?): CombatRole = entries.firstOrNull { it.key == key } ?: STRIKER
    }
}

data class NpcCombat(
    /** Başlangıç seviyesindeki statlar. */
    val stats: Stats,
    /** Başlangıçtan sonraki her seviyede artış. */
    val growth: Map<Stat, Int>,
    val role: CombatRole,
    val attackStat: Stat,
    val attackName: LText,
    /** Yoldaşken düzyazı: "{npc} ... {dusman}". */
    val prose: LText,
    /** Düelloda sana saldırırken düzyazı: "{dusman} ..." (ikinci şahıs). */
    val duelProse: LText,
)

/** Rakip "İlk" unvanını bu seviyeye ulaşınca (oyuncu henüz almadıysa) kapar. */
data class FirstClaimDef(val title: String, val level: Int)

/** GDD eşik olayları. */
enum class ThresholdKind(val key: String) {
    BETRAYAL("ihanet"),
    PERSONAL("kisisel"),
    FLEE("kacis"),
    OBEY("itaat"),
    RIVAL("rakip"),
    DEPARTURE("ayrilik"),
    /** Söylenen bir yalan ortaya çıktı. */
    LIE("yalan");

    /** Genel olay kimliği; NPC'ye özel geçersiz kılma `esik_<tur>_<npc>`. */
    val genericEvent: String get() = "esik_$key"

    companion object {
        fun fromKey(key: String?): ThresholdKind? = entries.firstOrNull { it.key == key }
    }
}

/** OP MC: oyuncunun büyüyen gücüne tepki. */
enum class PowerReaction(val key: String) {
    ADMIRATION("hayranlik"), WORRY("endise"), ENVY("kiskanclik"), DEPENDENCE("bagimlilik");

    companion object {
        fun fromKey(key: String?): PowerReaction? = entries.firstOrNull { it.key == key }
    }
}

data class NpcDef(
    val id: String,
    /** İlişkiler ekranındaki kısa tanım. */
    val description: LText,
    val personality: Personality,
    /** Ahlak eksenlerinde durduğu yer (−100…+100). */
    val values: Morality,
    val startRelation: Relation,
    val goals: List<GoalDef>,
    val affinity: Map<Affinity, Int>,
    val startLevel: Int,
    /** Büyüme hızı (1 = sıradan insan). */
    val growthRate: Double,
    val faction: String? = null,
    /** Yoldaş olabilir mi? */
    val canJoin: Boolean = false,
    /** Küresel Sıralama'da görünür mü? */
    val ranked: Boolean = false,
    /** Oyunun başından tanınıyor mu? */
    val knownAtStart: Boolean = false,
    /** Yoldaş olmadan da oyuncunun yanında sayıldığı koşul (Yankı: `yanki_bagi`). */
    val presentWhen: Condition? = null,
    val combat: NpcCombat? = null,
    val arcNodes: List<String> = emptyList(),
    val firstClaims: List<FirstClaimDef> = emptyList(),
    /** Yoldaş görüşü: "merhamet+", "duzen-" … → satırlar. */
    val opinions: Map<String, List<LText>> = emptyMap(),
    val powerLines: Map<PowerReaction, List<LText>> = emptyMap(),
    /** Bu NPC'ye özgü hatıra metinleri (kimlik → metin). */
    val memories: Map<String, LText> = emptyMap(),
    /** Kişisel görev ödülü (ortak yetenek ya da unvan). */
    val personalReward: List<Effect> = emptyList(),
) {
    companion object {
        fun opinionKey(axis: MoralAxis, positive: Boolean) = axis.key + if (positive) "+" else "-"
    }
}

/** İz: oyuncuda bedelli mutasyon/soy uyanışı, NPC'de kalıcı özellik ya da yara. */
data class TraitDef(
    val id: String,
    val name: LText,
    val description: LText,
    val onPlayer: Boolean,
    val statBonus: Map<Stat, Int> = emptyMap(),
    val statPenalty: Map<Stat, Int> = emptyMap(),
    val personality: Map<Trait, Int> = emptyMap(),
    val corruption: Int = 0,
)

/** Bilgi günlüğü kaydı: söylenti, sır ya da gerçek seviye. */
data class KnowledgeDef(val id: String, val name: LText, val description: LText, val kind: String)

/** Ordu birliği: sınıfa/eğilime göre gölge asker, canavar ya da lonca. */
data class ArmyUnitDef(val id: String, val name: LText, val description: LText, val affinities: List<Affinity>, val power: Int)

/** Küresel Sıralama'nın içerik tarafı: prosedürel avcılar ve sıraya fraksiyon tepkileri. */
data class RankingDef(
    val firstNames: List<LText> = emptyList(),
    val lastNames: List<LText> = emptyList(),
    val hunters: Int = 0,
    val levelMin: Int = 1,
    val levelMax: Int = 3,
    val rateMin: Double = 0.6,
    val rateMax: Double = 1.2,
    val reactions: List<RankReaction> = emptyList(),
)

/** Oyuncunun görünen sırası ilk kez `rank` ya da üstüne çıktığında bir kez uygulanır. */
data class RankReaction(val rank: Int, val effects: List<Effect>)

// ---------------------------------------------------------------- durum

/** Hatıra: duygusal ağırlıkla; küçükler solar, |ağırlık| ≥ PERMANENT kalıcıdır. */
data class Memory(val id: String, val day: Int, val weight: Int) {
    val permanent: Boolean get() = kotlin.math.abs(weight) >= PERMANENT

    companion object {
        const val PERMANENT = 40
        const val LIMIT = 30

        /** Olaydan doğan hatıra: metni olayın kart başlığıdır. */
        const val EVENT_PREFIX = "olay:"

        /** Söylenen (henüz ortaya çıkmamış) yalan. */
        const val LIE_PREFIX = "yalan:"

        /** Ortaya çıkmış yalan. */
        const val LIE_REVEALED_PREFIX = "yalan_acik:"
    }
}

enum class NpcStatus(val key: String) {
    ACTIVE("aktif"), DEAD("olu"), GONE("gitti");

    companion object {
        fun fromKey(key: String?): NpcStatus = entries.firstOrNull { it.key == key } ?: ACTIVE
    }
}

data class NpcState(
    val id: String,
    val status: NpcStatus = NpcStatus.ACTIVE,
    val relation: Relation = Relation(),
    /** 0: henüz içerikten kurulmadı (eski kayıt ya da yeni oyun). */
    val level: Int = 0,
    /** Bir sonraki seviyeye ilerleme (yüzde biri). */
    val progress: Int = 0,
    val faction: String? = null,
    val companion: Boolean = false,
    val known: Boolean = false,
    val memories: List<Memory> = emptyList(),
    val goal: String? = null,
    val goalProgress: Int = 0,
    val doneGoals: Set<String> = emptySet(),
    /** Eşik ve durum işaretleri: "esik_ihanet", "rakip", "itaatkar", "hain", "ihmal" … */
    val marks: Set<String> = emptySet(),
    val traits: List<String> = emptyList(),
    val lastContactDay: Int = 0,
    val lastPowerDay: Int = -100,
) {
    val alive: Boolean get() = status != NpcStatus.DEAD
    val initialized: Boolean get() = level > 0

    fun remembers(memoryId: String) = memories.any { it.id == memoryId }
}

/** Prosedürel avcı (yalnızca Küresel Sıralama'da yaşar). */
data class Hunter(val uid: Int, val first: Int, val last: Int, val level: Int, val progress: Int, val rate: Double)
