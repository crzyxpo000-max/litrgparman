package com.zincir.mantik

/** Bir setin değeri. Ağırlıksız hareketlerde kg = 0; "sn" birimli hareketlerde tekrar = saniye. */
data class SetDegeri(val kg: Double, val tekrar: Int)

/** Yeni seans için önerilen başlangıç değerleri. */
data class SetOnerisi(
    val setler: List<SetDegeri>,
    val agirlikArtir: Boolean,   // "Ağırlığı artır, alt tekrar sınırından başla"
    val donus: Boolean,          // kesintiden dönüş: değerler son seansın %80'i
)

object Ilerleme {
    const val DONUS_GUN = 7
    const val DONUS_KATSAYISI = 0.8

    /** İlerleme kuralı: önceki seansın tüm setlerinde üst tekrar sınırına ulaşıldıysa ağırlık artırılır. */
    fun agirlikArtirilmali(onceki: List<SetDegeri>, tekrarUst: Int): Boolean =
        onceki.isNotEmpty() && onceki.all { it.tekrar >= tekrarUst }

    /** Kesintiden dönüş: son ağırlık seansının üzerinden 7 günden fazla geçtiyse. */
    fun kesintidenDonus(sonAgirlikGunu: Long?, bugun: Long): Boolean =
        sonAgirlikGunu != null && bugun - sonAgirlikGunu > DONUS_GUN

    /** Ağırlığı en yakın yarım kiloya yuvarlar. */
    fun yuvarla(kg: Double, adim: Double = 0.5): Double = Math.round(kg / adim) * adim

    /**
     * Yeni seansta her set için varsayılan değerler.
     *  - Önceki kayıt yoksa: 0 kg, alt tekrar sınırı.
     *  - Kesintiden dönüşte: önceki ağırlığın %80'i (ilerleme önerisi yapılmaz).
     *  - İlerleme kuralı tuttuysa: aynı ağırlık yazılır (kullanıcı artırır), tekrar alt sınıra iner.
     *  - Yoksa: önceki değerlerin aynısı.
     */
    fun oner(onceki: List<SetDegeri>, setSayisi: Int, tekrarMin: Int, tekrarUst: Int, donus: Boolean): SetOnerisi {
        if (onceki.isEmpty()) {
            return SetOnerisi(List(setSayisi) { SetDegeri(0.0, tekrarMin) }, agirlikArtir = false, donus = false)
        }
        val artir = !donus && agirlikArtirilmali(onceki, tekrarUst)
        val setler = List(setSayisi) { i ->
            val o = onceki.getOrElse(i) { onceki.last() }
            when {
                donus -> SetDegeri(yuvarla(o.kg * DONUS_KATSAYISI), o.tekrar)
                artir -> SetDegeri(o.kg, tekrarMin)
                else -> o
            }
        }
        return SetOnerisi(setler, agirlikArtir = artir, donus = donus)
    }
}
