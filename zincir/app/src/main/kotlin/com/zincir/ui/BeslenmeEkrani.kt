package com.zincir.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zincir.AnaViewModel
import com.zincir.mantik.Bicim
import com.zincir.veri.BaslangicVerisi
import com.zincir.veri.Kacamak
import com.zincir.veri.Ogun
import com.zincir.veri.YemekKaydi
import com.zincir.veri.Yiyecek
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/** Beslenme sekmesi: planlı öğünler, hızlı ekleme, haftalık kaçamak sayacı, bilgi kartları. */
@Composable
fun BeslenmeEkrani(vm: AnaViewModel) {
    val a = vm.ayarlar.collectAsStateWithLifecycle().value ?: return
    val bugun by vm.bugun.collectAsStateWithLifecycle()
    val ogunler by vm.ogunler.collectAsStateWithLifecycle()
    val yemekler by vm.yemekler.collectAsStateWithLifecycle()
    val yiyecekler by vm.yiyecekler.collectAsStateWithLifecycle()
    val kacamaklar by vm.kacamaklar.collectAsStateWithLifecycle()
    val kapsam = rememberCoroutineScope()

    val kcal = yemekler.sumOf { it.kcal }
    val protein = yemekler.sumOf { it.protein }
    val yenenOgunler = yemekler.mapNotNull { it.ogunId }.toSet()
    val payIdleri = ogunler.filter { it.pay }.map { it.id }.toSet()

    var hizliEkle by remember { mutableStateOf(false) }
    var kacamakDialog by remember { mutableStateOf(false) }
    var silinecekYemek by remember { mutableStateOf<YemekKaydi?>(null) }
    var silinecekKacamak by remember { mutableStateOf<Kacamak?>(null) }

    // Öğün arası uyarısı için saat; yarım dakikada bir yenilenir.
    var simdi by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            simdi = System.currentTimeMillis()
            delay(30_000)
        }
    }

    Sayfa {
        EkranBasligi("Beslenme", ust = Bicim.tarih(bugun))
        Bosluk(8.dp)
        HedefSatiri("Kalori", Bicim.tam(kcal.toInt()), "${Bicim.tam(a.kaloriHedef)} kcal", (kcal / a.kaloriHedef).toFloat())
        HedefSatiri("Protein", Bicim.tam(protein.toInt()), "${a.proteinMin}–${a.proteinMax} g", (protein / a.proteinMin).toFloat())

        // Öğünler arası süre aşıldıysa (ayar açıksa ve bugün yenecek öğün kaldıysa).
        val sonYemek = yemekler.filter { it.ogunId == null || it.ogunId !in payIdleri }.maxOfOrNull { it.zaman }
        val kalanOgun = ogunler.any { !it.pay && it.id !in yenenOgunler }
        if (a.bildirimOgun && sonYemek != null && kalanOgun) {
            val dakika = (simdi - sonYemek) / 60_000
            if (dakika >= a.ogunAralikSaat * 60) {
                Bosluk()
                Uyari("Öğün zamanı", "Son öğünden beri ${dakika / 60} sa ${dakika % 60} dk geçti.")
            }
        }

        Bolum("Öğünler")
        Text(
            "Gramlar pişmiş ağırlıktır; yulaf ve whey kuru tartılır.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        var no = 0
        ogunler.forEach { o ->
            if (!o.pay) no++
            OgunSatiri(o, no, isaretli = o.id in yenenOgunler) { vm.ogunIsaretle(o, it) }
        }

        Bolum("Plandan saptıysan")
        CizgiDugme("Hızlı ekle (gram gir)", Modifier.fillMaxWidth()) { hizliEkle = true }
        yemekler.filter { it.ogunId == null }.forEach { y ->
            ListeSatiri(
                baslik = y.ad + (y.gram?.let { " · ${Bicim.ondalik(it)} g" } ?: ""),
                alt = "${Bicim.tam(y.kcal.toInt())} kcal · ${Bicim.ondalik(y.protein)} g protein",
                onClick = { silinecekYemek = y },
            )
        }

        Bolum("Haftada en fazla 1")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Biryani, paratha, kızartma, şerbetli tatlı", style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Text(
                "${kacamaklar.size}/1", style = MaterialTheme.typography.displaySmall,
                color = if (kacamaklar.size > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            )
        }
        if (kacamaklar.size > 1) {
            Bosluk(8.dp)
            Uyari("Bu hafta sınır aşıldı.", "Sonraki öğün plandaki öğündür.")
        }
        Bosluk()
        CizgiDugme("Kaçamak ekle", Modifier.fillMaxWidth()) { kacamakDialog = true }
        kacamaklar.forEach { k -> ListeSatiri(k.ad, alt = Bicim.tarih(k.gun), onClick = { silinecekKacamak = k }) }

        Bolum("Tabak kuralı")
        BilgiSatiri("Protein", "2 avuç içi · 150–170 g")
        BilgiSatiri("Pilav", "1 yumruk · 150 g")
        BilgiSatiri("Sebze", "yarım tabak · 150–200 g")
        BilgiSatiri("Yağ", "1 başparmak · 10 g")

        Bolum("Yasaklar")
        listOf("Çiğ salata", "Buz", "Sokak yemeği", "Şekerli içecek").forEach {
            Text(it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 6.dp))
            Cizgi()
        }

        Bolum("Uçuş günü kuralı")
        BilgiSatiri("Otelde", "Tam öğünle çık.")
        BilgiSatiri("Çantada", "30 g whey + 1 muz + 30 g fıstık")
        BilgiSatiri("Uçakta", "Ekip yemeğinin et ve sebzesini ye.")
    }

    if (hizliEkle) {
        HizliEkleDialog(
            yiyecekler = yiyecekler,
            onKapat = { hizliEkle = false },
            onEkle = { y, gram -> vm.hizliEkle(y, gram) },
            onYeni = { y -> kapsam.launch { vm.dao.yiyecekYaz(y) } },
        )
    }
    if (kacamakDialog) {
        FormDialog(
            baslik = "Kaçamak ekle", onKapat = { kacamakDialog = false }, kaydetMetni = "Ekle",
            aciklama = "Haftada en fazla 1. Kalorisi ayrıca sayılmaz; yalnızca sayaç tutulur.",
            alanlar = listOf(Alan("Ne yedin?", BaslangicVerisi.KACAMAKLAR[0], AlanTuru.SECIM, BaslangicVerisi.KACAMAKLAR)),
        ) { d -> vm.kacamakEkle(d[0]) }
    }
    silinecekYemek?.let { y ->
        OnayDialog("Kaydı sil", "${y.ad} bugünkü toplamdan çıkarılacak.", "Sil", onKapat = { silinecekYemek = null }) {
            kapsam.launch { vm.dao.yemekSil(y) }
        }
    }
    silinecekKacamak?.let { k ->
        OnayDialog("Kaydı sil", "${k.ad} haftalık sayaçtan çıkarılacak.", "Sil", onKapat = { silinecekKacamak = null }) {
            kapsam.launch { vm.dao.kacamakSil(k) }
        }
    }
}

