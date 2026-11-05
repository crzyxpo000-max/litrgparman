package com.yukselis.engine.core

import com.yukselis.engine.combat.CombatState
import com.yukselis.engine.combat.EnemyMemory
import com.yukselis.engine.system.AffinityVector
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Stats

enum class GameMode { NORMAL, IRONMAN }

/** Üç ahlak ekseni, her biri −100…+100. Pozitif uç: Merhamet, Düzen, İnsanlık. */
enum class MoralAxis(val key: String) {
    MERCY("merhamet"),
    ORDER("duzen"),
    HUMANITY("insanlik");

    companion object {
        fun fromKey(key: String): MoralAxis? = entries.firstOrNull { it.key == key }
    }
}

data class Morality(
    val mercy: Int = 0,
    val order: Int = 0,
    val humanity: Int = 0,
) {
    operator fun get(axis: MoralAxis): Int = when (axis) {
        MoralAxis.MERCY -> mercy
        MoralAxis.ORDER -> order
        MoralAxis.HUMANITY -> humanity
    }

    fun shift(axis: MoralAxis, delta: Int): Morality {
        val value = (get(axis) + delta).coerceIn(-100, 100)
        return when (axis) {
            MoralAxis.MERCY -> copy(mercy = value)
            MoralAxis.ORDER -> copy(order = value)
            MoralAxis.HUMANITY -> copy(humanity = value)
        }
    }
}

/** Oyuncunun sahip olduğu bir yetenek; seviye 1–10, kullanım XP'si. */
data class SkillState(val id: String, val level: Int = 1, val xp: Int = 0)

data class ProfessionState(val id: String, val level: Int = 1, val xp: Int = 0)

data class PlayerState(
    val level: Int = 1,
    val xp: Long = 0,
    val freePoints: Int = 0,
    val stats: Stats = Stats(),
    val hp: Int = Progression.maxHp(stats, level),
    val mp: Int = Progression.maxMp(stats),
    val titles: List<String> = emptyList(),
    /** Gizli Eğilim vektörü; arayüzde gösterilmez. */
    val affinity: AffinityVector = AffinityVector(),
    val classId: String? = null,
    /** Evrimi tamamlanmış en yüksek kademe. Seviye bu kademenin dışına çıkamaz. */
    val unlockedGrade: Grade = Grade.F,
    val skills: List<SkillState> = emptyList(),
    val inventory: Map<String, Int> = emptyMap(),
    val credits: Int = 0,
    val essenceCrystals: Int = 0,
    val profession: ProfessionState? = null,
    /** Öz kavrayışı (kimlik → 0…100); 100'e ulaşan ve yuvası olan Öz bağlanabilir. */
    val essenceInsight: Map<String, Int> = emptyMap(),
    val essences: List<String> = emptyList(),
    /** A kademesinde iki Öz'den oluşan Yol (içerik Aşama 7). */
    val path: String? = null,
) {
    val maxHp: Int get() = Progression.maxHp(stats, level)
    val maxMp: Int get() = Progression.maxMp(stats)
    val grade: Grade get() = Grade.forLevel(level)

    fun skill(id: String): SkillState? = skills.firstOrNull { it.id == id }

    fun itemCount(id: String): Int = inventory[id] ?: 0
}

/**
 * Motorun ürettiği, oyuncunun karar vermesi gereken Sistem istemi: sınıf teklifi, evrim,
 * meslek seçimi, yetenek evrimi, kaynaştırma, Öz bağlama. Kuyruktadır; ilki çözülmeden
 * hikaye seçimi yapılamaz. Yalnızca kimlik tutar, kayda girer.
 */
data class SystemPrompt(
    val kind: PromptKind,
    val options: List<String>,
    /** Evrim için kademe, yetenek evrimi için yetenek, kaynaştırma için tarif, Öz için Öz kimliği. */
    val subject: String? = null,
)

enum class PromptKind(val key: String) {
    CLASS_OFFER("sinif_teklifi"),
    EVOLUTION("evrim"),
    PROFESSION("meslek"),
    SKILL_EVOLUTION("yetenek_evrimi"),
    FUSION("kaynastirma"),
    ESSENCE_BIND("oz_baglama");

    companion object {
        fun fromKey(key: String?): PromptKind? = entries.firstOrNull { it.key == key }
        const val ACCEPT = "kabul"
        const val DECLINE = "red"
    }
}

