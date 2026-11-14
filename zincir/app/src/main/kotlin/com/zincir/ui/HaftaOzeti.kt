package com.zincir.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zincir.mantik.Bicim
import com.zincir.veri.Ayarlar
import com.zincir.veri.GunKaydi
import com.zincir.veri.Seans
import com.zincir.veri.Tur
import com.zincir.veri.YemekKaydi

/** Bir haftanın özeti. Ortalamalar yalnızca veri girilen günler üzerinden alınır; hiç veri yoksa null. */
data class HaftaOzeti(
    val agirlik: Int, val kardiyo: Int, val germeGunu: Int,
    val adimOrt: Int?, val uykuOrt: Double?, val kcalOrt: Int?, val proteinOrt: Int?,
)

fun haftaOzeti(haftaBasi: Long, seanslar: List<Seans>, gunler: List<GunKaydi>, yemekler: List<YemekKaydi>, germeHedefDk: Int): HaftaOzeti {
    val aralik = haftaBasi..haftaBasi + 6
    val s = seanslar.filter { it.tamamlandi && it.gun in aralik }
    val g = gunler.filter { it.gun in aralik }
    val gunlukYemek = yemekler.filter { it.gun in aralik }.groupBy { it.gun }
    val adimlar = g.filter { it.adim > 0 }.map { it.adim }
    val uykular = g.mapNotNull { it.uykuSaat }
    return HaftaOzeti(
        agirlik = s.count { it.tur != Tur.KARDIYO },
        kardiyo = s.count { it.tur == Tur.KARDIYO },
        germeGunu = g.count { it.germeDk >= germeHedefDk },
        adimOrt = adimlar.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        uykuOrt = uykular.takeIf { it.isNotEmpty() }?.average(),
        kcalOrt = gunlukYemek.values.takeIf { it.isNotEmpty() }?.map { gun -> gun.sumOf { it.kcal } }?.average()?.toInt(),
        proteinOrt = gunlukYemek.values.takeIf { it.isNotEmpty() }?.map { gun -> gun.sumOf { it.protein } }?.average()?.toInt(),
    )
}

@Composable
fun HaftaOzetiTablosu(o: HaftaOzeti, a: Ayarlar) {
    OzetSatiri("Ağırlık seansı", "${o.agirlik}/${a.haftalikSeansHedef}")
    OzetSatiri("Kardiyo", "${o.kardiyo}")
    OzetSatiri("Germe günü", "${o.germeGunu}/${a.haftalikGermeHedef}")
    OzetSatiri("Ortalama adım", o.adimOrt?.let { Bicim.tam(it) } ?: "—")
    OzetSatiri("Ortalama uyku", o.uykuOrt?.let { "${Bicim.ondalik(it)} sa" } ?: "—")
    OzetSatiri("Ortalama kalori", o.kcalOrt?.let { "${Bicim.tam(it)} kcal" } ?: "—")
    OzetSatiri("Ortalama protein", o.proteinOrt?.let { "$it g" } ?: "—")
}

@Composable
fun OzetSatiri(ad: String, deger: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(ad, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(deger, style = MaterialTheme.typography.titleLarge)
    }
    Cizgi()
}