@Composable
private fun OgunSatiri(o: Ogun, no: Int, isaretli: Boolean, onIsaret: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp)) {
        Etiket(if (o.pay) "Pay" else "Öğün $no")
        Text(o.ad, style = MaterialTheme.typography.headlineSmall)
        Bosluk(4.dp)
        Text(o.icerik, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Bosluk(6.dp)
        Text("${Bicim.tam(o.kcal)} kcal · ${o.protein} g protein", style = MaterialTheme.typography.titleSmall)
        Bosluk()
        if (isaretli) {
            CizgiDugme("✓ " + (if (o.pay) "Eklendi" else "Yedim") + " · geri al", Modifier.fillMaxWidth()) { onIsaret(false) }
        } else {
            DoluDugme(if (o.pay) "Bugüne ekle" else "Planlandığı gibi yedim", Modifier.fillMaxWidth()) { onIsaret(true) }
        }
    }
    Cizgi()
}

@Composable
private fun BilgiSatiri(sol: String, sag: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(sol, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(96.dp))
        Text(sag, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
    Cizgi()
}

/** Hızlı ekleme: önce yiyecek seçilir, sonra gram girilir. Listeye yeni yiyecek de eklenebilir. */
@Composable
private fun HizliEkleDialog(yiyecekler: List<Yiyecek>, onKapat: () -> Unit, onEkle: (Yiyecek, Double) -> Unit, onYeni: (Yiyecek) -> Unit) {
    var secili by remember { mutableStateOf<Yiyecek?>(null) }
    var gram by remember { mutableStateOf("") }
    var yeniDialog by remember { mutableStateOf(false) }
    val y = secili

    if (yeniDialog) {
        FormDialog(
            baslik = "Yeni yiyecek", onKapat = { yeniDialog = false },
            aciklama = "Örnek: 100 g = 165 kcal, 31 g protein.",
            alanlar = listOf(
                Alan("Ad", ""), Alan("Ölçü (gram)", "100", AlanTuru.ONDALIK),
                Alan("Bu ölçüde kalori (kcal)", "", AlanTuru.ONDALIK), Alan("Bu ölçüde protein (g)", "", AlanTuru.ONDALIK),
            ),
            dogrula = { d ->
                val g = Bicim.coz(d[1])
                when {
                    d[0].isBlank() -> "Ad boş olamaz."
                    g == null || g <= 0 || Bicim.coz(d[2]) == null || Bicim.coz(d[3]) == null -> "Sayıları kontrol et."
                    else -> null
                }
            },
        ) { d -> onYeni(Yiyecek(ad = d[0].trim(), bazGram = Bicim.coz(d[1])!!, kcal = Bicim.coz(d[2])!!, protein = Bicim.coz(d[3])!!)) }
        return
    }

    AlertDialog(
        onDismissRequest = onKapat, shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp,
        title = { Text(y?.ad ?: "Hızlı ekle", style = MaterialTheme.typography.headlineSmall) },
        text = {
            if (y == null) {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                    yiyecekler.forEach { yiyecek ->
                        Column(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable {
                                secili = yiyecek
                                gram = Bicim.giris(yiyecek.bazGram)
                            }.padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(yiyecek.ad, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${Bicim.ondalik(yiyecek.bazGram)} g = ${Bicim.ondalik(yiyecek.kcal)} kcal, ${Bicim.ondalik(yiyecek.protein)} g protein",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Cizgi()
                    }
                    MetinDugme("+ Yeni yiyecek ekle") { yeniDialog = true }
                }
            } else {
                val g = Bicim.coz(gram)
                Column {
                    SayiGirisi(gram, { gram = it }, "gram", adim = 10.0, ondalik = true)
                    Bosluk(16.dp)
                    if (g != null && g > 0) {
                        val oran = g / y.bazGram
                        Text("${Bicim.tam((y.kcal * oran).toInt())} kcal", style = MaterialTheme.typography.displaySmall)
                        Text("${Bicim.ondalik(y.protein * oran)} g protein", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        },
        confirmButton = {
            if (y != null) {
                val g = Bicim.coz(gram)
                TextButton(enabled = g != null && g > 0, onClick = { onEkle(y, g!!); onKapat() }) {
                    Text("Ekle", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        dismissButton = {
            Row {
                if (y != null) TextButton(onClick = { secili = null }) { Text("Geri", style = MaterialTheme.typography.labelLarge) }
                TextButton(onClick = onKapat) { Text("Vazgeç", style = MaterialTheme.typography.labelLarge) }
            }
        },
    )
}