/** O günün Günlük Görevi. */
data class DailyQuest(
    val templateId: String,
    val day: Int,
    val progress: Int = 0,
    val steps: Int,
    val completed: Boolean = false,
)

/** Oyuncunun geçtiği sahne ve yaptığı seçim; hikaye akışını yeniden çizmek için kullanılır. */
data class HistoryEntry(val sceneId: String, val choiceId: String)

/**
 * Tek doğruluk kaynağı. Değişmezdir: her değişiklik yeni bir kopya üretir.
 * Kayıt dosyası bu sınıfın tamamını tutar; dil bilgisi içermez.
 */
data class WorldState(
    val seed: Long,
    val rngState: Long,
    val playerName: String,
    val mode: GameMode,
    val sceneId: String,
    val day: Int = 0,
    val player: PlayerState = PlayerState(),
    val flags: Set<String> = emptySet(),
    val vars: Map<String, Int> = emptyMap(),
    val reputation: Map<String, Int> = emptyMap(),
    val morality: Morality = Morality(),
    val history: List<HistoryEntry> = emptyList(),
    val ended: Boolean = false,
    /** "İlk" unvanlarının dünya kaydı: unvan → sahibi ("oyuncu" ya da NPC kimliği). */
    val firstClaims: Map<String, String> = emptyMap(),
    val prompts: List<SystemPrompt> = emptyList(),
    val daily: DailyQuest? = null,
    /** Art arda tamamlanan Günlük Görev sayısı. */
    val dailyStreak: Int = 0,
    /** Kesinti sahnelerinden (Ceza Bölgesi gibi) "@geri" ile dönülecek sahneler. */
    val returnStack: List<String> = emptyList(),
    /** Günlük Görev ihmal edildi; bir sonraki geçişte Ceza Bölgesi'ne ışınlanma. */
    val penaltyPending: Boolean = false,
    /** Kendini açan arayüz: Sistem'in verdiği sekmeler. Hikaye, Durum ve Daha fazla her zaman açıktır. */
    val unlockedTabs: Set<String> = emptySet(),
    /** Süren savaş; kayda girer, yüklenince aynı yerden devam edilir. */
    val combat: CombatState? = null,
    /** Kaçan ya da bağışlanan zeki düşmanlar (düşman kimliği → hafıza). */
    val enemyMemory: Map<String, EnemyMemory> = emptyMap(),
    /** İkinci Şans'ın kullanıldığı kademeler (her kademede bir kez). */
    val secondChances: Set<Grade> = emptySet(),
    /** Normal modda ölüm: arayüz son kayıt noktasına dönmeyi önerir. */
    val dead: Boolean = false,
) {
    val act: Int get() = GameClock.actFor(day)

    fun hasFlag(flag: String): Boolean = flag in flags

    fun variable(key: String): Int = vars[key] ?: 0

    companion object {
        const val HISTORY_LIMIT = 12
        const val PLAYER_CLAIM = "oyuncu"
        const val TAB_SKILLS = "yetenekler"
        const val TAB_BAG = "canta"
        const val TAB_MAP = "harita"
        val TABS = listOf(TAB_SKILLS, TAB_BAG, TAB_MAP)

        /** Benzersiz yetenekler de dünyada tek sahiplidir; "İlk" kaydında bu önekle tutulur. */
        const val UNIQUE_CLAIM_PREFIX = "yetenek:"

        /** Savaş başında yazılan kayıt noktası (Normal modda ölümden sonra dönülür). */
        const val CHECKPOINT_SLOT = "checkpoint"

        fun newGame(seed: Long, playerName: String, mode: GameMode, startScene: String): WorldState =
            WorldState(
                seed = seed,
                rngState = seed,
                playerName = playerName.trim().ifEmpty { "?" },
                mode = mode,
                sceneId = startScene,
            )
    }
}

/** Oyun takvimi: 365 gün, dört perde (GDD: Yapı → Perdeler). */
object GameClock {
    const val LAST_DAY = 365

    fun actFor(day: Int): Int = when {
        day <= 30 -> 1
        day <= 180 -> 2
        day <= 330 -> 3
        else -> 4
    }

    fun advance(state: WorldState, days: Int): WorldState =
        state.copy(day = (state.day + days).coerceIn(0, LAST_DAY))
}
