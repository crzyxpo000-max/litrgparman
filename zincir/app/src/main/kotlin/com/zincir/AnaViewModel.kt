package com.zincir

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zincir.bildirim.Bildirimler
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Set arası dinlenme geri sayımı. [bitis]: telefonun açılışından beri geçen süre cinsinden bitiş anı. */
data class Dinlenme(val bitis: Long, val toplamSn: Int)

/** Germe rutininin tek adımı (iki taraflı germeler iki adım olur). */
data class GermeAdimi(val ad: String, val bolge: String, val taraf: String?, val sureSn: Int)

/** Çalışan germe rutininin durumu. */
data class GermeCalisma(
    val adimlar: List<GermeAdimi>,
    val index: Int = 0,
    val kalanMs: Long,
    val duraklatildi: Boolean = false,
    val bitti: Boolean = false,
)

/**
 * ViewModel: ekranların ihtiyacı olan canlı veriyi ("StateFlow") ve kullanıcı eylemlerini bir arada tutar.
 * Ekran döndürülse ya da sekme değişse bile burada duran durum (zamanlayıcılar dahil) kaybolmaz.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnaViewModel(app: Application) : AndroidViewModel(app) {
    val depo: Depo = (app as ZincirUygulamasi).depo
    val dao = depo.dao

    private val _bugun = MutableStateFlow(LocalDate.now().toEpochDay())
    val bugun: StateFlow<Long> = _bugun

    private fun <T> Flow<T>.durum(ilk: T): StateFlow<T> = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ilk)

    // ---- Canlı veri
    val ayarlar: StateFlow<Ayarlar?> = dao.ayarlarAkisi().durum(null)
    val gun: StateFlow<GunKaydi> = _bugun.flatMapLatest { g -> dao.gunAkisi(g).map { it ?: GunKaydi(g) } }.durum(GunKaydi(_bugun.value))
    val sonGunler: StateFlow<List<GunKaydi>> = _bugun.flatMapLatest { dao.gunAraligiAkisi(it - 400, it) }.durum(emptyList())
    val seanslar: StateFlow<List<Seans>> = dao.seanslarAkisi().durum(emptyList())
    val aktifSeans: StateFlow<Seans?> = dao.aktifSeansAkisi().durum(null)
    val aktifSetler: StateFlow<List<SetKaydi>> =
        aktifSeans.flatMapLatest { s -> if (s == null) flowOf(emptyList()) else dao.setlerAkisi(s.id) }.durum(emptyList())
    val hareketler: StateFlow<List<Hareket>> = dao.hareketlerAkisi().durum(emptyList())
    val sonAgirlikGunu: StateFlow<Long?> = dao.sonAgirlikGunuAkisi().durum(null)
    val ogunler = dao.ogunlerAkisi().durum(emptyList())
    val yiyecekler = dao.yiyeceklerAkisi().durum(emptyList())
    val yemekler = _bugun.flatMapLatest { dao.yemekKayitlariAkisi(it) }.durum(emptyList())
    val sonYemekler = _bugun.flatMapLatest { dao.yemekAraligiAkisi(it - 400, it) }.durum(emptyList())
    val germeler = dao.germelerAkisi().durum(emptyList())
    val egerPlanlar = dao.egerPlanlarAkisi().durum(emptyList())
    val kilolar = dao.olcumlerAkisi(Tur.KILO).durum(emptyList())
    val beller = dao.olcumlerAkisi(Tur.BEL).durum(emptyList())
    val esneklikler = dao.olcumlerAkisi(Tur.ESNEKLIK).durum(emptyList())
    val kayitliHareketAdlari = dao.kayitliHareketAdlari().durum(emptyList())

    /** İçinde bulunulan program haftasının ilk günü. */
    val haftaBasi: StateFlow<Long> =
        combine(_bugun, ayarlar) { g, a -> Program.haftaBasi(a?.baslangicGunu ?: g, g) }.durum(_bugun.value)
    val kacamaklar: StateFlow<List<Kacamak>> = haftaBasi.flatMapLatest { dao.kacamaklarAkisi(it, it + 6) }.durum(emptyList())

    init {
        calistir {
            depo.hazirla(_bugun.value)
            // Alarmlar uygulama güncellenince ya da durdurulunca silinir: her açılışta yeniden kur.
            dao.ayarlar()?.let { Bildirimler.planla(getApplication(), it) }
            ogunHatirlatmasiniKur()
        }
    }

    private fun calistir(blok: suspend () -> Unit): Job = viewModelScope.launch { blok() }

    /** Uygulama öne gelince çağrılır: gece yarısı geçtiyse "bugün" değişir ve sıra güncellenir. */
    fun gunuYenile() {
        val g = LocalDate.now().toEpochDay()
        _bugun.value = g
        calistir { depo.siraGuncelle(g) }
    }

    // ---- Gün kaydı
    fun gunDegistir(degisim: (GunKaydi) -> GunKaydi) = calistir { depo.gunDegistir(_bugun.value, degisim) }
    fun ayarlariDegistir(degisim: (Ayarlar) -> Ayarlar) = calistir {
        depo.ayarlariDegistir(degisim)
        dao.ayarlar()?.let { Bildirimler.planla(getApplication(), it) }
        ogunHatirlatmasiniKur()
    }
    fun baslangicGununuDegistir(gun: Long) = calistir { depo.baslangicGununuDegistir(gun) }
    fun siraAtla() = calistir { depo.siraAtla(_bugun.value) }

    // ---- Antrenman
    fun seansBaslat(tur: String, kisaMod: Boolean) = calistir { depo.seansBaslat(tur, kisaMod, _bugun.value) }
    fun seansBitir(id: Long) = calistir {
        dinlenmeGec()
        depo.seansBitir(id, _bugun.value)
    }
    fun seansSil(id: Long) = calistir {
        dinlenmeGec()
        depo.seansSil(id)
    }
    fun kardiyoKaydet(tur: String, sureDk: Int) = calistir { depo.kardiyoKaydet(tur, sureDk, _bugun.value) }

    /** Seti kaydeder ve o hareketin dinlenme süresi kadar geri sayım başlatır. */
    fun setKaydet(seans: Seans, hareket: Hareket, ad: String, setNo: Int, kg: Double, tekrar: Int) = calistir {
        dao.setEkle(SetKaydi(seansId = seans.id, hareketId = hareket.id, hareketAdi = ad, setNo = setNo, kg = kg, tekrar = tekrar))
        dinlenmeBaslat(hareket.dinlenmeSn)
    }
    fun setSil(set: SetKaydi) = calistir { dao.setSil(set) }

    // ---- Dinlenme sayacı
    private val _dinlenme = MutableStateFlow<Dinlenme?>(null)
    val dinlenme: StateFlow<Dinlenme?> = _dinlenme
    private var dinlenmeIsi: Job? = null

    private fun dinlenmeBaslat(saniye: Int) {
        dinlenmeIsi?.cancel()
        if (saniye <= 0) return
        _dinlenme.value = Dinlenme(SystemClock.elapsedRealtime() + saniye * 1000L, saniye)
        dinlenmeIsi = viewModelScope.launch {
            delay(saniye * 1000L)
            Titresim.titret(getApplication(), 700)
            _dinlenme.value = null
        }
    }

    fun dinlenmeGec() {
        dinlenmeIsi?.cancel()
        _dinlenme.value = null
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

    // ---- Germe rutini
    private val _germe = MutableStateFlow<GermeCalisma?>(null)
    val germeCalisma: StateFlow<GermeCalisma?> = _germe
    private var germeIsi: Job? = null
    private var germeGecenMs = 0L

    fun germeBaslat(germeler: List<Germe>) {
        val adimlar = germeler.flatMap { g ->
            if (g.ikiTaraf) listOf(GermeAdimi(g.ad, g.bolge, "Sol", g.sureSn), GermeAdimi(g.ad, g.bolge, "Sağ", g.sureSn))
            else listOf(GermeAdimi(g.ad, g.bolge, null, g.sureSn))
        }
        if (adimlar.isEmpty()) return
        germeIsi?.cancel()
        germeGecenMs = 0
        _germe.value = GermeCalisma(adimlar, kalanMs = adimlar[0].sureSn * 1000L)
        Titresim.titret(getApplication(), 200)
        germeIsi = viewModelScope.launch {
            var son = SystemClock.elapsedRealtime()
            while (true) {
                delay(100)
                val simdi = SystemClock.elapsedRealtime()
                val gecen = simdi - son
                son = simdi
                val d = _germe.value ?: break
                if (d.duraklatildi || d.bitti) continue
                germeGecenMs += gecen
                val kalan = d.kalanMs - gecen
                if (kalan > 0) {
                    _germe.value = d.copy(kalanMs = kalan)
                } else if (!germeSonrakiAdim(d)) {
                    break
                }
            }
        }
    }

    /** Sıradaki germe adımına geçer; rutin bittiyse günü kaydeder ve false döner. */
    private suspend fun germeSonrakiAdim(d: GermeCalisma): Boolean {
        val sonraki = d.index + 1
        return if (sonraki < d.adimlar.size) {
            Titresim.titret(getApplication(), 500)
            _germe.value = d.copy(index = sonraki, kalanMs = d.adimlar[sonraki].sureSn * 1000L)
            true
        } else {
            Titresim.cift(getApplication())
            val toplamDk = (d.adimlar.sumOf { it.sureSn } + 59) / 60
            depo.gunDegistir(_bugun.value) { it.copy(germeDk = maxOf(it.germeDk, toplamDk)) }
            _germe.value = d.copy(kalanMs = 0, bitti = true)
            false
        }
    }

    fun germeDuraklat(duraklat: Boolean) {
        _germe.value = _germe.value?.copy(duraklatildi = duraklat)
    }

    fun germeAtla() = calistir {
        val d = _germe.value ?: return@calistir
        if (!d.bitti && !germeSonrakiAdim(d)) germeIsi?.cancel()
    }

    /** Rutini kapatır. Yarıda bırakıldıysa geçen tam dakikalar yine de güne yazılır. */
    fun germeKapat() = calistir {
        val d = _germe.value
        germeIsi?.cancel()
        _germe.value = null
        if (d != null && !d.bitti) {
            val dk = (germeGecenMs / 60_000L).toInt()
            if (dk > 0) depo.gunDegistir(_bugun.value) { it.copy(germeDk = it.germeDk + dk) }
        }
    }

    // ---- Yedekleme
    suspend fun yedekJson(): String = Yedek.disaAktar(depo.db)

    /** Yedeği yükler. Başarılıysa null, değilse hata metni döner. */
    suspend fun yedekYukle(json: String): String? = try {
        Yedek.iceAktar(depo.db, json)
        depo.siraGuncelle(_bugun.value)
        dao.ayarlar()?.let { Bildirimler.planla(getApplication(), it) }
        null
    } catch (e: Exception) {
        e.message ?: "Yedek yüklenemedi."
    }
}
