package com.zincir.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zincir.AnaViewModel
import com.zincir.Dinlenme
import com.zincir.mantik.Bicim
import com.zincir.mantik.Ilerleme
import com.zincir.mantik.Program
import com.zincir.mantik.Saglik
import com.zincir.mantik.SetDegeri
import com.zincir.mantik.Sira
import com.zincir.veri.Ayarlar
import com.zincir.veri.BaslangicVerisi
import com.zincir.veri.Hareket
import com.zincir.veri.Seans
import com.zincir.veri.SetKaydi
import com.zincir.veri.Tur
import kotlinx.coroutines.delay
import kotlin.math.ceil

/** Antrenman sekmesi: açık bir seans varsa onu, yoksa seans seçimini gösterir. */
@Composable
fun AntrenmanEkrani(vm: AnaViewModel) {
    val a = vm.ayarlar.collectAsStateWithLifecycle().value ?: return
    val aktif by vm.aktifSeans.collectAsStateWithLifecycle()
    val seans = aktif
    if (seans != null) AktifSeans(vm, a, seans) else AntrenmanOzeti(vm, a)
}

private const val ISINMA = "5 dk yürüyüş. İlk hareketi hafif ağırlıkla 2 set yap."

private fun haftaNotu(haftaIndex: Int): String =
    if (Program.alismaDonemi(haftaIndex)) "İlk 2 hafta: her hareket 2 set, rahat ağırlık."
    else "Her sette 1–2 tekrar yedekte bırak."

