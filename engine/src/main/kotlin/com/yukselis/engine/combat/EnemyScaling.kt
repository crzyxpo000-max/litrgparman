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
 *
 * Çözülmüş başvuru kademe de taşıyabilir: `"id@738^S"` (yalnız motor üretir; `@oyuncu±n%` biçiminden, DECISIONS #177).
 */
object EnemyScaling {
    const val SEPARATOR = '@'

    /** Çözülmüş başvuruda sabit kademe eki: "id@738^S". İçerikte yazılmaz. */
    const val GRADE_SEPARATOR = '^'
    private const val MAX_LEVEL = 2000
    const val WORLD_PLAYER_MARGIN = 6

    /** XP oranının büyümesinin durduğu seviye (Aşama 9). */
    const val XP_LEVEL_CAP = 400

    /** "id@seviye" → (id, seviye); ölçeksiz başvuruda seviye null. */
    fun parse(ref: String): Pair<String, Int?> {
        val i = ref.lastIndexOf(SEPARATOR)
        if (i <= 0) return ref to null
        val level = ref.substring(i + 1).substringBefore(GRADE_SEPARATOR).toIntOrNull() ?: return ref to null
        return ref.substring(0, i) to level.coerceIn(1, MAX_LEVEL)
    }

    /** Çözülmüş başvurunun sabit kademesi ("x@738^S" → S); yoksa null (kademe seviyeden türetilir). */
    fun pinnedGrade(ref: String): Grade? {
        val at = ref.lastIndexOf(SEPARATOR)
        val i = ref.lastIndexOf(GRADE_SEPARATOR)
        return if (at <= 0 || i <= at) null else Grade.fromKey(ref.substring(i + 1))
    }

    fun ref(id: String, level: Int, grade: Grade? = null): String =
        "$id$SEPARATOR$level" + if (grade == null) "" else "$GRADE_SEPARATOR${grade.name}"

    /** Temel düşman kimliği ("x@40" → "x", "x@dunya+10" → "x"). */
    fun baseId(ref: String): String = ref.indexOf(SEPARATOR).let { if (it > 0) ref.substring(0, it) else ref }

    private val RELATIVE = Regex("""^(dunya|oyuncu)([+-]\d+)?(%)?$""")

    /** `%` yalnız `oyuncu±n%` biçiminde geçerlidir (`dunya±n` zaten yüzdedir). */
    private fun relative(suffix: String): MatchResult? =
        RELATIVE.matchEntire(suffix)?.takeIf { it.groupValues[3].isEmpty() || (it.groupValues[1] == "oyuncu" && it.groupValues[2].isNotEmpty()) }

    /** Başvuru eki geçerli mi: ek yok, tamsayı seviye ya da `dunya±n` / `oyuncu±n` / `oyuncu±n%`. */
    fun validSuffix(ref: String): Boolean {
        val i = ref.indexOf(SEPARATOR)
        if (i < 0) return true
        val suffix = ref.substring(i + 1)
        return suffix.toIntOrNull()?.let { it in 1..MAX_LEVEL } ?: (relative(suffix) != null)
    }

    /** Göreli seviye mi (`@dunya`, `@dunya+20` yüzde; `@oyuncu`, `@oyuncu+5` seviye; `@oyuncu-10%` yüzde)? */
    fun isRelative(ref: String): Boolean = ref.indexOf(SEPARATOR).let { it > 0 && relative(ref.substring(it + 1)) != null }

    /**
     * Göreli başvuruyu savaş başında sayıya çevirir: `@dunya±n` dünyanın seviyesi ±%n (gün ilerledikçe yükselir),
     * `@oyuncu±n` oyuncunun gerçek seviyesi ±n (her oyuncu için gerçek savaş). Mutlak başvuru olduğu gibi kalır.
     *
     * `@oyuncu±n%` oyuncunun seviyesinin ±%n'i ve **oyuncunun kademesi** (DECISIONS #177): yüksek seviyede ±n seviye
     * hiçbir şey değiştirmez (Sv. 850'de 5 seviye < %1 stat), yüzde her seviyede aynı ağırlıktadır. Kademe sabit
     * kalmasa Sv. 800–888 oyuncuda −%10 düşmanı S'in altına düşürür ve kademe farkı (×2 / ×0,5) savaşı bitirirdi.
     */
    fun resolve(ref: String, day: Int, playerLevel: Int): String {
        val i = ref.indexOf(SEPARATOR)
        if (i <= 0) return ref
        val m = relative(ref.substring(i + 1)) ?: return ref
        val n = m.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: 0
        if (m.groupValues[3].isNotEmpty()) {
            val level = (playerLevel * (1 + n / 100.0)).roundToInt().coerceIn(1, MAX_LEVEL)
            return EnemyScaling.ref(ref.substring(0, i), level, Grade.forLevel(playerLevel))
        }
        val level = when (m.groupValues[1]) {
            // Dünyaya bağlı tehdit, geride kalan oyuncunun en fazla WORLD_PLAYER_MARGIN seviye üstüne çıkar.
            "dunya" -> minOf(Progression.worldLevel(day) * (1 + n / 100.0), (playerLevel + WORLD_PLAYER_MARGIN).toDouble())
            else -> (playerLevel + n).toDouble()
        }
        return EnemyScaling.ref(ref.substring(0, i), level.roundToInt().coerceIn(1, MAX_LEVEL))
    }

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
        // Seviye başına zafer oranı S kademesine kadar büyür, sonra sabitlenir (Aşama 9: son günlerde seviye patlamasın).
        return (Progression.xpToNext(level) * (0.012 * level.coerceAtMost(XP_LEVEL_CAP)).coerceAtLeast(0.1) * kind).roundToLong()
    }

    fun scale(def: EnemyDef, level: Int, pinned: Grade? = null): EnemyDef {
        if (level == def.level) return def.copy(id = ref(def.id, level, pinned), grade = pinned ?: def.grade, xp = xpFor(def, level))
        val s = expectedStat(level) / expectedStat(def.level)
        val hp = expectedHp(level) / expectedHp(def.level)
        fun scaleStats(st: Stats) = Stats(Stat.entries.associateWith { (st[it] * s).roundToInt().coerceAtLeast(1) })
        fun scaleAttack(a: EnemyAttack) = a.copy(base = (a.base * s).roundToInt())
        return def.copy(
            id = ref(def.id, level, pinned),
            level = level,
            grade = pinned ?: maxOf(Grade.forLevel(level), if (level >= def.level) def.grade else Grade.F),
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
