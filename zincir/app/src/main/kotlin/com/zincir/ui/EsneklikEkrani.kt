package com.zincir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zincir.AnaViewModel
import com.zincir.GermeCalisma
import com.zincir.mantik.Bicim
import com.zincir.veri.Olcum
import com.zincir.veri.Tur
import kotlinx.coroutines.launch
import kotlin.math.ceil

/** Esneklik sekmesi: germe rutini, haftalık sayaç, aylık ölçüm. */
@Composable
fun EsneklikEkrani(vm: AnaViewModel) {
    val a = vm.ayarlar.collectAsStateWithLifecycle().value ?: return
    val calisma by vm.germeCalisma.collectAsStateWithLifecycle()
    val c = calisma
    if (c != null) {
        GermeZamanlayici(vm, c)
        return
    }

    val bugun by vm.bugun.collectAsStateWithLifecycle()
    val gun by vm.gun.collectAsStateWithLifecycle()
    val germeler by vm.germeler.collectAsStateWithLifecycle()
    val sonGunler by vm.sonGunler.collectAsStateWithLifecycle()
    val haftaBasi by vm.haftaBasi.collectAsStateWithLifecycle()
    val olcumler by vm.esneklikler.collectAsStateWithLifecycle()
    val kapsam = rememberCoroutineScope()

    val haftaGun = sonGunler.count { it.gun in haftaBasi..haftaBasi + 6 && it.germeDk >= a.germeHedefDk }
    val toplamSn = germeler.sumOf { it.sureSn * if (it.ikiTaraf) 2 else 1 }
    var olcumDialog by remember { mutableStateOf(false) }
    var silinecek by remember { mutableStateOf<Olcum?>(null) }

    Sayfa {
        EkranBasligi("Esneklik", ust = "Hedef: haftada en az ${a.haftalikGermeHedef} gün")
        Bosluk(8.dp)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Bu hafta germe günü", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("$haftaGun/${a.haftalikGermeHedef}", style = MaterialTheme.typography.displaySmall)
        }
        HedefSatiri("Bugün", "${gun.germeDk}", "${a.germeHedefDk} dk", gun.germeDk.toFloat() / a.germeHedefDk)
        Bosluk()
        DoluDugme("Başlat · ${(toplamSn + 59) / 60} dk", Modifier.fillMaxWidth(), etkin = germeler.isNotEmpty()) { vm.germeBaslat(germeler) }
        Text(
            "Rutin kendiliğinden ilerler. Geçişlerde telefon titrer; ekran kapanmaz.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Bolum("Rutin")
        germeler.forEachIndexed { i, g ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Text("${i + 1}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 16.dp))
                Column {
                    Text(g.ad, style = MaterialTheme.typography.titleMedium)
                    Text(
                        g.bolge + " · " + (if (g.ikiTaraf) "her taraf " else "") + "${g.sureSn} sn",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Cizgi()
        }

        Bolum("Aylık ölçüm")
        Text("Parmak ucu–yer mesafesi (cm). Dizler düz, öne eğil; parmak ucuyla yer arasını ölç.", style = MaterialTheme.typography.bodyMedium)
        val son = olcumler.lastOrNull()
        if (son == null || bugun - son.gun >= 30) {
            Bosluk()
            Not(if (son == null) "Henüz ölçüm yok. İlk ölçümü şimdi al." else "Son ölçümün üzerinden ${bugun - son.gun} gün geçti. Yeniden ölç.", baslik = "Ölçüm zamanı")
        }
        Bosluk()
        CizgiDugme("Ölçüm gir", Modifier.fillMaxWidth()) { olcumDialog = true }
        olcumler.reversed().take(8).forEach { o ->
            ListeSatiri(Bicim.tamTarih(o.gun), sag = "${Bicim.ondalik(o.deger)} cm", onClick = { silinecek = o })
        }
    }

    if (olcumDialog) {
        FormDialog(
            baslik = "Parmak ucu–yer", onKapat = { olcumDialog = false },
            alanlar = listOf(Alan("Mesafe (cm)", "", AlanTuru.ONDALIK)),
            dogrula = { if (Bicim.coz(it[0]) == null) "Bir sayı gir." else null },
        ) { d -> vm.olcumKaydet(Tur.ESNEKLIK, Bicim.coz(d[0])!!) }
    }
    silinecek?.let { o ->
        OnayDialog("Ölçümü sil", "${Bicim.tamTarih(o.gun)} tarihli ölçüm silinecek.", "Sil", onKapat = { silinecek = null }) {
            kapsam.launch { vm.dao.olcumSil(o) }
        }
    }
}

/** Çalışan rutin: büyük geri sayım. Süreyi ViewModel sayar; bu ekran yalnızca gösterir. */
@Composable
private fun GermeZamanlayici(vm: AnaViewModel, c: GermeCalisma) {
    EkraniAcikTut()
    val adim = c.adimlar[c.index]
    val toplamMs = c.adimlar.sumOf { it.sureSn } * 1000L
    val gecenMs = c.adimlar.take(c.index).sumOf { it.sureSn } * 1000L + (adim.sureSn * 1000L - c.kalanMs)

    Column(Modifier.fillMaxSize().padding(horizontal = Kenar).padding(top = 28.dp, bottom = 20.dp)) {
        if (c.bitti) {
            Etiket("Germe")
            Text("Tamamlandı.", style = MaterialTheme.typography.displayMedium)
            Bosluk(8.dp)
            Text("${(toplamMs / 60_000)} dakika bugüne kaydedildi.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            DoluDugme("Kapat", Modifier.fillMaxWidth()) { vm.germeKapat() }
            return@Column
        }

        Etiket("${c.index + 1} / ${c.adimlar.size}" + (adim.taraf?.let { " · $it" } ?: ""))
        Bosluk(6.dp)
        Text(adim.ad, style = MaterialTheme.typography.headlineMedium)
        Bosluk(4.dp)
        Text(
            adim.bolge + (adim.taraf?.let { " · $it taraf" } ?: ""),
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))
        Text(Bicim.sayac(ceil(c.kalanMs / 1000.0).toInt()), style = MaterialTheme.typography.displayLarge)
        Bosluk(8.dp)
        IlerlemeCizgisi(gecenMs.toFloat() / toplamMs)
        Bosluk(8.dp)
        val sonraki = c.adimlar.getOrNull(c.index + 1)
        Text(
            if (sonraki != null) "Sıradaki: ${sonraki.ad}" + (sonraki.taraf?.let { " ($it)" } ?: "") else "Son germe",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (c.duraklatildi) DoluDugme("Devam", Modifier.weight(1f)) { vm.germeDuraklat(false) }
            else CizgiDugme("Duraklat", Modifier.weight(1f)) { vm.germeDuraklat(true) }
            CizgiDugme("Geç", Modifier.weight(1f)) { vm.germeAtla() }
        }
        MetinDugme("Rutini bitir") { vm.germeKapat() }
    }
}