@Composable
private fun AntrenmanOzeti(vm: AnaViewModel, a: Ayarlar) {
    val bugun by vm.bugun.collectAsStateWithLifecycle()
    val dunSecili by vm.dunSecili.collectAsStateWithLifecycle()
    val hareketler by vm.hareketler.collectAsStateWithLifecycle()
    val seanslar by vm.seanslar.collectAsStateWithLifecycle()
    val sonAgirlik by vm.sonAgirlikGunu.collectAsStateWithLifecycle()
    val saatEgzersizleri by vm.saatEgzersizleri.collectAsStateWithLifecycle()
    val haftaIndex = Program.haftaIndex(a.baslangicGunu, bugun)
    val siradaki = Sira.isAt(a.siraIndex)
    val hafif = a.hafifHaftaIndex == haftaIndex

    var kardiyoTuru by rememberSaveable { mutableStateOf(BaslangicVerisi.KARDIYO_TURLERI[0]) }
    var kardiyoSure by rememberSaveable { mutableStateOf("30") }
    var silinecek by remember { mutableStateOf<Seans?>(null) }

    Sayfa {
        EkranBasligi("Antrenman", ust = if (haftaIndex >= 0) "Hafta ${haftaIndex + 1}" else "Program başlamadı")
        if (dunSecili) {
            Bosluk()
            Not("Kayıtlar düne (${Bicim.tarih(bugun)}) yazılıyor. Değiştirmek için Bugün sekmesinin üstündeki geçişi kullan.")
        }
        Bosluk(16.dp)
        Not(ISINMA, baslik = "Isınma")
        Bosluk()
        Not(haftaNotu(haftaIndex))
        if (Ilerleme.kesintidenDonus(sonAgirlik, bugun)) {
            Bosluk()
            Not(
                "Son ağırlık seansından beri ${bugun - (sonAgirlik ?: bugun)} gün geçti. Önerilen ağırlıklar son değerlerin %80'i olacak.",
                baslik = "Kesintiden dönüş",
            )
        }
        if (Program.hafifHaftaOnerilir(haftaIndex) || hafif) {
            Bosluk()
            Not("8 haftada bir hafif hafta: set sayısı yarıya iner, ağırlıklar aynı kalır.", baslik = "Hafif hafta önerisi")
            AnahtarSatiri("Bu hafta hafif hafta uygula", hafif) { yeni ->
                vm.ayarlariDegistir { it.copy(hafifHaftaIndex = if (yeni) haftaIndex else -1) }
            }
        }

        for (tur in listOf(Tur.A, Tur.B)) {
            val sirada = siradaki.kod == tur
            Bolum("Seans $tur" + if (sirada) " · sıradaki" else "")
            hareketler.filter { it.seans == tur }.forEachIndexed { i, h ->
                Text(
                    "${i + 1}  ${h.ad}  ·  ${setOzeti(h, Program.setSayisi(h.setSayisi, haftaIndex, hafif))}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
            Bosluk()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sirada) DoluDugme("Başlat", Modifier.weight(1f)) { vm.seansBaslat(tur, false) }
                else CizgiDugme("Başlat", Modifier.weight(1f)) { vm.seansBaslat(tur, false) }
                CizgiDugme("15 dakika", Modifier.weight(1f)) { vm.seansBaslat(tur, true) }
            }
        }

        Bolum("Kardiyo" + if (siradaki.kod == Tur.KARDIYO) " · sıradaki" else "")
        // Saatte kaydedilmiş, henüz buraya işlenmemiş kardiyo varsa tek dokunuşla kaydetmeyi öner.
        // Kendiliğinden kaydedilmez: sıra ancak sen onaylayınca ilerler.
        val oneriler = Saglik.kardiyoOnerileri(
            saatEgzersizleri[bugun].orEmpty(),
            seanslar.filter { it.gun == bugun && it.tur == Tur.KARDIYO && it.tamamlandi }.mapNotNull { it.sureDk },
        )
        oneriler.forEach { e ->
            Not("Saatinde ${if (dunSecili) "dün" else "bugün"} ${e.sureDk} dk ${e.tur} kaydı var.", baslik = "Saatten öneri")
            Bosluk(8.dp)
            DoluDugme("${e.sureDk} dk ${e.tur} · kardiyo olarak kaydet", Modifier.fillMaxWidth()) { vm.kardiyoKaydet(e.tur, e.sureDk) }
            Bosluk(16.dp)
        }
        if (oneriler.isNotEmpty()) Etiket("Ya da elle gir")
        Text("Hedef 30–40 dk.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Bosluk()
        Secim(BaslangicVerisi.KARDIYO_TURLERI, kardiyoTuru) { kardiyoTuru = it }
        Bosluk()
        SayiGirisi(kardiyoSure, { kardiyoSure = it }, "Süre (dk)", adim = 5.0)
        Bosluk()
        val sure = Bicim.cozTam(kardiyoSure)
        DoluDugme("Kardiyoyu kaydet", Modifier.fillMaxWidth(), etkin = sure != null && sure > 0) {
            vm.kardiyoKaydet(kardiyoTuru, sure ?: 0)
        }

        val bitenler = seanslar.filter { it.tamamlandi }.take(8)
        if (bitenler.isNotEmpty()) {
            Bolum("Son seanslar")
            bitenler.forEach { s ->
                ListeSatiri(
                    baslik = if (s.tur == Tur.KARDIYO) "Kardiyo · ${s.kardiyoTuru.orEmpty()}" else "Seans ${s.tur}" + if (s.kisaMod) " · 15 dk modu" else "",
                    alt = Bicim.tarih(s.gun),
                    sag = s.sureDk?.let { "$it dk" },
                    onClick = { silinecek = s },
                )
            }
        }
    }

    silinecek?.let { s ->
        OnayDialog(
            "Kaydı sil", "${Bicim.tarih(s.gun)} tarihli kayıt silinecek. Sıra geri alınmaz.", "Sil",
            onKapat = { silinecek = null },
        ) { vm.seansSil(s.id) }
    }
}

private fun aralik(h: Hareket): String = if (h.tekrarMin == h.tekrarMax) "${h.tekrarMin}" else "${h.tekrarMin}–${h.tekrarMax}"

/** "3 × 8–12", "3 × 30–45 sn", "2 × 10 her bacak" */
private fun setOzeti(h: Hareket, setSayisi: Int): String =
    "$setSayisi × ${aralik(h)}" + (if (h.birim == "sn") " sn" else "") + (if (h.ek.isNotBlank()) " ${h.ek}" else "")

private fun setYazisi(h: Hareket, kg: Double, tekrar: Int): String = when {
    h.agirlikli -> "${Bicim.ondalik(kg)} kg × $tekrar"
    h.birim == "sn" -> "$tekrar sn"
    else -> "$tekrar tekrar"
}

@Composable
private fun AktifSeans(vm: AnaViewModel, a: Ayarlar, seans: Seans) {
    EkraniAcikTut()
    ZamanlayiciIzinleri() // dinlenme bitişinde kilitliyken de haber verebilmek için (ilk kullanımda sorar)
    val bugun by vm.bugun.collectAsStateWithLifecycle()
    val dunSecili by vm.dunSecili.collectAsStateWithLifecycle()
    val hareketler by vm.hareketler.collectAsStateWithLifecycle()
    val setler by vm.aktifSetler.collectAsStateWithLifecycle()
    val sonAgirlik by vm.sonAgirlikGunu.collectAsStateWithLifecycle()
    val dinlenme by vm.dinlenme.collectAsStateWithLifecycle()

    val haftaIndex = Program.haftaIndex(a.baslangicGunu, bugun)
    val hafif = a.hafifHaftaIndex == haftaIndex
    val donus = Ilerleme.kesintidenDonus(sonAgirlik, bugun)
    val liste = hareketler.filter { it.seans == seans.tur }.let { if (seans.kisaMod) it.take(2) else it }
    fun hedefSet(h: Hareket) = Program.setSayisi(h.setSayisi, haftaIndex, hafif)
    val eksikVar = liste.any { h -> setler.count { it.hareketId == h.id } < hedefSet(h) }

    var bitirDialog by remember { mutableStateOf(false) }
    var iptalDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Sayfa(Modifier.weight(1f)) {
            EkranBasligi("Seans ${seans.tur}", ust = if (seans.kisaMod) "15 dakika modu · ilk 2 hareket" else gunBasligi(bugun, dunSecili))
            Bosluk(16.dp)
            Not(ISINMA, baslik = "Isınma")
            Bosluk()
            Not(if (hafif && !Program.alismaDonemi(haftaIndex)) "Hafif hafta: set sayısı yarıya indi." else haftaNotu(haftaIndex))

            liste.forEachIndexed { i, h ->
                key(h.id) {
                    HareketBolumu(
                        no = i + 1, toplam = liste.size, h = h, seans = seans,
                        setler = setler.filter { it.hareketId == h.id }, setSayisi = hedefSet(h), donus = donus, vm = vm,
                    )
                }
            }

            Bolum("Bitir")
            DoluDugme("Seansı bitir", Modifier.fillMaxWidth(), etkin = setler.isNotEmpty()) {
                if (eksikVar) bitirDialog = true else vm.seansBitir(seans.id)
            }
            MetinDugme("Seansı iptal et") { iptalDialog = true }
        }
        dinlenme?.let { DinlenmeCubugu(it, onGec = vm::dinlenmeGec) }
    }

    if (bitirDialog) {
        OnayDialog("Eksik setler var", "Yine de seansı bitirmek istiyor musun? Seans sayılır.", "Bitir", onKapat = { bitirDialog = false }) {
            vm.seansBitir(seans.id)
        }
    }
    if (iptalDialog) {
        OnayDialog("Seansı iptal et", "Bu seansta kaydedilen setler silinecek.", "İptal et", onKapat = { iptalDialog = false }) {
            vm.seansSil(seans.id)
        }
    }
}

