package com.yukselis.engine.combat

import com.yukselis.engine.i18n.LText
import com.yukselis.engine.story.Condition
import com.yukselis.engine.story.Effect
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Stat
import com.yukselis.engine.system.Stats
import com.yukselis.engine.system.StatusApplication

// ---- İçerik tanımları (content/enemies.json, content/awakenings.json) ----

/** GDD: düşman davranış kalıpları. */
enum class Behavior(val key: String) {
    AGGRESSIVE("agresif"), CAUTIOUS("temkinli"), PACK("suru"), FLEEING("kacan");

    companion object {
        fun fromKey(key: String?): Behavior = entries.firstOrNull { it.key == key } ?: AGGRESSIVE
    }
}

data class EnemyAttack(
    val id: String,
    val name: LText,
    val stat: Stat,
    val base: Int,
    val multiplier: Double,
    val tags: List<String> = emptyList(),
    val status: StatusApplication? = null,
    val cooldown: Int = 0,
    /** Düzyazı: "{dusman} dişlerini bacağına geçiriyor." */
    val prose: LText,
)

data class BossPhase(
    /** HP yüzdesi eşiği (70, 40, 15). */
    val threshold: Int,
    val text: LText,
    val buff: Map<Stat, Int> = emptyMap(),
    val attack: EnemyAttack? = null,
)

data class EnemyDef(
    val id: String,
    val name: LText,
    val description: LText,
    val hook: LText,
    val level: Int,
    val grade: Grade,
    val maxHp: Int,
    val stats: Stats,
    val behavior: Behavior,
    val intelligent: Boolean,
    val weakness: String?,
    val weaknessText: LText?,
    val attacks: List<EnemyAttack>,
    val xp: Long,
    val credits: Int = 0,
    val loot: Map<String, Double> = emptyMap(),
    val tags: List<String> = emptyList(),
    val boss: Boolean = false,
    val phases: List<BossPhase> = emptyList(),
)

/**
 * OP MC: uyanış tetikleyicisi. Savaş bittiğinde (ya da etkilerden sonra) koşulu sağlanırsa
 * bir kez çalışır: yazılmış bir kırılma anı + gizli yetenek / evrim yolu.
 * Koşullarda savaş bağlamı değişkenleri kullanılabilir (`_savas_*`, bkz. [CombatContext]).
 */
data class AwakeningDef(
    val id: String,
    val title: LText,
    val lines: List<LText>,
    val condition: Condition,
    val effects: List<Effect>,
)

// ---- Durum ----

enum class Stance(val key: String, val damage: Double, val incoming: Double, val staminaRegen: Int) {
    /** Tam saldırı: daha çok hasar verir, daha çok alır. */
    ALL_OUT("tam", 1.3, 1.25, 0),
    MEASURED("olculu", 1.0, 1.0, 5),
    /** Savunmacı: az verir, az alır, nefes toplar. */
    DEFENSIVE("savunma", 0.7, 0.6, 12);

    companion object {
        fun fromKey(key: String?): Stance = entries.firstOrNull { it.key == key || it.name == key } ?: MEASURED
    }
}

/**
 * Bir durum etkisinin süren örneği. `power`: kalkanda kalan emme, zehir/yanmada tur hasarı.
 * `fresh`: bu tur uygulandı; tur sonunda süresi düşmez (1 turluk sersemleme gerçekten bir turu alsın).
 */
data class StatusInstance(val effect: String, val turns: Int, val power: Int = 0, val fresh: Boolean = false)

data class EnemyState(
    val uid: Int,
    val id: String,
    val hp: Int,
    val maxHp: Int,
    val level: Int,
    val stats: Stats,
    val statuses: List<StatusInstance> = emptyList(),
    /** Geçilen boss fazı sayısı (0–3). */
    val phase: Int = 0,
    val cooldowns: Map<String, Int> = emptyMap(),
    val analyzed: Boolean = false,
    /** Temkinli düşman bu tur savunmada. */
    val guarding: Boolean = false,
    /** Faz saldırıları (boss). */
    val extraAttacks: List<String> = emptyList(),
    /** Teslim oldu / bağışlandı / kaçtı: savaştan çıktı ama ölmedi. */
    val gone: Boolean = false,
    /** Kendi isteğiyle kaçtı (XP ve zafer saymaz). */
    val fled: Boolean = false,
) {
    val alive: Boolean get() = hp > 0 && !gone
    fun has(effect: String) = statuses.any { it.effect == effect }
}

/** Savaş günlüğünün bir satırı: düzyazı ve isteğe bağlı sayı satırı. */
data class LogEntry(val prose: LText, val numbers: LText? = null)

/** Savaş sonrası gidilecek sahneler; `kaybet` yoksa Normal modda kayıt noktasına dönülür. */
data class CombatSpec(
    val enemies: List<String>,
    val environment: List<String> = emptyList(),
    val win: String,
    val lose: String? = null,
    val flee: String? = null,
)

data class CombatState(
    val spec: CombatSpec,
    val enemies: List<EnemyState>,
    val round: Int = 1,
    val stamina: Int,
    val maxStamina: Int,
    val stance: Stance = Stance.MEASURED,
    /** Dayanım taahhüdü 0–3: her kademe hasar ve isabeti artırır, Dayanım harcar. */
    val effort: Int = 0,
    val playerStatuses: List<StatusInstance> = emptyList(),
    val cooldowns: Map<String, Int> = emptyMap(),
    val defending: Boolean = false,
    /** Bu tur kullanılan küçük eylem (eşya). */
    val minorUsed: Boolean = false,
    /** Hızlanmayla kalan ek ana eylem. */
    val extraActions: Int = 0,
    val usedEnvironment: Set<String> = emptySet(),
    val log: List<LogEntry> = emptyList(),
    /** Son düşman düştü: oyuncu infaz biçimini seçmeli. */
    val pendingExecution: Int? = null,
    /** Bu savaşta ilk vuruşu alan düşmanlar (ilk vuruşta kritik öldürme sayacı için). */
    val struck: Set<Int> = emptySet(),
    val lowestHpPercent: Int = 100,
    val kills: Int = 0,
) {
    fun enemy(uid: Int): EnemyState? = enemies.firstOrNull { it.uid == uid }
    val living: List<EnemyState> get() = enemies.filter { it.alive }
    fun playerHas(effect: String) = playerStatuses.any { it.effect == effect }

    companion object {
        const val LOG_LIMIT = 40
        const val MAX_EFFORT = 3
    }
}

/** Kaçan zeki düşmanın hafızası: güçlenmiş olarak geri döner. */
data class EnemyMemory(val fled: Int = 0, val spared: Int = 0)

/** Durum etkileri (GDD: 8 etki). */
object StatusEffects {
    const val BLEED = "kanama"
    const val POISON = "zehir"
    const val BURN = "yanma"
    const val STUN = "sersemleme"
    const val FEAR = "korku"
    const val BLIND = "kor"
    const val HASTE = "hizlanma"
    const val SHIELD = "kalkan"
    val ALL = setOf(BLEED, POISON, BURN, STUN, FEAR, BLIND, HASTE, SHIELD)

    /** Olumlu etkiler kullanıcının kendisine uygulanır. */
    val SELF = setOf(HASTE, SHIELD)
}
