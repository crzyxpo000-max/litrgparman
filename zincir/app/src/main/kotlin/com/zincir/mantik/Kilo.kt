package com.zincir.mantik

import kotlin.math.floor
import kotlin.math.roundToInt

data class Tarti(val gun: Long, val kg: Double)

/** Bir program haftasının tartı ortalaması. [index] 0 = ilk hafta; başlangıçtan önceki haftalar eksi. */
data class HaftaOrt(val index: Int, val ortalama: Double, val adet: Int)

/** Ayar önerileri. Yalnızca öneridir; uygulama hedefi kendiliğinden değiştirmez. */
enum class Oneri(val metin: String) {
    SU_NOTU("İlk 2 haftadaki hızlı düşüşün çoğu sudur; hesaba katma, hedefi değiştirme."),
    DEGISTIRME("Haftalık düşüş 0,4–0,8 kg aralığında. Değiştirme."),
    AZALT("2 hafta üst üste 0,3 kg'dan az düşüş. Kaydı kontrol et; doğruysa günlük 150–200 kcal azalt ya da 2.000 adım ekle."),
    EKLE("Haftada 1 kg'dan fazla düşüyorsun. Günlük 150–200 kcal ekle."),
    BES_KG("5 kg daha verdin. Kalori hedefini 100 kcal düşür."),
    IZLE("Bu hafta için ayar gerekmiyor. Kayda devam et."),
}

object Kilo {
    /** Kalori hedefi bunun altına ayarlanamaz. */
    const val KALORI_TABAN = 1800

    /** Tartıları program haftalarına böler ve her haftanın ortalamasını alır (haftaya göre sıralı). */
    fun haftalikOrtalamalar(tartilar: List<Tarti>, baslangic: Long): List<HaftaOrt> =
        tartilar.groupBy { Program.haftaIndex(baslangic, it.gun) }
            .map { (index, liste) -> HaftaOrt(index, liste.map { it.kg }.average(), liste.size) }
            .sortedBy { it.index }

    /** Bir haftanın ortalamasının önceki haftaya göre farkı (eksi = kilo verildi). Veri yoksa null. */
    fun fark(haftalar: List<HaftaOrt>, index: Int): Double? {
        val bu = haftalar.firstOrNull { it.index == index } ?: return null
        val onceki = haftalar.firstOrNull { it.index == index - 1 } ?: return null
        return bu.ortalama - onceki.ortalama
    }

    /**
     * Ayar önerileri. Değerlendirilen hafta: bitmiş son hafta ya da en az 3 tartısı olan içinde bulunulan hafta.
     * [buHafta]: bugünün program haftası (index).
     */
    fun oneriler(haftalar: List<HaftaOrt>, buHafta: Int, baslangicKilo: Double): List<Oneri> {
        val son = haftalar.lastOrNull { it.index in 0..buHafta && (it.index < buHafta || it.adet >= 3) }
            ?: return emptyList()
        // Kayıp: artı sayı = kilo verildi. Kayan nokta hatası sınırları bozmasın diye 2 haneye yuvarlanır.
        fun kayip(index: Int): Double? = fark(haftalar, index)?.let { -(it * 100).roundToInt() / 100.0 }

        val sonuc = mutableListOf<Oneri>()
        val k = kayip(son.index)
        if (son.index <= 1) {
            sonuc += Oneri.SU_NOTU
        } else if (k != null) {
            // İlk 2 haftanın düşüşü sayılmaz: "üst üste 2 hafta" için önceki kayıp da 3. haftadan sonra olmalı.
            val oncekiKayip = if (son.index >= 3) kayip(son.index - 1) else null
            sonuc += when {
                son.index >= 3 && k > 1.0 -> Oneri.EKLE
                k in 0.4..0.8 -> Oneri.DEGISTIRME
                k < 0.3 && oncekiKayip != null && oncekiKayip < 0.3 -> Oneri.AZALT
                else -> Oneri.IZLE
            }
        }
        // Her 5 kg kayıpta: bu hafta yeni bir 5 kg eşiği geçildiyse.
        val onceki = haftalar.firstOrNull { it.index == son.index - 1 }
        if (onceki != null) {
            val simdi = floor((baslangicKilo - son.ortalama) / 5.0)
            val once = floor((baslangicKilo - onceki.ortalama) / 5.0)
            if (simdi >= 1 && simdi > once) sonuc += Oneri.BES_KG
        }
        return sonuc
    }
}