/** Tek hareket: başlık, öneriler, kaydedilen setler ve sıradaki setin girişi. */
@Composable
private fun HareketBolumu(
    no: Int, toplam: Int, h: Hareket, seans: Seans, setler: List<SetKaydi>, setSayisi: Int, donus: Boolean, vm: AnaViewModel,
) {
    var alternatif by rememberSaveable { mutableStateOf(false) }
    // İlk set kaydedildikten sonra hareketin adı değişmez: kayıt o adla tutulur.
    val kayitliAd = setler.firstOrNull()?.hareketAdi
    val ad = kayitliAd ?: if (alternatif && h.alternatifAd != null) h.alternatifAd else h.ad
    val onceki by produceState(emptyList<SetDegeri>(), ad, seans.id) {
        value = vm.dao.oncekiSetler(ad, seans.id).map { SetDegeri(it.kg, it.tekrar) }
    }
    val oneri = remember(onceki, setSayisi, donus) { Ilerleme.oner(onceki, setSayisi, h.tekrarMin, h.tekrarMax, donus) }
    val birim = if (h.birim == "sn") "sn" else "tekrar"

    Bolum("Hareket $no / $toplam")
    Text(ad, style = MaterialTheme.typography.headlineMedium)
    Text(
        "${setOzeti(h, setSayisi)} · dinlenme ${Bicim.sure(h.dinlenmeSn)}",
        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (h.alternatifAd != null && kayitliAd == null) {
        AnahtarSatiri("Makine yok", alternatif, alt = "Yerine: ${h.alternatifAd}") { alternatif = it }
    }
    if (oneri.agirlikArtir) {
        Bosluk(8.dp)
        Not(
            "Geçen seans tüm setlerde ${h.tekrarMax} $birim tamamlandı.",
            baslik = if (h.agirlikli) "Ağırlığı artır, ${h.tekrarMin} tekrardan başla." else "Üst sınıra ulaştın: daha zor bir çeşidini dene.",
        )
    }
    if (oneri.donus) {
        Bosluk(8.dp)
        Not("Kesintiden dönüş: son değerlerin %80'i yazıldı.")
    }
    if (onceki.isNotEmpty()) {
        Bosluk(8.dp)
        Text(
            "Önceki: " + onceki.joinToString("  ·  ") { setYazisi(h, it.kg, it.tekrar) },
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Bosluk(8.dp)
    setler.forEach { s ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Set ${s.setNo}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(setYazisi(h, s.kg, s.tekrar) + "  ✓", style = MaterialTheme.typography.titleLarge)
        }
        Cizgi()
    }

    if (setler.size < setSayisi) {
        val setNo = setler.size + 1
        val varsayilan = oneri.setler.getOrElse(setler.size) { oneri.setler.last() }
        // Bu seansta bir set yapıldıysa sıradaki set aynı ağırlıkla başlar.
        val kgVarsayilan = setler.lastOrNull()?.kg ?: varsayilan.kg
        var kg by remember(setNo, oneri, ad) { mutableStateOf(Bicim.giris(kgVarsayilan)) }
        var tekrar by remember(setNo, oneri, ad) { mutableStateOf(varsayilan.tekrar.toString()) }
        val kgDeger = if (h.agirlikli) Bicim.coz(kg) else 0.0
        val tekrarDeger = Bicim.cozTam(tekrar)

        Bosluk()
        Etiket("Set $setNo / $setSayisi")
        Bosluk(8.dp)
        if (h.agirlikli) {
            SayiGirisi(kg, { kg = it }, "kg", adim = 1.0, ondalik = true)
            Bosluk(8.dp)
        }
        SayiGirisi(tekrar, { tekrar = it }, birim, adim = if (h.birim == "sn") 5.0 else 1.0)
        Bosluk()
        DoluDugme("Set $setNo kaydet", Modifier.fillMaxWidth(), etkin = kgDeger != null && tekrarDeger != null && tekrarDeger > 0) {
            vm.setKaydet(seans, h, ad, setNo, kgDeger ?: 0.0, tekrarDeger ?: 0)
        }
    } else {
        Bosluk(8.dp)
        Text("Hareket tamam.", style = MaterialTheme.typography.titleMedium)
    }
    if (setler.isNotEmpty()) MetinDugme("Son seti sil") { vm.setSil(setler.last()) }
}

/** Ekranın altında duran dinlenme geri sayımı. Bitince telefon titrer (ViewModel yapar). */
@Composable
private fun DinlenmeCubugu(d: Dinlenme, onGec: () -> Unit) {
    fun kalanSn() = ceil((d.bitis - SystemClock.elapsedRealtime()) / 1000.0).toInt().coerceAtLeast(0)
    var kalan by remember(d) { mutableIntStateOf(kalanSn()) }
    LaunchedEffect(d) {
        while (true) {
            kalan = kalanSn()
            delay(200)
        }
    }
    val zemin = MaterialTheme.colorScheme.onBackground
    val yazi = MaterialTheme.colorScheme.background
    Row(Modifier.fillMaxWidth().background(zemin).padding(horizontal = Kenar, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Etiket("Dinlenme", renk = yazi)
            Text(Bicim.sayac(kalan), style = MaterialTheme.typography.displayMedium, color = yazi)
        }
        Box(
            Modifier.defaultMinSize(minWidth = 72.dp, minHeight = 56.dp).clickable(role = Role.Button, onClick = onGec),
            contentAlignment = Alignment.Center,
        ) { Text("Geç", style = MaterialTheme.typography.titleLarge, color = yazi, textDecoration = TextDecoration.Underline) }
    }
}
