package com.zincir.mantik

/** Sıradaki iş türleri. [kod] veritabanındaki seans türüyle eşleşir. */
enum class Is(val baslik: String, val kod: String?, val agirlik: Boolean) {
    SEANS_A("Seans A", "A", true),
    SEANS_B("Seans B", "B", true),
    KARDIYO("Kardiyo", "KARDIYO", false),
    ADIM("Yalnızca adım", null, false),
}

/**
 * Sıranın durumu: döngüde kaçıncı işteyiz ([index]) ve bu iş hangi günden beri bekliyor ([gun]).
 * [gun] bugünden büyükse bugünün işi yapılmış demektir (sıradaki iş yarın başlar).
 */
data class SiraDurumu(val index: Int, val gun: Long)

/**
 * Program haftanın günlerine değil sıraya göre ilerler.
 * Kurallar:
 *  - Antrenman (A, B, Kardiyo) yapılınca sıra bir ilerler; yapılmazsa aynı iş bekler (atlanan gün sırayı bozmaz).
 *  - "Yalnızca adım" günü, o gün geçince kendiliğinden biter.
 */
object Sira {
    /** İki haftalık döngü: A, Kardiyo, B, Adım, A, Kardiyo, Adım; sonra B, Kardiyo, A, Adım, B, Kardiyo, Adım. */
    val DONGU: List<Is> = listOf(
        Is.SEANS_A, Is.KARDIYO, Is.SEANS_B, Is.ADIM, Is.SEANS_A, Is.KARDIYO, Is.ADIM,
        Is.SEANS_B, Is.KARDIYO, Is.SEANS_A, Is.ADIM, Is.SEANS_B, Is.KARDIYO, Is.ADIM,
    )

    fun isAt(index: Int): Is = DONGU[Math.floorMod(index, DONGU.size)]

    private fun sonraki(index: Int): Int = Math.floorMod(index + 1, DONGU.size)

    /** Gün değişince çağrılır: günü geçmiş "Yalnızca adım" işini bitirir. */
    fun guncelle(d: SiraDurumu, bugun: Long): SiraDurumu {
        var s = d
        while (isAt(s.index) == Is.ADIM && s.gun < bugun) {
            s = SiraDurumu(sonraki(s.index), s.gun + 1)
        }
        return s
    }

    /** Sıradaki iş bugün yapıldı: sıra ilerler, yeni iş yarından itibaren bekler. */
    fun tamamla(d: SiraDurumu, bugun: Long): SiraDurumu = SiraDurumu(sonraki(d.index), bugun + 1)

    /** Sıradaki iş yapılmadan atlandı: yeni iş bugünden itibaren bekler. */
    fun atla(d: SiraDurumu, bugun: Long): SiraDurumu = SiraDurumu(sonraki(d.index), bugun)

    /** Bugünün işi yapıldı mı? */
    fun bugunTamam(d: SiraDurumu, bugun: Long): Boolean = d.gun > bugun

    /**
     * Sıradaki ağırlık seansı kaç gündür yapılmadan bekliyor (bugün hariç).
     * [muafGunler]: uçuş günleri ve seansın ertelendiği günler sayılmaz.
     */
    fun kacirilanGun(d: SiraDurumu, bugun: Long, muafGunler: Set<Long> = emptySet()): Int {
        if (!isAt(d.index).agirlik) return 0
        return (d.gun until bugun).count { it !in muafGunler }
    }

    /** "İki seansı üst üste kaçırma" uyarısı: ağırlık seansının günü geçtiyse gösterilir. */
    fun zincirUyarisi(d: SiraDurumu, bugun: Long, muafGunler: Set<Long> = emptySet()): Boolean =
        kacirilanGun(d, bugun, muafGunler) >= 1
}

/** Program haftaları: başlangıç tarihinden sayılan 7 günlük dilimler. İlk hafta index 0'dır. */
object Program {
    fun haftaIndex(baslangic: Long, gun: Long): Int = Math.floorDiv(gun - baslangic, 7L).toInt()

    fun haftaBasi(baslangic: Long, gun: Long): Long = baslangic + haftaIndex(baslangic, gun) * 7L

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
