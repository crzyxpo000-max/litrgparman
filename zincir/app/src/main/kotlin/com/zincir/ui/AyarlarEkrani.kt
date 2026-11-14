package com.zincir.ui

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zincir.AnaViewModel
import com.zincir.bildirim.Bildirimler
import com.zincir.mantik.Bicim
import com.zincir.mantik.Kilo
import com.zincir.mantik.Sira
import com.zincir.saglik.SaglikBaglantisi
import com.zincir.veri.Ayarlar
import com.zincir.veri.EgerPlan
import com.zincir.veri.Germe
import com.zincir.veri.Hareket
import com.zincir.veri.Ogun
import com.zincir.veri.Yiyecek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

private const val GUN_MS = 86_400_000L

/** Ayarlar: program, hedefler, içerik düzenleme, bildirimler, görünüm, yedekleme. */
@Composable
fun AyarlarEkrani(vm: AnaViewModel, onGeri: () -> Unit) {
    val a = vm.ayarlar.collectAsStateWithLifecycle().value ?: return
    // Alt sayfa: null = ana ayarlar; "hareket", "ogun", "germe", "yiyecek", "plan" = düzenleme listeleri.
    var altSayfa by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = altSayfa != null) { altSayfa = null }

    when (altSayfa) {
        "hareket" -> HareketleriDuzenle(vm) { altSayfa = null }
        "ogun" -> OgunleriDuzenle(vm, a) { altSayfa = null }
        "germe" -> GermeleriDuzenle(vm) { altSayfa = null }
        "yiyecek" -> YiyecekleriDuzenle(vm) { altSayfa = null }
        "plan" -> PlanlariDuzenle(vm) { altSayfa = null }
        else -> AnaAyarlar(vm, a, onGeri) { altSayfa = it }
    }
}

