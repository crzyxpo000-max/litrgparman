package com.yukselis.engine.system

/**
 * Gizli Eğilim vektörünün sekiz ekseni (GDD: Sınıf → davranıştan doğan teklif).
 * Oyuncu görmez; Sistem seviye 10'daki sınıf teklifini buna göre yapar.
 * Sıra önemlidir: eşitlikte önce gelen eğilim kazanır.
 */
enum class Affinity(val key: String) {
    MELEE("yakin_dovus"),
    RANGED("menzilli"),
    MAGIC("buyu"),
    STEALTH("gizlilik"),
    GUARD("koruma"),
    SUMMON("cagirma"),
    CRAFT("zanaat"),
    DARK("karanlik");

    companion object {
        fun fromKey(key: String): Affinity? = entries.firstOrNull { it.key == key }

        /**
         * Aşama 1 içeriğinde eğilimler düz değişkendi (egilim_*). Kayıt geçişi ve içerik
         * taşıması bu eşlemeyi kullanır. Algı, "uzaktan gözleyen" oyuncuyu Menzilli'ye iter.
         */
        val LEGACY_VARS: Map<String, Affinity> = mapOf(
            "egilim_algi" to RANGED,
            "egilim_menzilli" to RANGED,
            "egilim_yakin" to MELEE,
            "egilim_gizlilik" to STEALTH,
            "egilim_koruma" to GUARD,
            "egilim_buyu" to MAGIC,
            "egilim_cagirma" to SUMMON,
            "egilim_zanaat" to CRAFT,
            "egilim_karanlik" to DARK,
        )
    }
}

/** Eğilim puanları; değişmez. */
data class AffinityVector(val values: Map<Affinity, Int> = emptyMap()) {
    operator fun get(a: Affinity): Int = values[a] ?: 0

    fun plus(a: Affinity, delta: Int): AffinityVector =
        copy(values = values + (a to (get(a) + delta).coerceAtLeast(0)))

    /** Büyükten küçüğe sıralı eğilimler; eşitlikte enum sırası. */
    fun ranked(): List<Affinity> = Affinity.entries.sortedWith(compareByDescending<Affinity> { get(it) }.thenBy { it.ordinal })
}
