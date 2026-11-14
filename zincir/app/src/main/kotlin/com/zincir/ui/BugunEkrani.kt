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
import com.zincir.mantik.Zincir
import com.zincir.mantik.ZincirDurumu
import com.zincir.veri.Ayarlar
import com.zincir.veri.GunKaydi
import com.zincir.veri.Tur

private const val DUN = "‹ Dün"
private const val BUGUN = "Bugün ›"

/** Açılış ekranı: gün geçişi, zincir uyarısı, sıradaki iş, uçuş günü, uyku, günlük hedefler. */
@Composable
fun BugunEkrani(vm: AnaViewModel, onAyarlar: () -> Unit, onSekme: (Int) -> Unit) {
    val a = vm.ayarlar.collectAsStateWithLifecycle().value ?: return
    // "bugun" kayıtların yazıldığı gündür (üstteki geçişte "Dün" seçiliyse dün); "gercekBugun" takvimdeki bugün.
    val bugun by vm.bugun.collectAsStateWithLifecycle()
    val gercekBugun by vm.gercekBugun.collectAsStateWithLifecycle()
    val dunSecili by vm.dunSecili.collectAsStateWithLifecycle()
    val gun by vm.gun.collectAsStateWithLifecycle()
    val sonGunler by vm.sonGunler.collectAsStateWithLifecycle()
    val seanslar by vm.seanslar.collectAsStateWithLifecycle()
    val yemekler by vm.yemekler.collectAsStateWithLifecycle()
    val aktifSeans by vm.aktifSeans.collectAsStateWithLifecycle()
    val hareketler by vm.hareketler.collectAsStateWithLifecycle()
    val sonAgirlik by vm.sonAgirlikGunu.collectAsStateWithLifecycle()
    val planlar by vm.egerPlanlar.collectAsStateWithLifecycle()
    val saatKalorisi by vm.saatKalorisi.collectAsStateWithLifecycle()

    val isi = Sira.isAt(a.siraIndex)
    val yarinIcin = a.siraGunu > gercekBugun   // bugün bir şey tamamlandı; sıradaki iş yarının işi
    val haftaIndex = Program.haftaIndex(a.baslangicGunu, bugun)
    val agirlikGunleri = seanslar.filter { it.tamamlandi && it.tur != Tur.KARDIYO }.map { it.gun }
    val son7 = Zincir.son7GunSeans(agirlikGunleri, gercekBugun)
    val zincir = Zincir.durum(sonAgirlik, a.baslangicGunu, gercekBugun)
    val yapilanlar = seanslar.filter { it.gun == bugun && it.tamamlandi }.reversed()
    val dunUcus = sonGunler.any { it.gun == bugun - 1 && it.ucusGunu }
    val planHaftasi = Program.takvimHaftaBasi(gercekBugun)
    val plan by remember(planHaftasi) { vm.dao.gozdenAkisi(planHaftasi - 7) }.collectAsStateWithLifecycle(null)

    var adimDialog by remember { mutableStateOf(false) }
    var atlaDialog by remember { mutableStateOf(false) }
    var planDialog by remember { mutableStateOf(false) }

    Sayfa {
        // Gece uçuşları için: gece yarısından sonra yapılan girişler düne yazılabilir.
        Secim(listOf(DUN, BUGUN), if (dunSecili) DUN else BUGUN) { vm.dunSec(it == DUN) }
        EkranBasligi(
            baslik = if (dunSecili) "Dün" else "Bugün",
            ust = Bicim.tarih(bugun) + if (haftaIndex >= 0) " · Hafta ${haftaIndex + 1}" else "",
            sag = { MetinDugme("Ayarlar", onClick = onAyarlar) },
        )
        if (dunSecili) {
            Bosluk()
            Not("Bütün sekmelerdeki girişler şu an düne (${Bicim.tarih(bugun)}) kaydediliyor. İşin bitince \"Bugün ›\" seç.")
        }

        if (gercekBugun < a.baslangicGunu) {
            Bosluk()
            Not("Program başlangıcı: ${Bicim.tamTarih(a.baslangicGunu)} (${a.baslangicGunu - gercekBugun} gün sonra). O güne kadar uygulamayı deneyebilirsin.")
        }

        // Zincir uyarısı: son ağırlık seansından beri 3 tam gün geçtiyse uyarı, 5 tam gün geçtiyse belirgin uyarı.
        if (zincir != ZincirDurumu.YOK && aktifSeans == null) {
            val gecen = Zincir.gecenTamGun(sonAgirlik ?: (a.baslangicGunu - 1), gercekBugun)
            val neden = if (sonAgirlik == null) "Program başlayalı $gecen tam gün oldu; henüz ağırlık seansı yok."
            else "Son ağırlık seansından beri $gecen tam gün geçti."
            val cozum = if (isi.agirlik) "Vaktin azsa 15 dakika modu da sayılır."
            else "Sırada ${isi.baslik} var: atlayıp ağırlık seansına geçebilirsin."
            Bosluk(20.dp)
            if (zincir == ZincirDurumu.BELIRGIN) BelirginUyari("İki seansı üst üste kaçırma.", "$neden $cozum")
            else Uyari("İki seansı üst üste kaçırma.", "$neden $cozum")
        }

        Bolum(if (yarinIcin) "Sıradaki iş · yarın için" else "Sıradaki iş", ust = 24.dp)
        if (yapilanlar.isNotEmpty()) {
            Text(
                (if (dunSecili) "Dün" else "Bugün") + " yapıldı: " + yapilanlar.joinToString(" · ") { seansAdi(it.tur) } + " ✓",
                style = MaterialTheme.typography.titleMedium,
            )
            if (yarinIcin) {
                Text("İstersen sıradakini de bugün yapabilirsin.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Bosluk()
        }
        when {
            gun.ucusGunu -> {
                Text("Uçuş günü", style = MaterialTheme.typography.displaySmall)
                Bosluk(8.dp)
                Not("Antrenman zorunlu değil. Bugün yalnızca adım, su, öğün ve germe. Sırada bekleyen: ${isi.baslik}.")
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

        Bosluk()
        CizgiDugme("Planımı hatırla", Modifier.fillMaxWidth()) { planDialog = true }

        Bosluk(8.dp)
        Cizgi()
        AnahtarSatiri(if (dunSecili) "Dün uçuş günüydü" else "Bugün uçuş günü", gun.ucusGunu) { yeni -> vm.gunDegistir { it.copy(ucusGunu = yeni) } }
        Cizgi()
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Son 7 günde ağırlık seansı", style = MaterialTheme.typography.titleMedium)
                val gunler = plan?.planGunleri?.split(",")?.mapNotNull { it.toLongOrNull() }.orEmpty()
                if (gunler.isNotEmpty()) {
                    Text(
                        "Bu haftanın planı: " + gunler.sorted().joinToString(" · ") { Bicim.gunAdi(it) } + if (gercekBugun in gunler) " (bugün)" else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text("$son7/${a.haftalikSeansHedef}", style = MaterialTheme.typography.displaySmall)
        }

        Bolum("Uyku")
        UykuGirisi(gun, dunSecili) { yeni -> vm.gunDegistir { it.copy(uykuSaat = yeni) } }
        val uyku = gun.uykuSaat
        val agirlikYapildi = yapilanlar.any { it.tur != Tur.KARDIYO }
        if (uyku != null && uyku < 6.0 && dunUcus && !gun.ucusGunu && isi.agirlik && !agirlikYapildi && !gun.ertelendi && !yarinIcin) {
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
        saatKalorisi[bugun]?.let { kcal ->
            // Saatin yakım tahmini kabadır: yalnızca bilgi. Kalori hakkına eklenmez.
            Text(
                "Saat: ${Bicim.tam(kcal)} kcal aktif yakım (bilgi içindir; kalori hakkına eklenmez).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
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
            baslik = if (dunSecili) "Dünkü adım" else "Bugünkü adım", onKapat = { adimDialog = false },
            alanlar = listOf(Alan("Adım sayısı", if (gun.adim > 0) gun.adim.toString() else "", AlanTuru.TAM)),
            dogrula = { if (Bicim.cozTam(it[0]) == null) "Bir sayı gir." else null },
        ) { d -> vm.gunDegistir { it.copy(adim = Bicim.cozTam(d[0]) ?: it.adim) } }
    }
    if (atlaDialog) {
        OnayDialog(
            "Bu adımı atla", "${isi.baslik} yapılmadan sıradaki işe (${Sira.isAt(a.siraIndex + 1).baslik}) geçilecek.", "Atla",
            onKapat = { atlaDialog = false },
        ) { vm.siraAtla() }
    }
    if (planDialog) {
        BilgiDialog(
            "Eğer-o zaman planlarım", planlar.map { it.metin }, onKapat = { planDialog = false },
            bosMetin = "Henüz plan yok. Ayarlar › Eğer-o zaman planlarım bölümünden ekleyebilirsin.",
        )
    }
}

private fun seansAdi(tur: String): String = when (tur) {
    Tur.A -> "Seans A"
    Tur.B -> "Seans B"
    else -> "Kardiyo"
}

@Composable
private fun SiradakiIs(
    isi: Is, a: Ayarlar, seansAcik: Boolean, hareketSayisi: Int,
    onBasla: (kisa: Boolean) -> Unit, onKardiyo: () -> Unit, onAtla: () -> Unit,
) {
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
                "Antrenman yok. Hedef ${Bicim.tam(a.adimMin)}–${Bicim.tam(a.adimMax)} adım. Gün bitince sıra kendiliğinden ilerler.",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    // Yoğun uçuş haftasında ağırlık seansı bloke olmasın: Kardiyo ve Adım atlanabilir (ağırlık seansı atlanmaz).
    if (Sira.atlanabilir(isi)) MetinDugme("Bu adımı atla", onClick = onAtla)
}

/** "Dün gece kaç saat uyudun?" : yarım saatlik adımlarla − / +. */
@Composable
private fun UykuGirisi(gun: GunKaydi, dunSecili: Boolean, onDegis: (Double) -> Unit) {
    Text(if (dunSecili) "Önceki gece kaç saat uyudun?" else "Dün gece kaç saat uyudun?", style = MaterialTheme.typography.titleMedium)
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
