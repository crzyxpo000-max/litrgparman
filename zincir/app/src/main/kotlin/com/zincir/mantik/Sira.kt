package com.zincir.mantik

/** Sıradaki iş türleri. [kod] veritabanındaki seans türüyle eşleşir. */
enum class Is(val baslik: String, val kod: String?, val agirlik: Boolean) {
    SEANS_A("Seans A", "A", true),
    SEANS_B("Seans B", "B", true),
    KARDIYO("Kardiyo", "KARDIYO", false),
    ADIM("Yalnızca adım", null, false),
}

/**
 * Sıranın durumu: döngüde kaçıncı adımdayız ([index]) ve bu adım hangi günün işi ([gun]).
 * [gun] bugünden büyükse sıradaki iş yarın içindir (bugün bir şey tamamlanmıştır); yine de bugün yapılabilir.
 */
data class SiraDurumu(val index: Int, val gun: Long)

/**
 * Program haftanın günlerine değil sıraya göre ilerler: 14 adımlık tek bir dairesel sıra.
 * Kurallar:
 *  - Sıra kendiliğinden ilerlemez. A, B ve Kardiyo ancak yapılınca (ya da elle atlanınca) geçilir.
 *  - Tek istisna: sıradaki iş "Yalnızca adım" ise ve o gün bittiyse adım tamamlanmış sayılır.
 *  - Aynı gün içinde birden fazla adım tamamlanabilir.
 */
object Sira {
    /** A, Kardiyo, B, Adım, A, Kardiyo, Adım, B, Kardiyo, A, Adım, B, Kardiyo, Adım; 14. adımdan sonra 1'e döner. */
    val DONGU: List<Is> = listOf(
        Is.SEANS_A, Is.KARDIYO, Is.SEANS_B, Is.ADIM, Is.SEANS_A, Is.KARDIYO, Is.ADIM,
        Is.SEANS_B, Is.KARDIYO, Is.SEANS_A, Is.ADIM, Is.SEANS_B, Is.KARDIYO, Is.ADIM,
    )

    fun isAt(index: Int): Is = DONGU[Math.floorMod(index, DONGU.size)]

    private fun sonraki(index: Int): Int = Math.floorMod(index + 1, DONGU.size)

    /** Gün değişince çağrılır: günü bitmiş "Yalnızca adım" adımını tamamlanmış sayar. */
    fun guncelle(d: SiraDurumu, bugun: Long): SiraDurumu {
        var s = d
        while (isAt(s.index) == Is.ADIM && s.gun < bugun) {
            s = SiraDurumu(sonraki(s.index), s.gun + 1)
        }
        return s
    }

    /** Sıradaki iş [gun] günü yapıldı: sıra ilerler; yeni adım ertesi günün işidir (ama aynı gün de yapılabilir). */
    fun tamamla(d: SiraDurumu, gun: Long): SiraDurumu = SiraDurumu(sonraki(d.index), maxOf(d.gun, gun + 1))

    /** Sıradaki iş yapılmadan atlandı: yeni adım, atlananın gününü devralır. */
    fun atla(d: SiraDurumu, bugun: Long): SiraDurumu = SiraDurumu(sonraki(d.index), maxOf(d.gun, bugun))

    /** "Bu adımı atla" yalnızca Kardiyo ve Adım için vardır; ağırlık seansı atlanmaz. */
    fun atlanabilir(isi: Is): Boolean = isi == Is.KARDIYO || isi == Is.ADIM
}

enum class ZincirDurumu { YOK, UYARI, BELIRGIN }

/** Zincir uyarısı: son ağırlık seansının üzerinden kaç tam gün geçti? Uçuş günleri de sayılır. */
object Zincir {
    const val UYARI_GUN = 3
    const val BELIRGIN_GUN = 5

    /**
     * Son ağırlık seansından sonra tamamen geçen gün sayısı (seans günü ve henüz bitmemiş bugün sayılmaz).
     * Pazartesi seans yapıldıysa: Salı, Çarşamba, Perşembe geçince (Cuma günü) 3 olur.
     */
    fun gecenTamGun(sonAgirlikGunu: Long, bugun: Long): Int = (bugun - sonAgirlikGunu - 1).coerceAtLeast(0).toInt()

    /**
     * [sonAgirlikGunu] yoksa (hiç seans yapılmadıysa) program başlangıcından sayılır;
     * program henüz başlamadıysa uyarı yoktur.
     */
    fun durum(sonAgirlikGunu: Long?, baslangic: Long, bugun: Long): ZincirDurumu {
        if (bugun < baslangic) return ZincirDurumu.YOK
        val gecen = gecenTamGun(sonAgirlikGunu ?: (baslangic - 1), bugun)
        return when {
            gecen >= BELIRGIN_GUN -> ZincirDurumu.BELIRGIN
            gecen >= UYARI_GUN -> ZincirDurumu.UYARI
            else -> ZincirDurumu.YOK
        }
    }

    /** "Bu hafta" sayacı takvim haftası değil, bugün dahil son 7 gündür. */
    fun son7GunSeans(agirlikSeansGunleri: List<Long>, bugun: Long): Int = agirlikSeansGunleri.count { it in (bugun - 6)..bugun }
}

/**
 * Takvime bağlı hesaplar.
 *  - Program haftası: başlangıç tarihinden gün sayarak (ilk 2 hafta kuralı, 8 haftada bir hafif hafta).
 *  - Takvim haftası: Pazartesi–Pazar (haftalık kilo ortalaması, haftalık özet, haftalık sayaçlar).
 */
object Program {
    /** Program haftası: başlangıç gününden itibaren 7'şer gün. İlk hafta 0'dır. */
    fun haftaIndex(baslangic: Long, gun: Long): Int = Math.floorDiv(gun - baslangic, 7L).toInt()

    /** Verilen günün içinde olduğu haftanın Pazartesi'si. (Gün 0 = 1 Ocak 1970, Perşembe.) */
    fun takvimHaftaBasi(gun: Long): Long = gun - Math.floorMod(gun + 3, 7L)

    /** Takvim haftası numarası: program başlangıcının içinde olduğu hafta 0'dır. */
    fun takvimHaftaIndex(baslangic: Long, gun: Long): Int = ((takvimHaftaBasi(gun) - takvimHaftaBasi(baslangic)) / 7L).toInt()

    /** İlk 2 hafta alışma dönemi: her hareket 2 set, rahat ağırlık. */
    fun alismaDonemi(haftaIndex: Int): Boolean = haftaIndex < 2

    /** Her 8 haftada bir (8., 16., 24. hafta) hafif hafta önerilir. */
    fun hafifHaftaOnerilir(haftaIndex: Int): Boolean = haftaIndex >= 0 && (haftaIndex + 1) % 8 == 0

    /** O hafta bir hareket için gösterilecek set sayısı. */
    fun setSayisi(temel: Int, haftaIndex: Int, hafifHafta: Boolean): Int = when {
        alismaDonemi(haftaIndex) -> minOf(temel, 2)
        hafifHafta -> (temel + 1) / 2      // yarıya iner, yukarı yuvarlanır: 3 → 2, 2 → 1
        else -> temel
    }
}
