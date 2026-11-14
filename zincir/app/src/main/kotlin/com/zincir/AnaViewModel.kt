package com.zincir

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zincir.bildirim.Bildirimler
import com.zincir.bildirim.GermeCalisma
import com.zincir.bildirim.GermeMotoru
import com.zincir.bildirim.Titresim
import com.zincir.mantik.Program
import com.zincir.veri.Ayarlar
import com.zincir.veri.Depo
import com.zincir.veri.Germe
import com.zincir.veri.GunKaydi
import com.zincir.veri.Hareket
import com.zincir.veri.Kacamak
import com.zincir.veri.Ogun
import com.zincir.veri.Seans
import com.zincir.veri.SetKaydi
import com.zincir.veri.Tur
import com.zincir.veri.Yedek
import com.zincir.veri.Yiyecek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Set arası dinlenme geri sayımı. [bitis]: SystemClock.elapsedRealtime cinsinden bitiş anı. */
data class Dinlenme(val bitis: Long, val toplamSn: Int)

/**
 * ViewModel: ekranların ihtiyacı olan canlı veriyi ("StateFlow") ve kullanıcı eylemlerini bir arada tutar.
 * Ekran döndürülse ya da sekme değişse bile burada duran durum kaybolmaz.
 *
 * İki ayrı "gün" vardır:
 *  - [gercekBugun]: takvimdeki bugün (telefonun yerel tarihi). Sıra ve zincir uyarısı buna bakar.
 *  - [bugun]: kayıtların yazıldığı gün. Normalde bugündür; Bugün ekranındaki "‹ Dün" seçilirse dündür.
 *    (Gece yarısından sonra biten uçuşlarda girişleri önceki güne yazabilmek için.)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnaViewModel(app: Application) : AndroidViewModel(app) {
    private val uygulama = app as ZincirUygulamasi
    val depo: Depo = uygulama.depo
    val dao = depo.dao

    private val _gercekBugun = MutableStateFlow(LocalDate.now().toEpochDay())
    val gercekBugun: StateFlow<Long> = _gercekBugun
    private val _dunSecili = MutableStateFlow(false)
    val dunSecili: StateFlow<Boolean> = _dunSecili
    private val _bugun = MutableStateFlow(_gercekBugun.value)
    val bugun: StateFlow<Long> = _bugun

    private fun <T> Flow<T>.durum(ilk: T): StateFlow<T> = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ilk)

    // ---- Canlı veri
    val ayarlar: StateFlow<Ayarlar?> = dao.ayarlarAkisi().durum(null)
    val gun: StateFlow<GunKaydi> = _bugun.flatMapLatest { g -> dao.gunAkisi(g).map { it ?: GunKaydi(g) } }.durum(GunKaydi(_bugun.value))
    val sonGunler: StateFlow<List<GunKaydi>> = _gercekBugun.flatMapLatest { dao.gunAraligiAkisi(it - 400, it) }.durum(emptyList())
    val seanslar: StateFlow<List<Seans>> = dao.seanslarAkisi().durum(emptyList())
    val aktifSeans: StateFlow<Seans?> = dao.aktifSeansAkisi().durum(null)
    val aktifSetler: StateFlow<List<SetKaydi>> =
        aktifSeans.flatMapLatest { s -> if (s == null) flowOf(emptyList()) else dao.setlerAkisi(s.id) }.durum(emptyList())
    val hareketler: StateFlow<List<Hareket>> = dao.hareketlerAkisi().durum(emptyList())
    val sonAgirlikGunu: StateFlow<Long?> = dao.sonAgirlikGunuAkisi().durum(null)
    val ogunler = dao.ogunlerAkisi().durum(emptyList())
    val yiyecekler = dao.yiyeceklerAkisi().durum(emptyList())
    val yemekler = _bugun.flatMapLatest { dao.yemekKayitlariAkisi(it) }.durum(emptyList())
    val sonYemekler = _gercekBugun.flatMapLatest { dao.yemekAraligiAkisi(it - 400, it) }.durum(emptyList())
    val germeler = dao.germelerAkisi().durum(emptyList())
    val egerPlanlar = dao.egerPlanlarAkisi().durum(emptyList())
    val kilolar = dao.olcumlerAkisi(Tur.KILO).durum(emptyList())
    val beller = dao.olcumlerAkisi(Tur.BEL).durum(emptyList())
    val esneklikler = dao.olcumlerAkisi(Tur.ESNEKLIK).durum(emptyList())
    val kayitliHareketAdlari = dao.kayitliHareketAdlari().durum(emptyList())

    /** Kayıt gününün içinde olduğu takvim haftasının Pazartesi'si. */
    val haftaBasi: StateFlow<Long> = _bugun.map { Program.takvimHaftaBasi(it) }.durum(Program.takvimHaftaBasi(_bugun.value))
    val kacamaklar: StateFlow<List<Kacamak>> = haftaBasi.flatMapLatest { dao.kacamaklarAkisi(it, it + 6) }.durum(emptyList())

    init {
        calistir {
            depo.hazirla(_gercekBugun.value)
            // Alarmlar uygulama güncellenince ya da durdurulunca silinir: her açılışta yeniden kur.
            dao.ayarlar()?.let { Bildirimler.planla(getApplication(), it) }
            ogunHatirlatmasiniKur()
        }
    }

    private fun calistir(blok: suspend () -> Unit): Job = viewModelScope.launch { blok() }

    /** Uygulama öne gelince çağrılır: gün değiştiyse "bugün" yenilenir, "Dün" seçimi sıfırlanır, sıra güncellenir. */
    fun gunuYenile() {
        val g = LocalDate.now().toEpochDay()
        if (g != _gercekBugun.value) {
            _gercekBugun.value = g
            _dunSecili.value = false
            _bugun.value = g
        }
        calistir { depo.siraGuncelle(g) }
    }

    /** Bugün ekranındaki "‹ Dün | Bugün ›" geçişi. */
    fun dunSec(dun: Boolean) {
        _dunSecili.value = dun
        _bugun.value = _gercekBugun.value - if (dun) 1 else 0
    }

    // ---- Gün kaydı
    fun gunDegistir(degisim: (GunKaydi) -> GunKaydi) = calistir { depo.gunDegistir(_bugun.value, degisim) }
    fun ayarlariDegistir(degisim: (Ayarlar) -> Ayarlar) = calistir {
        depo.ayarlariDegistir(degisim)
        dao.ayarlar()?.let { Bildirimler.planla(getApplication(), it) }
        ogunHatirlatmasiniKur()
    }
    fun baslangicGununuDegistir(gun: Long) = calistir { depo.baslangicGununuDegistir(gun) }
    fun siraAtla() = calistir { depo.siraAtla(_gercekBugun.value) }

    // ---- Antrenman
    fun seansBaslat(tur: String, kisaMod: Boolean) = calistir { depo.seansBaslat(tur, kisaMod, _bugun.value) }
    fun seansBitir(id: Long) = calistir {
        dinlenmeGec()
        depo.seansBitir(id, _bugun.value, _gercekBugun.value)
    }
    fun seansSil(id: Long) = calistir {
        dinlenmeGec()
        depo.seansSil(id)
    }
    fun kardiyoKaydet(tur: String, sureDk: Int) = calistir { depo.kardiyoKaydet(tur, sureDk, _bugun.value, _gercekBugun.value) }

    /** Seti kaydeder ve o hareketin dinlenme süresi kadar geri sayım başlatır. */
    fun setKaydet(seans: Seans, hareket: Hareket, ad: String, setNo: Int, kg: Double, tekrar: Int) = calistir {
        dao.setEkle(SetKaydi(seansId = seans.id, hareketId = hareket.id, hareketAdi = ad, setNo = setNo, kg = kg, tekrar = tekrar))
        dinlenmeBaslat(hareket.dinlenmeSn)
    }
    fun setSil(set: SetKaydi) = calistir { dao.setSil(set) }

    // ---- Dinlenme sayacı
    // Süre, bitiş anı ile SystemClock.elapsedRealtime farkından hesaplanır; telefon kilitlense de şaşmaz.
    // Bitişte: uygulama öndeyse buradan titreşim; kilitli ya da arka plandaysa alarm → bildirim (bkz. Bildirimler).
    private val _dinlenme = MutableStateFlow<Dinlenme?>(null)
    val dinlenme: StateFlow<Dinlenme?> = _dinlenme
    private var dinlenmeIsi: Job? = null

    private fun dinlenmeBaslat(saniye: Int) {
        dinlenmeIsi?.cancel()
        if (saniye <= 0) return
        val bitis = SystemClock.elapsedRealtime() + saniye * 1000L
        _dinlenme.value = Dinlenme(bitis, saniye)
        Bildirimler.dinlenmeAlarmiKur(getApplication(), bitis)
        dinlenmeIsi = viewModelScope.launch {
            while (true) {
                val kalan = bitis - SystemClock.elapsedRealtime()
                if (kalan <= 0) break
                delay(minOf(kalan, 500))
            }
            if (ZincirUygulamasi.onPlanda) Titresim.titret(getApplication(), 700)
            _dinlenme.value = null
        }
    }

    fun dinlenmeGec() {
        dinlenmeIsi?.cancel()
        _dinlenme.value = null
        Bildirimler.dinlenmeAlarminiIptalEt(getApplication())
    }

    // ---- Beslenme
    fun ogunIsaretle(ogun: Ogun, isaretli: Boolean) = calistir {
        depo.ogunIsaretle(ogun, _bugun.value, isaretli)
        ogunHatirlatmasiniKur()
    }
    fun hizliEkle(y: Yiyecek, gram: Double) = calistir {
        depo.hizliEkle(y, gram, _bugun.value)
        ogunHatirlatmasiniKur()
    }
    fun kacamakEkle(ad: String) = calistir { dao.kacamakEkle(Kacamak(gun = _bugun.value, ad = ad)) }

    private suspend fun ogunHatirlatmasiniKur() {
        val a = dao.ayarlar() ?: return
        Bildirimler.ogunPlanla(getApplication(), a, dao.sonYemekZamani())
    }

    // ---- Ölçümler
    fun olcumKaydet(tur: String, deger: Double, gun: Long = _bugun.value) = calistir { depo.olcumKaydet(tur, gun, deger) }

    // ---- Germe rutini (süreyi GermeMotoru sayar; ekran kapansa da ön plan servisiyle sürer)
    val germeCalisma: StateFlow<GermeCalisma?> = GermeMotoru.durum
    fun germeBaslat(germeler: List<Germe>) = GermeMotoru.baslat(uygulama, germeler, _bugun.value)
    fun germeDuraklat(duraklat: Boolean) = GermeMotoru.duraklat(duraklat)
    fun germeAtla() = GermeMotoru.atla(uygulama)
    fun germeKapat() = GermeMotoru.kapat(uygulama)

    // ---- Yedekleme
    suspend fun yedekJson(): String = Yedek.disaAktar(depo.db)

    /** Yedeği yükler. Başarılıysa null, değilse hata metni döner. */
    suspend fun yedekYukle(json: String): String? = try {
        Yedek.iceAktar(depo.db, json)
        depo.siraGuncelle(_gercekBugun.value)
        dao.ayarlar()?.let { Bildirimler.planla(getApplication(), it) }
        null
    } catch (e: Exception) {
        e.message ?: "Yedek yüklenemedi."
    }
}