@Composable
private fun AnaAyarlar(vm: AnaViewModel, a: Ayarlar, onGeri: () -> Unit, onAltSayfa: (String) -> Unit) {
    val context = LocalContext.current
    val kapsam = rememberCoroutineScope()
    // Açık pencere: satıra dokununca buraya bir pencere konur.
    var pencere by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    val kapat = { pencere = null }
    var yedekDurumu by remember { mutableStateOf<String?>(null) }
    var bekleyenYedek by remember { mutableStateOf<String?>(null) }

    /** Tek sayılık ayar penceresi. */
    fun sayiPenceresi(
        baslik: String, etiket: String, deger: String, ondalik: Boolean = false, aciklama: String? = null,
        kural: (Double) -> String? = { null }, kaydet: (Ayarlar, Double) -> Ayarlar,
    ) {
        pencere = {
            FormDialog(
                baslik = baslik, aciklama = aciklama, onKapat = kapat,
                alanlar = listOf(Alan(etiket, deger, if (ondalik) AlanTuru.ONDALIK else AlanTuru.TAM)),
                dogrula = { d -> Bicim.coz(d[0])?.let { if (it <= 0) "Sıfırdan büyük olmalı." else kural(it) } ?: "Bir sayı gir." },
            ) { d -> vm.ayarlariDegistir { kaydet(it, Bicim.coz(d[0])!!) } }
        }
    }

    /** Alt ve üst sınırlı ayar penceresi. */
    fun aralikPenceresi(baslik: String, alt: String, ust: String, ondalik: Boolean = false, kaydet: (Ayarlar, Double, Double) -> Ayarlar) {
        val tur = if (ondalik) AlanTuru.ONDALIK else AlanTuru.TAM
        pencere = {
            FormDialog(
                baslik = baslik, onKapat = kapat,
                alanlar = listOf(Alan("Alt sınır", alt, tur), Alan("Üst sınır", ust, tur)),
                dogrula = { d ->
                    val x = Bicim.coz(d[0])
                    val y = Bicim.coz(d[1])
                    when {
                        x == null || y == null || x <= 0 -> "Sayıları kontrol et."
                        x > y -> "Alt sınır üst sınırdan büyük olamaz."
                        else -> null
                    }
                },
            ) { d -> vm.ayarlariDegistir { kaydet(it, Bicim.coz(d[0])!!, Bicim.coz(d[1])!!) } }
        }
    }

    // ---- Bildirim izni (Android 13 ve sonrası sorar)
    // İzin verilmezse aşağıdaki "Bildirim izni kapalı" uyarısı görünür; burada ayrıca bir şey yapmaya gerek yok.
    val izinIste = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun bildirimAc(degisim: (Ayarlar) -> Ayarlar) {
        if (Build.VERSION.SDK_INT >= 33 && !Bildirimler.izinVar(context)) izinIste.launch(Manifest.permission.POST_NOTIFICATIONS)
        vm.ayarlariDegistir(degisim)
    }
    fun saatPenceresi(baslik: String, dakika: Int, kaydet: (Ayarlar, Int) -> Ayarlar) {
        pencere = {
            FormDialog(
                baslik = baslik, onKapat = kapat, aciklama = "24 saat biçiminde yaz. Örnek: 07:30",
                alanlar = listOf(Alan("Saat", Bicim.saat(dakika))),
                dogrula = { d -> if (Bicim.cozSaat(d[0]) == null) "Saati 07:30 gibi yaz." else null },
            ) { d -> vm.ayarlariDegistir { kaydet(it, Bicim.cozSaat(d[0])!!) } }
        }
    }

    // ---- Yedekleme
    val dosyaAdi = "zincir-yedek-${LocalDate.now()}.json"
    val dosyayaKaydet = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) kapsam.launch {
            yedekDurumu = try {
                val json = vm.yedekJson()
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } }
                "Yedek kaydedildi."
            } catch (e: Exception) {
                "Yedek kaydedilemedi: ${e.message}"
            }
        }
    }
    val dosyaAc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) kapsam.launch {
            try {
                bekleyenYedek = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                }
            } catch (e: Exception) {
                yedekDurumu = "Dosya okunamadı: ${e.message}"
            }
        }
    }
    fun paylas() = kapsam.launch {
        yedekDurumu = try {
            val json = vm.yedekJson()
            val dosya = withContext(Dispatchers.IO) {
                File(context.cacheDir, "yedek").apply { mkdirs() }.resolve(dosyaAdi).also { it.writeText(json) }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.dosya", dosya)
            val niyet = Intent(Intent.ACTION_SEND).setType("application/json")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            niyet.clipData = ClipData.newRawUri(dosyaAdi, uri)
            context.startActivity(Intent.createChooser(niyet, "Yedeği paylaş"))
            null
        } catch (e: Exception) {
            "Yedek paylaşılamadı: ${e.message}"
        }
    }

    Sayfa {
        MetinDugme("‹ Geri", onClick = onGeri)
        EkranBasligi("Ayarlar")

        Bolum("Program", ust = 20.dp)
        ListeSatiri("Program başlangıcı", sag = Bicim.tamTarih(a.baslangicGunu)) {
            pencere = { TarihPenceresi(a.baslangicGunu, kapat) { vm.baslangicGununuDegistir(it) } }
        }
        ListeSatiri("Başlangıç kilosu", sag = "${Bicim.ondalik(a.baslangicKilo)} kg") {
            sayiPenceresi("Başlangıç kilosu", "kg", Bicim.giris(a.baslangicKilo), ondalik = true) { x, v -> x.copy(baslangicKilo = v) }
        }
        ListeSatiri("Hedef kilo", sag = "${Bicim.ondalik(a.hedefKiloAlt)}–${Bicim.ondalik(a.hedefKiloUst)} kg") {
            aralikPenceresi("Hedef kilo (kg)", Bicim.giris(a.hedefKiloAlt), Bicim.giris(a.hedefKiloUst), ondalik = true) { x, alt, ust ->
                x.copy(hedefKiloAlt = alt, hedefKiloUst = ust)
            }
        }

        ListeSatiri("Sıradaki iş", alt = "Sıra kaydıysa buradan düzelt", sag = Sira.isAt(a.siraIndex).baslik) {
            val adimlar = Sira.DONGU.mapIndexed { i, isi -> "${i + 1} · ${isi.baslik}" }
            pencere = {
                ListeDialog("Sıradaki iş (14 adımlık döngü)", adimlar, onKapat = kapat) { secilen ->
                    val bugun = LocalDate.now().toEpochDay()
                    vm.ayarlariDegistir { it.copy(siraIndex = adimlar.indexOf(secilen), siraGunu = maxOf(bugun, it.baslangicGunu)) }
                }
            }
        }

        Bolum("Günlük hedefler")
        ListeSatiri("Kalori", sag = "${Bicim.tam(a.kaloriHedef)} kcal") {
            sayiPenceresi(
                "Günlük kalori hedefi", "kcal", a.kaloriHedef.toString(),
                aciklama = "Uygulama bu hedefi kendiliğinden değiştirmez. En az ${Bicim.tam(Kilo.KALORI_TABAN)} kcal.",
                kural = { if (it < Kilo.KALORI_TABAN) "Kalori hedefi ${Bicim.tam(Kilo.KALORI_TABAN)} kcal altına ayarlanamaz. Daha fazla açık için adım ekle." else null },
            ) { x, v -> x.copy(kaloriHedef = v.toInt()) }
        }
        ListeSatiri("Protein", sag = "${a.proteinMin}–${a.proteinMax} g") {
            aralikPenceresi("Günlük protein (g)", a.proteinMin.toString(), a.proteinMax.toString()) { x, alt, ust ->
                x.copy(proteinMin = alt.toInt(), proteinMax = ust.toInt())
            }
        }
        ListeSatiri("Adım", sag = "${Bicim.tam(a.adimMin)}–${Bicim.tam(a.adimMax)}") {
            aralikPenceresi("Günlük adım", a.adimMin.toString(), a.adimMax.toString()) { x, alt, ust ->
                x.copy(adimMin = alt.toInt(), adimMax = ust.toInt())
            }
        }
        ListeSatiri("Su", sag = "${Bicim.ondalik(a.suHedefMl / 1000.0, 2)} L") {
            sayiPenceresi("Günlük su", "litre", Bicim.giris(a.suHedefMl / 1000.0, 2), ondalik = true) { x, v -> x.copy(suHedefMl = (v * 1000).toInt()) }
        }
        ListeSatiri("Germe", sag = "${a.germeHedefDk} dk") {
            sayiPenceresi("Günlük germe", "dakika", a.germeHedefDk.toString()) { x, v -> x.copy(germeHedefDk = v.toInt()) }
        }
        ListeSatiri("Uyku", sag = "${Bicim.ondalik(a.uykuHedefSaat)}+ sa") {
            sayiPenceresi("Uyku hedefi", "saat", Bicim.giris(a.uykuHedefSaat), ondalik = true) { x, v -> x.copy(uykuHedefSaat = v) }
        }
        ListeSatiri("Haftalık ağırlık seansı", sag = "${a.haftalikSeansHedef}") {
            sayiPenceresi("Haftalık ağırlık seansı", "seans", a.haftalikSeansHedef.toString()) { x, v -> x.copy(haftalikSeansHedef = v.toInt()) }
        }
        ListeSatiri("Haftalık germe günü", sag = "${a.haftalikGermeHedef}") {
            sayiPenceresi("Haftalık germe günü", "gün", a.haftalikGermeHedef.toString(), kural = { if (it > 7) "En fazla 7." else null }) { x, v ->
                x.copy(haftalikGermeHedef = v.toInt())
            }
        }

        Bolum("Programı düzenle")
        ListeSatiri("Hareketler", alt = "Seans A ve B") { onAltSayfa("hareket") }
        ListeSatiri("Öğünler", alt = "Plandaki öğünler ve paylar") { onAltSayfa("ogun") }
        ListeSatiri("Germeler", alt = "Esneklik rutini") { onAltSayfa("germe") }
        ListeSatiri("Yiyecek listesi", alt = "Hızlı ekleme için referans değerler") { onAltSayfa("yiyecek") }
        ListeSatiri("Eğer-o zaman planlarım") { onAltSayfa("plan") }

        Bolum("Bildirimler")
        Text(
            "Hepsi yereldir; internet gerekmez. Birkaç dakika gecikebilirler.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AnahtarSatiri("Sabah tartı", a.bildirimTarti, alt = "O gün tartı girilmediyse hatırlatır") { ac -> bildirimAc { it.copy(bildirimTarti = ac) } }
        if (a.bildirimTarti) {
            ListeSatiri("Tartı saati", sag = Bicim.saat(a.tartiDakika)) { saatPenceresi("Tartı hatırlatma saati", a.tartiDakika) { x, v -> x.copy(tartiDakika = v) } }
        } else Cizgi()
        AnahtarSatiri("Akşam germe", a.bildirimGerme, alt = "O gün germe yapılmadıysa hatırlatır") { ac -> bildirimAc { it.copy(bildirimGerme = ac) } }
        if (a.bildirimGerme) {
            ListeSatiri("Germe saati", sag = Bicim.saat(a.germeDakika)) { saatPenceresi("Germe hatırlatma saati", a.germeDakika) { x, v -> x.copy(germeDakika = v) } }
        } else Cizgi()
        AnahtarSatiri(
            "Öğün arası", a.bildirimOgun,
            alt = "Son öğünden ${a.ogunAralikSaat} saat sonra hatırlatır (23:00–07:00 arası susar)",
        ) { ac -> bildirimAc { it.copy(bildirimOgun = ac) } }
        if (a.bildirimOgun) {
            ListeSatiri("Öğün arası süre", sag = "${a.ogunAralikSaat} saat") {
                sayiPenceresi("Öğünler arası en uzun süre", "saat", a.ogunAralikSaat.toString(), kural = { if (it > 12) "En fazla 12." else null }) { x, v ->
                    x.copy(ogunAralikSaat = v.toInt())
                }
            }
        } else Cizgi()
        if ((a.bildirimTarti || a.bildirimGerme || a.bildirimOgun) && !Bildirimler.izinVar(context)) {
            Bosluk()
            Uyari("Bildirim izni kapalı.", "Telefonun Ayarlar › Uygulamalar › Zincir › Bildirimler bölümünden izin ver.")
        }

        Bolum("Saat verisi (Health Connect)")
        SaatVerisiAyari(vm)

        Bolum("Görünüm")
        val temalar = listOf("Sistem", "Açık", "Koyu")
        Secim(temalar, temalar[a.tema.coerceIn(0, 2)]) { secilen -> vm.ayarlariDegistir { it.copy(tema = temalar.indexOf(secilen)) } }

        Bolum("Yedekleme")
        Text(
            "Tüm veri tek bir JSON dosyasına yazılır. Dosyayı kendine e-postayla gönderebilir ya da Drive'a kaydedebilirsin.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Bosluk()
        DoluDugme("Yedeği paylaş", Modifier.fillMaxWidth()) { paylas() }
        Bosluk(8.dp)
        CizgiDugme("Dosyaya kaydet", Modifier.fillMaxWidth()) { dosyayaKaydet.launch(dosyaAdi) }
        Bosluk(8.dp)
        CizgiDugme("Yedekten geri yükle", Modifier.fillMaxWidth()) { dosyaAc.launch(arrayOf("*/*")) }
        yedekDurumu?.let {
            Bosluk()
            Not(it)
        }

        Bosluk(32.dp)
        Text(
            "Zincir 1.3 · Veri yalnızca bu telefonda durur. Hesap, sunucu, reklam ve analitik yoktur.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    pencere?.invoke()
    bekleyenYedek?.let { json ->
        OnayDialog(
            "Yedekten geri yükle", "Telefondaki tüm kayıtlar silinecek ve yedekteki veri yüklenecek. Bu işlem geri alınamaz.", "Geri yükle",
            onKapat = { bekleyenYedek = null },
        ) {
            kapsam.launch { yedekDurumu = vm.yedekYukle(json)?.let { "Geri yüklenemedi: $it" } ?: "Yedek geri yüklendi." }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarihPenceresi(gun: Long, onKapat: () -> Unit, onSec: (Long) -> Unit) {
    val durum = rememberDatePickerState(initialSelectedDateMillis = gun * GUN_MS)
    DatePickerDialog(
        onDismissRequest = onKapat,
        confirmButton = {
            TextButton(onClick = {
                durum.selectedDateMillis?.let { onSec(Math.floorDiv(it, GUN_MS)) }
                onKapat()
            }) { Text("Seç", style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = { TextButton(onClick = onKapat) { Text("Vazgeç", style = MaterialTheme.typography.labelLarge) } },
    ) { DatePicker(state = durum) }
}

// ---------------------------------------------------------------------------------------------
// Düzenleme listeleri. Hepsi aynı kalıpta: liste + satıra dokununca düzenleme penceresi + "ekle".

private val EVET_HAYIR = listOf("Evet", "Hayır")
private fun evetHayir(b: Boolean) = if (b) "Evet" else "Hayır"

@Composable
private fun DuzenlemeSayfasi(baslik: String, aciklama: String?, onGeri: () -> Unit, onEkle: () -> Unit, icerik: @Composable () -> Unit) {
    Sayfa {
        MetinDugme("‹ Ayarlar", onClick = onGeri)
        EkranBasligi(baslik)
        if (aciklama != null) {
            Bosluk(8.dp)
            Text(aciklama, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Bosluk(16.dp)
        Cizgi()
        icerik()
        Bosluk(20.dp)
        CizgiDugme("+ Yeni ekle", Modifier.fillMaxWidth(), onClick = onEkle)
    }
}

@Composable
private fun HareketleriDuzenle(vm: AnaViewModel, onGeri: () -> Unit) {
    val hareketler by vm.hareketler.collectAsStateWithLifecycle()
    val kapsam = rememberCoroutineScope()
    var secili by remember { mutableStateOf<Hareket?>(null) }

    DuzenlemeSayfasi(
        "Hareketler", "Geçmiş kayıtlar hareket adıyla tutulur; adı değiştirirsen grafik yeni adla baştan başlar.", onGeri,
        onEkle = { secili = Hareket(seans = "A", sira = hareketler.count { it.seans == "A" } + 1, ad = "", setSayisi = 3, tekrarMin = 8, tekrarMax = 12, dinlenmeSn = 90) },
    ) {
        hareketler.forEach { h ->
            ListeSatiri(
                "${h.seans}${h.sira} · ${h.ad}",
                alt = "${h.setSayisi} × ${h.tekrarMin}–${h.tekrarMax} ${h.birim} · dinlenme ${Bicim.sure(h.dinlenmeSn)}" + (h.alternatifAd?.let { " · yoksa: $it" } ?: ""),
            ) { secili = h }
        }
    }

    secili?.let { h ->
        FormDialog(
            baslik = if (h.id == 0L) "Yeni hareket" else "Hareketi düzenle", onKapat = { secili = null },
            onSil = if (h.id == 0L) null else ({ kapsam.launch { vm.dao.hareketSil(h) } }),
            alanlar = listOf(
                Alan("Seans", h.seans, AlanTuru.SECIM, listOf("A", "B")),
                Alan("Sıra", h.sira.toString(), AlanTuru.TAM),
                Alan("Ad", h.ad),
                Alan("Makine yoksa (boş bırakılabilir)", h.alternatifAd.orEmpty()),
                Alan("Set sayısı", h.setSayisi.toString(), AlanTuru.TAM),
                Alan("Tekrar alt sınırı", h.tekrarMin.toString(), AlanTuru.TAM),
                Alan("Tekrar üst sınırı", h.tekrarMax.toString(), AlanTuru.TAM),
                Alan("Birim", h.birim, AlanTuru.SECIM, listOf("tekrar", "sn")),
                Alan("Ek not (her bacak gibi)", h.ek),
                Alan("Dinlenme (saniye)", h.dinlenmeSn.toString(), AlanTuru.TAM),
                Alan("Ağırlık (kg) girilir", evetHayir(h.agirlikli), AlanTuru.SECIM, EVET_HAYIR),
            ),
            dogrula = { d ->
                val sayilar = listOf(d[1], d[4], d[5], d[6], d[9]).map { Bicim.cozTam(it) }
                when {
                    d[2].isBlank() -> "Ad boş olamaz."
                    sayilar.any { it == null || it < 0 } -> "Sayıları kontrol et."
                    sayilar[1]!! < 1 -> "En az 1 set olmalı."
                    sayilar[2]!! > sayilar[3]!! -> "Tekrar alt sınırı üst sınırdan büyük olamaz."
                    else -> null
                }
            },
        ) { d ->
            kapsam.launch {
                vm.dao.hareketYaz(
                    h.copy(
                        seans = d[0], sira = d[1].trim().toInt(), ad = d[2].trim(), alternatifAd = d[3].trim().ifBlank { null },
                        setSayisi = d[4].trim().toInt(), tekrarMin = d[5].trim().toInt(), tekrarMax = d[6].trim().toInt(),
                        birim = d[7], ek = d[8].trim(), dinlenmeSn = d[9].trim().toInt(), agirlikli = d[10] == "Evet",
                    )
                )
            }
        }
    }
}

@Composable
private fun OgunleriDuzenle(vm: AnaViewModel, a: Ayarlar, onGeri: () -> Unit) {
    val ogunler by vm.ogunler.collectAsStateWithLifecycle()
    val kapsam = rememberCoroutineScope()
    var secili by remember { mutableStateOf<Ogun?>(null) }
    val turler = listOf("Öğün", "Pay")

    DuzenlemeSayfasi(
        "Öğünler",
        "Plan toplamı: ${Bicim.tam(ogunler.sumOf { it.kcal })} kcal, ${ogunler.sumOf { it.protein }} g protein. Günlük hedef: ${Bicim.tam(a.kaloriHedef)} kcal.",
        onGeri, onEkle = { secili = Ogun(sira = ogunler.size + 1, ad = "", icerik = "", kcal = 0, protein = 0) },
    ) {
        ogunler.forEach { o -> ListeSatiri(o.ad, alt = o.icerik, sag = "${Bicim.tam(o.kcal)} kcal") { secili = o } }
    }

    secili?.let { o ->
        FormDialog(
            baslik = if (o.id == 0L) "Yeni öğün" else "Öğünü düzenle", onKapat = { secili = null },
            onSil = if (o.id == 0L) null else ({ kapsam.launch { vm.dao.ogunSil(o) } }),
            alanlar = listOf(
                Alan("Sıra", o.sira.toString(), AlanTuru.TAM), Alan("Ad", o.ad), Alan("İçerik", o.icerik, AlanTuru.UZUN_METIN),
                Alan("Kalori (kcal)", o.kcal.toString(), AlanTuru.TAM), Alan("Protein (g)", o.protein.toString(), AlanTuru.TAM),
                Alan("Tür (pay: pişirme yağı gibi ek kalori)", if (o.pay) "Pay" else "Öğün", AlanTuru.SECIM, turler),
            ),
            dogrula = { d ->
                when {
                    d[1].isBlank() -> "Ad boş olamaz."
                    listOf(d[0], d[3], d[4]).any { (Bicim.cozTam(it) ?: -1) < 0 } -> "Sayıları kontrol et."
                    else -> null
                }
            },
        ) { d ->
            kapsam.launch {
                vm.dao.ogunYaz(
                    o.copy(sira = d[0].trim().toInt(), ad = d[1].trim(), icerik = d[2].trim(), kcal = d[3].trim().toInt(), protein = d[4].trim().toInt(), pay = d[5] == "Pay")
                )
            }
        }
    }
}

@Composable
private fun GermeleriDuzenle(vm: AnaViewModel, onGeri: () -> Unit) {
    val germeler by vm.germeler.collectAsStateWithLifecycle()
    val kapsam = rememberCoroutineScope()
    var secili by remember { mutableStateOf<Germe?>(null) }
    val toplamSn = germeler.sumOf { it.sureSn * if (it.ikiTaraf) 2 else 1 }

    DuzenlemeSayfasi(
        "Germeler", "Rutin toplamı: ${Bicim.sayac(toplamSn)} dk.", onGeri,
        onEkle = { secili = Germe(sira = germeler.size + 1, ad = "", bolge = "") },
    ) {
        germeler.forEach { g ->
            ListeSatiri("${g.sira} · ${g.ad}", alt = g.bolge + " · " + (if (g.ikiTaraf) "her taraf " else "") + "${g.sureSn} sn") { secili = g }
        }
    }

    secili?.let { g ->
        FormDialog(
            baslik = if (g.id == 0L) "Yeni germe" else "Germeyi düzenle", onKapat = { secili = null },
            onSil = if (g.id == 0L) null else ({ kapsam.launch { vm.dao.germeSil(g) } }),
            alanlar = listOf(
                Alan("Sıra", g.sira.toString(), AlanTuru.TAM), Alan("Ad", g.ad, AlanTuru.UZUN_METIN), Alan("Bölge", g.bolge),
                Alan("Süre (saniye)", g.sureSn.toString(), AlanTuru.TAM),
                Alan("İki taraf ayrı ayrı", evetHayir(g.ikiTaraf), AlanTuru.SECIM, EVET_HAYIR),
            ),
            dogrula = { d ->
                when {
                    d[1].isBlank() -> "Ad boş olamaz."
                    Bicim.cozTam(d[0]) == null || (Bicim.cozTam(d[3]) ?: 0) < 5 -> "Sayıları kontrol et (süre en az 5 sn)."
                    else -> null
                }
            },
        ) { d ->
            kapsam.launch {
                vm.dao.germeYaz(g.copy(sira = d[0].trim().toInt(), ad = d[1].trim(), bolge = d[2].trim(), sureSn = d[3].trim().toInt(), ikiTaraf = d[4] == "Evet"))
            }
        }
    }
}

@Composable
private fun YiyecekleriDuzenle(vm: AnaViewModel, onGeri: () -> Unit) {
    val yiyecekler by vm.yiyecekler.collectAsStateWithLifecycle()
    val kapsam = rememberCoroutineScope()
    var secili by remember { mutableStateOf<Yiyecek?>(null) }

    DuzenlemeSayfasi(
        "Yiyecek listesi", "Hızlı eklemede kullanılan referans değerler. Her satır: ölçü (gram) ve o ölçüdeki kalori ile protein.", onGeri,
        onEkle = { secili = Yiyecek(ad = "", bazGram = 100.0, kcal = 0.0, protein = 0.0) },
    ) {
        yiyecekler.forEach { y ->
            ListeSatiri(y.ad, alt = "${Bicim.ondalik(y.bazGram)} g = ${Bicim.ondalik(y.kcal)} kcal, ${Bicim.ondalik(y.protein)} g protein") { secili = y }
        }
    }

    secili?.let { y ->
        FormDialog(
            baslik = if (y.id == 0L) "Yeni yiyecek" else "Yiyeceği düzenle", onKapat = { secili = null },
            onSil = if (y.id == 0L) null else ({ kapsam.launch { vm.dao.yiyecekSil(y) } }),
            alanlar = listOf(
                Alan("Ad", y.ad), Alan("Ölçü (gram)", Bicim.giris(y.bazGram), AlanTuru.ONDALIK),
                Alan("Bu ölçüde kalori (kcal)", Bicim.giris(y.kcal), AlanTuru.ONDALIK),
                Alan("Bu ölçüde protein (g)", Bicim.giris(y.protein), AlanTuru.ONDALIK),
            ),
            dogrula = { d ->
                when {
                    d[0].isBlank() -> "Ad boş olamaz."
                    (Bicim.coz(d[1]) ?: 0.0) <= 0 || Bicim.coz(d[2]) == null || Bicim.coz(d[3]) == null -> "Sayıları kontrol et."
                    else -> null
                }
            },
        ) { d ->
            kapsam.launch { vm.dao.yiyecekYaz(y.copy(ad = d[0].trim(), bazGram = Bicim.coz(d[1])!!, kcal = Bicim.coz(d[2])!!, protein = Bicim.coz(d[3])!!)) }
        }
    }
}

@Composable
private fun PlanlariDuzenle(vm: AnaViewModel, onGeri: () -> Unit) {
    val planlar by vm.egerPlanlar.collectAsStateWithLifecycle()
    val kapsam = rememberCoroutineScope()
    var secili by remember { mutableStateOf<EgerPlan?>(null) }

    DuzenlemeSayfasi(
        "Eğer-o zaman planlarım", "Zor anlar için önceden verilmiş kararlar. Kısa ve somut yaz.", onGeri,
        onEkle = { secili = EgerPlan(sira = (planlar.maxOfOrNull { it.sira } ?: 0) + 1, metin = "Eğer …, o zaman …") },
    ) {
        planlar.forEach { p -> ListeSatiri(p.metin) { secili = p } }
    }

    secili?.let { p ->
        FormDialog(
            baslik = if (p.id == 0L) "Yeni plan" else "Planı düzenle", onKapat = { secili = null },
            onSil = if (p.id == 0L) null else ({ kapsam.launch { vm.dao.egerPlanSil(p) } }),
            alanlar = listOf(Alan("Plan", p.metin, AlanTuru.UZUN_METIN)),
            dogrula = { d -> if (d[0].isBlank()) "Plan boş olamaz." else null },
        ) { d -> kapsam.launch { vm.dao.egerPlanYaz(p.copy(metin = d[0].trim())) } }
    }
}

/**
 * Health Connect'ten adım ve uyku okuma anahtarı. Açılınca Health Connect'in izin penceresi gelir.
 * Huawei Sağlık Health Connect'e kendisi yazmadığı için arada bir köprü uygulama (Health Sync) gerekir.
 */
@Composable
private fun SaatVerisiAyari(vm: AnaViewModel) {
    val context = LocalContext.current
    val durum = remember { SaglikBaglantisi.durum(context) }
    var acik by remember { mutableStateOf(SaglikBaglantisi.acik(context)) }
    var mesaj by remember { mutableStateOf<String?>(null) }
    val ozet by vm.saglikOzeti.collectAsStateWithLifecycle()

    val izinIste = rememberLauncherForActivityResult(SaglikBaglantisi.izinSozlesmesi()) { verilen ->
        if (verilen.isEmpty()) {
            mesaj = "İzin verilmedi. Health Connect'te Zincir için adım ve uyku okuma iznini açabilirsin."
        } else {
            SaglikBaglantisi.acikYap(context, true)
            acik = true
            mesaj = null
            vm.saglikEsitle()
        }
    }

    Text(
        "Saatinin adım, uyku ve kardiyo verisini telefondaki Health Connect'ten okur; elle giriş yine kalır. " +
            "Veri telefondan çıkmaz. Huawei Sağlık için arada Health Sync gibi bir köprü uygulama gerekir.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    when (durum) {
        SaglikBaglantisi.Durum.YOK -> {
            Bosluk()
            Not("Bu telefonda Health Connect bulunamadı.")
        }
        SaglikBaglantisi.Durum.GUNCELLEME_GEREK -> {
            Bosluk()
            Not("Health Connect'in güncellenmesi gerekiyor (Play Store).")
        }
        SaglikBaglantisi.Durum.HAZIR -> {
            AnahtarSatiri("Adım ve uykuyu saatten al", acik, alt = "Adımda büyük olan kalır; uyku yalnızca boşsa dolar") { yeni ->
                if (yeni) {
                    izinIste.launch(SaglikBaglantisi.IZINLER)
                } else {
                    SaglikBaglantisi.acikYap(context, false)
                    acik = false
                    mesaj = null
                    vm.saglikEsitle()
                }
            }
            Cizgi()
            if (acik) {
                Bosluk(8.dp)
                Text(ozet ?: "Henüz okunmadı.", style = MaterialTheme.typography.bodyMedium)
                MetinDugme("Şimdi yeniden oku") { vm.saglikEsitle() }
                // Sonradan eklenen bir veri türü (örneğin egzersiz) için izin henüz verilmemiş olabilir.
                val verilen by produceState<Set<String>?>(null, ozet) { value = SaglikBaglantisi.verilenIzinler(context) }
                if (verilen?.containsAll(SaglikBaglantisi.IZINLER) == false) {
                    Not("Bazı veri türleri için izin eksik (kardiyo önerisi egzersiz iznini ister).")
                    MetinDugme("Eksik izinleri ver") { izinIste.launch(SaglikBaglantisi.IZINLER) }
                }
            }
        }
    }
    mesaj?.let {
        Bosluk(8.dp)
        Not(it)
    }
}
