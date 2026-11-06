package com.yukselis.engine.combat

import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Stat
import com.yukselis.engine.system.Stats
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Ölçekli düşman başvurusu (Aşama 7): içerikte `"kanal_faresi@120"` yazmak, tanımlı düşmanı 120. seviyeye
 * taşır. Statlar ve saldırı tabanları "beklenen insan statı" eğrisiyle, can aynı eğrinin can formülüyle,
 * XP ise hedef seviyenin XP ihtiyacıyla ölçeklenir. Kademe seviyeden türetilir. Böylece Perde 2–4'ün
 * E→S düşmanları, Kule katları ve kapılar az sayıda elle yazılmış düşman tasarımından üretilir.
 */
object EnemyScaling {
    const val SEPARATOR = '@'
    private const val MAX_LEVEL = 2000

    /** "id@seviye" → (id, seviye); ölçeksiz başvuruda seviye null. */
    fun parse(ref: String): Pair<String, Int?> {
        val i = ref.lastIndexOf(SEPARATOR)
        if (i <= 0) return ref to null
        val level = ref.substring(i + 1).toIntOrNull() ?: return ref to null
        return ref.substring(0, i) to level.coerceIn(1, MAX_LEVEL)
    }

    fun ref(id: String, level: Int): String = "$id$SEPARATOR$level"

    /** Temel düşman kimliği ("x@40" → "x"). */
    fun baseId(ref: String): String = parse(ref).first

    /**
     * Seviyedeki beklenen ortalama stat: 5 + Σ (4 · kademe çarpanı + 3) / 8. Sınıf artışı (≈ 4 puan) kademeyle
     * çarpılır, 3 serbest puan eklenir; sekiz stata yayılır (DECISIONS #105).
     */
    private val avgStat: DoubleArray = DoubleArray(MAX_LEVEL + 2).also { a ->
        a[1] = Stats.BASE_VALUE.toDouble()
        for (l in 2..MAX_LEVEL + 1) {
            val mult = Progression.growthMultiplier(Grade.forLevel(l - 1))
            a[l] = a[l - 1] + (4.0 * mult + 3.0) / 8.0
        }
    }

    fun expectedStat(level: Int): Double = avgStat[level.coerceIn(1, MAX_LEVEL)]

    private fun expectedHp(level: Int): Double = 50 + 10 * expectedStat(level) + 5 * level

    /**
     * Ölçekli düşmanın XP'si: hedef seviyenin bir sonraki seviye ihtiyacı × max(0,1; 0,012 · L) × tür çarpanı.
     * Seviye yükseldikçe bir zafer daha çok seviyeye denk gelir (OP MC üstel büyüme); 25 → 800 aynı seviyedeki
     * ~290 zaferle aşılır. Tür çarpanı tanımdaki XP'nin kendi seviyesine oranıdır (sıradan ≈ 1, boss ×3).
     */
    fun xpFor(def: EnemyDef, level: Int): Long {
        val native = def.xp.toDouble() / Progression.xpToNext(def.level).coerceAtLeast(1)
        val kind = (native / 0.1).coerceIn(0.5, 3.0) * (if (def.boss) 3.0 else 1.0)
        return (Progression.xpToNext(level) * maxOf(0.1, 0.012 * level) * kind).roundToLong()
    }

    fun scale(def: EnemyDef, level: Int): EnemyDef {
        if (level == def.level) return def.copy(id = ref(def.id, level))
        val s = expectedStat(level) / expectedStat(def.level)
        val hp = expectedHp(level) / expectedHp(def.level)
        fun scaleStats(st: Stats) = Stats(Stat.entries.associateWith { (st[it] * s).roundToInt().coerceAtLeast(1) })
        fun scaleAttack(a: EnemyAttack) = a.copy(base = (a.base * s).roundToInt())
        return def.copy(
            id = ref(def.id, level),
            level = level,
            grade = maxOf(Grade.forLevel(level), if (level >= def.level) def.grade else Grade.F),
            maxHp = (def.maxHp * hp).roundToInt().coerceAtLeast(1),
            stats = scaleStats(def.stats),
            attacks = def.attacks.map(::scaleAttack),
            xp = xpFor(def, level),
            credits = (def.credits * level / def.level.coerceAtLeast(1).toDouble()).roundToInt(),
            phases = def.phases.map { p ->
                p.copy(buff = p.buff.mapValues { (_, v) -> (v * s).roundToInt() }, attack = p.attack?.let(::scaleAttack))
            },
        )
    }
}
