package com.zincir.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zincir.AnaViewModel
import com.zincir.mantik.Bicim
import com.zincir.mantik.HaftaOrt
import com.zincir.mantik.Kilo
import com.zincir.mantik.Oneri
import com.zincir.mantik.Program
import com.zincir.mantik.Tarti
import com.zincir.veri.Ayarlar
import com.zincir.veri.Olcum
import com.zincir.veri.Tur
import kotlinx.coroutines.launch

/** İlerleme sekmesi: kilo ve grafik, ayar önerileri, bel çevresi, hareket geçmişi, haftalık özet. */
@Composable
fun IlerlemeEkrani(vm: AnaViewModel, onGozden: () -> Unit) {
    val a = vm.ayarlar.collectAsStateWithLifecycle().value ?: return
    val bugun by vm.bugun.collectAsStateWithLifecycle()
    val kilolar by vm.kilolar.collectAsStateWithLifecycle()
    val beller by vm.beller.collectAsStateWithLifecycle()
    val seanslar by vm.seanslar.collectAsStateWithLifecycle()
    val sonGunler by vm.sonGunler.collectAsStateWithLifecycle()
    val sonYemekler by vm.sonYemekler.collectAsStateWithLifecycle()
    val hareketAdlari by vm.kayitliHareketAdlari.collectAsStateWithLifecycle()
    val kapsam = rememberCoroutineScope()

    val dunSecili by vm.dunSecili.collectAsStateWithLifecycle()
    // Haftalık ortalama ve özet takvim haftasına göredir: Pazartesi–Pazar.
    val buHafta = Program.takvimHaftaIndex(a.baslangicGunu, bugun)
    val haftalar = remember(kilolar, a.baslangicGunu) { Kilo.haftalikOrtalamalar(kilolar.map { Tarti(it.gun, it.deger) }, a.baslangicGunu) }
    val buHaftaOrt = haftalar.firstOrNull { it.index == buHafta }
    val fark = Kilo.fark(haftalar, buHafta)

    var silinecek by remember { mutableStateOf<Olcum?>(null) }
    var belDialog by remember { mutableStateOf(false) }
    var hareketDialog by remember { mutableStateOf(false) }
    var seciliHareket by rememberSaveable { mutableStateOf<String?>(null) }
    var haftaKayma by rememberSaveable { mutableIntStateOf(0) }

    Sayfa {
        EkranBasligi("İlerleme", ust = "Başlangıç ${Bicim.ondalik(a.baslangicKilo)} kg · hedef ${Bicim.ondalik(a.hedefKiloAlt)}–${Bicim.ondalik(a.hedefKiloUst)} kg")

        // ---- Kilo
        Bolum("Kilo", ust = 20.dp)
        val bugunku = kilolar.firstOrNull { it.gun == bugun }
        var kilo by remember(bugunku?.deger, kilolar.lastOrNull()?.deger) {
            mutableStateOf(Bicim.giris(bugunku?.deger ?: kilolar.lastOrNull()?.deger ?: a.baslangicKilo))
        }
        Text("Sabah, aç karnına.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Bosluk(8.dp)
        SayiGirisi(kilo, { kilo = it }, "kg", adim = 0.1, ondalik = true)
        Bosluk(8.dp)
        val kiloDeger = Bicim.coz(kilo)
        DoluDugme(
            (if (dunSecili) "Dünkü" else "Bugünkü") + if (bugunku == null) " tartıyı kaydet" else " tartıyı güncelle (${Bicim.kilo(bugunku.deger)} kg)",
            Modifier.fillMaxWidth(), etkin = kiloDeger != null && kiloDeger in 30.0..300.0,
        ) { vm.olcumKaydet(Tur.KILO, kiloDeger!!) }

        Bosluk(20.dp)
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Etiket("Bu hafta ortalama")
                Text(buHaftaOrt?.let { Bicim.kilo(it.ortalama) } ?: "—", style = MaterialTheme.typography.displaySmall)
                Text(buHaftaOrt?.let { "kg · ${it.adet} tartı" } ?: "tartı yok", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Etiket("Önceki haftaya göre")
                Text(fark?.let { Bicim.fark(it) } ?: "—", style = MaterialTheme.typography.displaySmall)
                Text("kg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (kilolar.isNotEmpty()) {
            Bosluk(20.dp)
            CizgiGrafik(
                noktalar = kilolar.map { Nokta(it.gun, it.deger) },
                // Haftalık ortalama, haftanın ortasındaki güne çizilir.
                cizgi = haftalar.map { Nokta(Program.takvimHaftaBasi(a.baslangicGunu) + it.index * 7L + 3, it.ortalama) },
                birim = "kg", noktaAdi = "tartı", cizgiAdi = "hafta ort.",
                bant = a.hedefKiloAlt..a.hedefKiloUst,
                bantAdi = "hedef ${Bicim.ondalik(a.hedefKiloAlt)}–${Bicim.ondalik(a.hedefKiloUst)}",
            )
            Bosluk()
            Etiket("Son tartılar")
            kilolar.reversed().take(7).forEach { o ->
                ListeSatiri(Bicim.tarih(o.gun), sag = "${Bicim.kilo(o.deger)} kg", onClick = { silinecek = o })
            }
        }

        // ---- Ayar önerileri
        Bolum("Ayar önerisi")
        AyarOnerileri(a, haftalar, buHafta)

        // ---- Bel çevresi
        Bolum("Bel çevresi")
        val sonBel = beller.lastOrNull()
        Row(verticalAlignment = Alignment.Bottom) {
            Text(sonBel?.let { Bicim.ondalik(it.deger) } ?: "—", style = MaterialTheme.typography.displaySmall)
            Text(
                sonBel?.let { " cm · ${Bicim.tamTarih(it.gun)}" } ?: " henüz ölçüm yok",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        if (sonBel == null || bugun - sonBel.gun >= 14) {
            Bosluk(8.dp)
            Not(
                if (sonBel == null) "2 haftada bir ölç: göbek deliği hizasından, nefes verirken." else "Son ölçümün üzerinden ${bugun - sonBel.gun} gün geçti.",
                baslik = "Bel ölçüsü zamanı",
            )
        }
        Bosluk()
        CizgiDugme("Bel ölçüsü gir", Modifier.fillMaxWidth()) { belDialog = true }
        beller.reversed().take(5).forEach { o ->
            ListeSatiri(Bicim.tamTarih(o.gun), sag = "${Bicim.ondalik(o.deger)} cm", onClick = { silinecek = o })
        }

        // ---- Hareket geçmişi
        Bolum("Hareket geçmişi")
        if (hareketAdlari.isEmpty()) {
            Text("Henüz kayıtlı set yok. İlk seanstan sonra burada grafik görünür.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val ad = seciliHareket?.takeIf { it in hareketAdlari } ?: hareketAdlari.first()
            ListeSatiri(ad, alt = "Hareketi değiştirmek için dokun", onClick = { hareketDialog = true })
            val gecmis by remember(ad) { vm.dao.hareketGecmisi(ad) }.collectAsStateWithLifecycle(emptyList())
            if (gecmis.isNotEmpty()) {
                // Ağırlıksız hareketlerde (plank, dead bug) kg hep 0'dır: tekrar/süre çizilir.
                val agirlikli = gecmis.any { it.kg > 0 }
                Bosluk()
                CizgiGrafik(
                    noktalar = emptyList(),
                    cizgi = gecmis.map { Nokta(it.gun, if (agirlikli) it.kg else it.tekrar.toDouble()) },
                    birim = if (agirlikli) "kg" else "",
                    cizgiAdi = if (agirlikli) "seanstaki en yüksek kg" else "seanstaki en yüksek tekrar/süre",
                )
            }
        }

        // ---- Haftalık özet
        Bolum("Haftalık özet")
        val haftaBasi = Program.takvimHaftaBasi(bugun) + haftaKayma * 7L
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KareDugme("‹") { haftaKayma-- }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (haftaKayma) {
                        0 -> "Bu hafta"
                        -1 -> "Geçen hafta"
                        else -> "${-haftaKayma} hafta önce"
                    },
                    style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                )
                Text(
                    "Pzt ${Bicim.kisaTarih(haftaBasi)} – Paz ${Bicim.kisaTarih(haftaBasi + 6)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            KareDugme("›") { if (haftaKayma < 0) haftaKayma++ }
        }
        Bosluk(8.dp)
        HaftaOzetiTablosu(haftaOzeti(haftaBasi, seanslar, sonGunler, sonYemekler, a.germeHedefDk), a)
        Bosluk(20.dp)
        DoluDugme("Haftalık gözden geçirme", Modifier.fillMaxWidth(), onClick = onGozden)
    }

    if (belDialog) {
        FormDialog(
            baslik = "Bel çevresi", onKapat = { belDialog = false },
            alanlar = listOf(Alan("Bel (cm)", "", AlanTuru.ONDALIK)),
            dogrula = { if (Bicim.coz(it[0]) == null) "Bir sayı gir." else null },
        ) { d -> vm.olcumKaydet(Tur.BEL, Bicim.coz(d[0])!!) }
    }
    if (hareketDialog) {
        ListeDialog("Hareket seç", hareketAdlari, onKapat = { hareketDialog = false }) { seciliHareket = it }
    }
    silinecek?.let { o ->
        OnayDialog("Ölçümü sil", "${Bicim.tamTarih(o.gun)} tarihli kayıt silinecek.", "Sil", onKapat = { silinecek = null }) {
            kapsam.launch { vm.dao.olcumSil(o) }
        }
    }
}

/** Ayar önerileri: yalnızca öneri gösterir, hiçbir hedefi kendiliğinden değiştirmez. */
@Composable
fun AyarOnerileri(a: Ayarlar, haftalar: List<HaftaOrt>, buHafta: Int) {
    val oneriler = Kilo.oneriler(haftalar, buHafta, a.baslangicKilo)
    if (oneriler.isEmpty()) {
        Text(
            "Öneri için programın içinde en az bir haftalık tartı gerekli.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    oneriler.forEach {
        Not(it.metin)
        Bosluk(10.dp)
    }
    val azaltmaOnerisi = Oneri.AZALT in oneriler || Oneri.BES_KG in oneriler
    if (azaltmaOnerisi && a.kaloriHedef - 100 < Kilo.KALORI_TABAN) {
        Uyari("Kalori hedefi ${Bicim.tam(Kilo.KALORI_TABAN)} kcal altına inemez.", "Şu anki hedef ${Bicim.tam(a.kaloriHedef)} kcal. Kalori yerine adım ekle.")
        Bosluk(10.dp)
    }
    Text(
        "Bunlar yalnızca öneridir. Hedefi Ayarlar'dan sen değiştirirsin; şu anki kalori hedefi ${Bicim.tam(a.kaloriHedef)} kcal.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
