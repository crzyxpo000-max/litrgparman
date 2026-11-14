package com.zincir.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zincir.AnaViewModel
import com.zincir.mantik.Bicim
import com.zincir.mantik.Is
import com.zincir.mantik.Program
import com.zincir.mantik.Sira
import com.zincir.mantik.SiraDurumu
import com.zincir.veri.Ayarlar
import com.zincir.veri.GunKaydi
import com.zincir.veri.Tur

/** Açılış ekranı: sıradaki iş, uçuş günü, uyku, günlük hedefler. */
@Composable
fun BugunEkrani(vm: AnaViewModel, onAyarlar: () -> Unit, onSekme: (Int) -> Unit) {
    val a = vm.ayarlar.collectAsStateWithLifecycle().value ?: return
    val bugun by vm.bugun.collectAsStateWithLifecycle()
    val gun by vm.gun.collectAsStateWithLifecycle()
    val sonGunler by vm.sonGunler.collectAsStateWithLifecycle()
    val seanslar by vm.seanslar.collectAsStateWithLifecycle()
    val yemekler by vm.yemekler.collectAsStateWithLifecycle()
    val aktifSeans by vm.aktifSeans.collectAsStateWithLifecycle()
    val hareketler by vm.hareketler.collectAsStateWithLifecycle()

    val sira = SiraDurumu(a.siraIndex, a.siraGunu)
    val isi = Sira.isAt(sira.index)
    // "Tamam": sıra yarına geçmiş VE bugün gerçekten bir seans yapılmış olmalı
    // (program başlamadan önce sıra ileri bir tarihte bekler; o durum "tamam" sayılmaz).
    val tamam = Sira.bugunTamam(sira, bugun) && seanslar.any { it.gun == bugun && it.tamamlandi }
    val haftaIndex = Program.haftaIndex(a.baslangicGunu, bugun)
    val haftaBasi = Program.haftaBasi(a.baslangicGunu, bugun)
    val haftaSeans = seanslar.count { it.tamamlandi && it.tur != Tur.KARDIYO && it.gun in haftaBasi..haftaBasi + 6 }
    val muafGunler = sonGunler.filter { it.ucusGunu || it.ertelendi }.map { it.gun }.toSet()
    val kacirilan = Sira.kacirilanGun(sira, bugun, muafGunler)
    val dunUcus = sonGunler.any { it.gun == bugun - 1 && it.ucusGunu }
    val plan by remember(haftaBasi) { vm.dao.gozdenAkisi(haftaBasi - 7) }.collectAsStateWithLifecycle(null)

    var adimDialog by remember { mutableStateOf(false) }
    var atlaDialog by remember { mutableStateOf(false) }

    Sayfa {
        EkranBasligi(
            baslik = "Bugün",
            ust = Bicim.tarih(bugun) + if (haftaIndex >= 0) " · Hafta ${haftaIndex + 1}" else "",
            sag = { MetinDugme("Ayarlar", onClick = onAyarlar) },
        )

        if (bugun < a.baslangicGunu) {
            Bosluk()
            Not("Program başlangıcı: ${Bicim.tamTarih(a.baslangicGunu)} (${a.baslangicGunu - bugun} gün sonra). O güne kadar uygulamayı deneyebilirsin.")
        }

        // Zincir uyarısı: uygulamanın asıl işi.
        if (kacirilan >= 1 && !tamam && aktifSeans == null) {
            Bosluk(20.dp)
            Uyari(
                "İki seansı üst üste kaçırma.",
                "${isi.baslik} $kacirilan gündür bekliyor. Vaktin azsa 15 dakika modu da sayılır.",
            )
        }

        Bolum("Sıradaki iş", ust = 24.dp)
        when {
            gun.ucusGunu -> {
                Text("Uçuş günü", style = MaterialTheme.typography.displaySmall)
                Bosluk(8.dp)
                Not("Antrenman zorunlu değil. Bugün yalnızca adım, su, öğün ve germe." + if (!tamam) " Sırada bekleyen: ${isi.baslik}." else "")
            }
            tamam -> {
                val yapilan = seanslar.firstOrNull { it.gun == bugun && it.tamamlandi }
                val ad = when (yapilan?.tur) {
                    Tur.A -> "Seans A"
                    Tur.B -> "Seans B"
                    Tur.KARDIYO -> "Kardiyo"
                    else -> "Bugünün işi"
                }
                Text("$ad tamam.", style = MaterialTheme.typography.displaySmall)
                Bosluk(4.dp)
                Text("Yarın: ${isi.baslik}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            gun.ertelendi && isi.agirlik -> {
                Text("Yalnızca adım", style = MaterialTheme.typography.displaySmall)
                Bosluk(8.dp)
                Not("${isi.baslik} yarına kaydırıldı. Bugün adım ve germe yeter.")
                MetinDugme("Geri al") { vm.gunDegistir { it.copy(ertelendi = false) } }
            }
            else -> SiradakiIs(
                isi = isi, a = a, seansAcik = aktifSeans != null, hareketSayisi = hareketler.count { it.seans == isi.kod },
                onBasla = { kisa ->
                    if (aktifSeans == null) vm.seansBaslat(isi.kod!!, kisa)
                    onSekme(1)
                },
                onKardiyo = { onSekme(1) },
                onAtla = { atlaDialog = true },
            )
        }

        Bosluk(8.dp)
        Cizgi()
        AnahtarSatiri("Bugün uçuş günü", gun.ucusGunu) { yeni -> vm.gunDegistir { it.copy(ucusGunu = yeni) } }
        Cizgi()
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Bu hafta ağırlık seansı", style = MaterialTheme.typography.titleMedium)
                val gunler = plan?.planGunleri?.split(",")?.mapNotNull { it.toLongOrNull() }.orEmpty()
                if (gunler.isNotEmpty()) {
                    Text(
                        "Plan: " + gunler.sorted().joinToString(" · ") { Bicim.gunAdi(it) } + if (bugun in gunler) " (bugün)" else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text("$haftaSeans/${a.haftalikSeansHedef}", style = MaterialTheme.typography.displaySmall)
        }

        Bolum("Uyku")
        UykuGirisi(gun) { yeni -> vm.gunDegistir { it.copy(uykuSaat = yeni) } }
        val uyku = gun.uykuSaat
        if (uyku != null && uyku < 6.0 && dunUcus && !gun.ucusGunu && isi.agirlik && !tamam && !gun.ertelendi) {
            Bosluk()
            Not("Uçuş sonrası 6 saatten az uyku. Ağırlık seansını yarına kaydır; bugün adım ve germe yeter.", baslik = "Öneri")
            Bosluk(8.dp)
            CizgiDugme("Seansı yarına kaydır", Modifier.fillMaxWidth()) { vm.gunDegistir { it.copy(ertelendi = true) } }
        }

        Bolum("Günlük hedefler")
        val kcal = yemekler.sumOf { it.kcal }
        val protein = yemekler.sumOf { it.protein }
        HedefSatiri("Kalori", Bicim.tam(kcal.toInt()), "${Bicim.tam(a.kaloriHedef)} kcal", (kcal / a.kaloriHedef).toFloat(), Modifier.clickable { onSekme(2) })
        HedefSatiri("Protein", Bicim.tam(protein.toInt()), "${a.proteinMin}–${a.proteinMax} g", (protein / a.proteinMin).toFloat(), Modifier.clickable { onSekme(2) })
        HedefSatiri("Adım", Bicim.tam(gun.adim), "${Bicim.tam(a.adimMin)}–${Bicim.tam(a.adimMax)}", gun.adim.toFloat() / a.adimMin)
        CizgiDugme("Adım gir", Modifier.fillMaxWidth()) { adimDialog = true }
        Bosluk(8.dp)
        HedefSatiri("Su", Bicim.ondalik(gun.suMl / 1000.0, 2), "${Bicim.ondalik(a.suHedefMl / 1000.0, 2)} L", gun.suMl.toFloat() / a.suHedefMl)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CizgiDugme("+250 ml", Modifier.weight(1f)) { vm.gunDegistir { it.copy(suMl = it.suMl + 250) } }
            CizgiDugme("+500 ml", Modifier.weight(1f)) { vm.gunDegistir { it.copy(suMl = it.suMl + 500) } }
        }
        if (gun.suMl > 0) MetinDugme("250 ml geri al") { vm.gunDegistir { it.copy(suMl = maxOf(0, it.suMl - 250)) } }
        Bosluk(8.dp)
        HedefSatiri("Germe", "${gun.germeDk}", "${a.germeHedefDk} dk", gun.germeDk.toFloat() / a.germeHedefDk, Modifier.clickable { onSekme(3) })
        HedefSatiri(
            "Uyku", uyku?.let { Bicim.ondalik(it) } ?: "—", "${Bicim.ondalik(a.uykuHedefSaat)}+ sa",
            ((uyku ?: 0.0) / a.uykuHedefSaat).toFloat(),
        )
    }

    if (adimDialog) {
        FormDialog(
            baslik = "Bugünkü adım", onKapat = { adimDialog = false },
            alanlar = listOf(Alan("Adım sayısı", if (gun.adim > 0) gun.adim.toString() else "", AlanTuru.TAM)),
            dogrula = { if (Bicim.cozTam(it[0]) == null) "Bir sayı gir." else null },
        ) { d -> vm.gunDegistir { it.copy(adim = Bicim.cozTam(d[0]) ?: it.adim) } }
    }
    if (atlaDialog) {
        OnayDialog(
            "Bu adımı atla", "${isi.baslik} yapılmadan sıradaki işe geçilecek. Emin misin?", "Atla",
            onKapat = { atlaDialog = false },
        ) { vm.siraAtla() }
    }
}

@Composable
private fun SiradakiIs(isi: Is, a: Ayarlar, seansAcik: Boolean, hareketSayisi: Int, onBasla: (kisa: Boolean) -> Unit, onKardiyo: () -> Unit, onAtla: () -> Unit) {
    Text(isi.baslik, style = MaterialTheme.typography.displaySmall)
    Bosluk(4.dp)
    when (isi) {
        Is.SEANS_A, Is.SEANS_B -> {
            Text("Ağırlık · $hareketSayisi hareket", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Bosluk(16.dp)
            if (seansAcik) {
                DoluDugme("Seansa devam et", Modifier.fillMaxWidth()) { onBasla(false) }
            } else {
                DoluDugme("Başla", Modifier.fillMaxWidth()) { onBasla(false) }
                Bosluk(8.dp)
                CizgiDugme("15 dakika modu", Modifier.fillMaxWidth()) { onBasla(true) }
                Text(
                    "Yalnızca ilk 2 hareket. Tamamlanınca seans sayılır.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Is.KARDIYO -> {
            Text("30–40 dk · eğimli yürüyüş, bisiklet ya da eliptik", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Bosluk(16.dp)
            DoluDugme("Kardiyoyu kaydet", Modifier.fillMaxWidth(), onClick = onKardiyo)
        }
        Is.ADIM -> {
            Text(
                "Bugün antrenman yok. Hedef ${Bicim.tam(a.adimMin)}–${Bicim.tam(a.adimMax)} adım.",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (isi != Is.ADIM && !seansAcik) MetinDugme("Bu adımı atla", onClick = onAtla)
}

/** "Dün gece kaç saat uyudun?" : yarım saatlik adımlarla − / +. */
@Composable
private fun UykuGirisi(gun: GunKaydi, onDegis: (Double) -> Unit) {
    Text("Dün gece kaç saat uyudun?", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        KareDugme("−") { onDegis(((gun.uykuSaat ?: 6.5) - 0.5).coerceIn(0.0, 14.0)) }
        Text(
            gun.uykuSaat?.let { "${Bicim.ondalik(it)} sa" } ?: "—",
            style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
        )
        KareDugme("+") { onDegis(((gun.uykuSaat ?: 6.5) + 0.5).coerceIn(0.0, 14.0)) }
    }
}
