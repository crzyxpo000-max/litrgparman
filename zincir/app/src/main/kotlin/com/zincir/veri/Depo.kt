package com.zincir.veri

import androidx.room.withTransaction
import com.zincir.mantik.Sira
import com.zincir.mantik.SiraDurumu

/**
 * Depo: veritabanına yazan işlemlerin toplandığı yer. Ekranlar veriyi buradan (ve [dao] akışlarından) alır.
 * Birden çok tabloya dokunan işlemler "transaction" içinde yapılır: ya hepsi yazılır ya hiçbiri.
 */
class Depo(val db: Veritabani) {
    val dao: TakipDao = db.dao()

    /** İlk açılışta başlangıç verisini yazar; sonra sıranın gününü günceller. */
    suspend fun hazirla(bugun: Long) {
        db.withTransaction {
            if (dao.ayarlar() == null) {
                dao.ayarlarYaz(BaslangicVerisi.ayarlar())
                dao.hareketleriEkle(BaslangicVerisi.hareketler)
                dao.ogunleriEkle(BaslangicVerisi.ogunler)
                dao.yiyecekleriEkle(BaslangicVerisi.yiyecekler)
                dao.germeleriEkle(BaslangicVerisi.germeler)
                dao.egerPlanlariEkle(BaslangicVerisi.egerPlanlar)
            }
        }
        siraGuncelle(bugun)
    }

    suspend fun ayarlariDegistir(degisim: (Ayarlar) -> Ayarlar) = db.withTransaction {
        val a = dao.ayarlar() ?: return@withTransaction
        val yeni = degisim(a)
        if (yeni != a) dao.ayarlarYaz(yeni)
    }

    /** Program başlangıç tarihini değiştirir. Henüz hiç seans yapılmadıysa sıra da o güne taşınır. */
    suspend fun baslangicGununuDegistir(gun: Long) = db.withTransaction {
        val a = dao.ayarlar() ?: return@withTransaction
        val siraBasta = a.siraIndex == 0 && dao.seansSayisi() == 0
        dao.ayarlarYaz(a.copy(baslangicGunu = gun, siraGunu = if (siraBasta) gun else a.siraGunu))
    }

    suspend fun gunDegistir(gun: Long, degisim: (GunKaydi) -> GunKaydi) = db.withTransaction {
        val g = dao.gun(gun) ?: GunKaydi(gun)
        dao.gunYaz(degisim(g))
    }

    // ---- Sıra

    suspend fun siraGuncelle(bugun: Long) = ayarlariDegistir { a ->
        val s = Sira.guncelle(SiraDurumu(a.siraIndex, a.siraGunu), bugun)
        a.copy(siraIndex = s.index, siraGunu = s.gun)
    }

    /** "Bu adımı atla": yalnızca Kardiyo ve Adım atlanabilir. [bugun] takvimdeki bugündür. */
    suspend fun siraAtla(bugun: Long) = ayarlariDegistir { a ->
        val d = SiraDurumu(a.siraIndex, a.siraGunu)
        if (!Sira.atlanabilir(Sira.isAt(d.index))) return@ayarlariDegistir a
        val s = Sira.atla(d, bugun)
        a.copy(siraIndex = s.index, siraGunu = s.gun)
    }

    /**
     * Bir iş tamamlandı: sıradaki işle aynı türdeyse sıra ilerler.
     * [gun]: işin kaydedildiği gün ("Dün" seçiliyse dün); [gercekBugun]: takvimdeki bugün.
     */
    private suspend fun siraIlerlet(tur: String, gun: Long, gercekBugun: Long) {
        val a = dao.ayarlar() ?: return
        val d = SiraDurumu(a.siraIndex, a.siraGunu)
        if (Sira.isAt(d.index).kod == tur) {
            val s = Sira.guncelle(Sira.tamamla(d, gun), gercekBugun)
            dao.ayarlarYaz(a.copy(siraIndex = s.index, siraGunu = s.gun))
        }
    }

    // ---- Seanslar

    suspend fun seansBaslat(tur: String, kisaMod: Boolean, bugun: Long): Long =
        dao.seansEkle(Seans(gun = bugun, tur = tur, kisaMod = kisaMod))

    suspend fun seansBitir(id: Long, gun: Long, gercekBugun: Long = gun) = db.withTransaction {
        val s = dao.seans(id) ?: return@withTransaction
        dao.seansGuncelle(s.copy(gun = gun, tamamlandi = true, bitis = System.currentTimeMillis()))
        siraIlerlet(s.tur, gun, gercekBugun)
    }

    suspend fun seansSil(id: Long) = db.withTransaction {
        dao.seansSetleriniSil(id)
        dao.seansSil(id)
    }

    suspend fun kardiyoKaydet(tur: String, sureDk: Int, gun: Long, gercekBugun: Long = gun) = db.withTransaction {
        dao.seansEkle(
            Seans(gun = gun, tur = Tur.KARDIYO, kardiyoTuru = tur, sureDk = sureDk, tamamlandi = true, bitis = System.currentTimeMillis())
        )
        siraIlerlet(Tur.KARDIYO, gun, gercekBugun)
    }

    // ---- Beslenme

    /** Planlı öğünü "planlandığı gibi yedim" diye işaretler ya da işareti kaldırır. */
    suspend fun ogunIsaretle(ogun: Ogun, gun: Long, isaretli: Boolean) = db.withTransaction {
        dao.ogunKaydiniSil(gun, ogun.id)
        if (isaretli) {
            dao.yemekEkle(
                YemekKaydi(gun = gun, zaman = System.currentTimeMillis(), ad = ogun.ad, kcal = ogun.kcal.toDouble(), protein = ogun.protein.toDouble(), ogunId = ogun.id)
            )
        }
    }

    suspend fun hizliEkle(y: Yiyecek, gram: Double, gun: Long) {
        val oran = gram / y.bazGram
        dao.yemekEkle(
            YemekKaydi(gun = gun, zaman = System.currentTimeMillis(), ad = y.ad, gram = gram, kcal = y.kcal * oran, protein = y.protein * oran)
        )
    }

    // ---- Ölçümler

    /** Aynı güne ikinci kez girilirse eski değerin üzerine yazar. */
    suspend fun olcumKaydet(tur: String, gun: Long, deger: Double) = db.withTransaction {
        val eski = dao.olcum(tur, gun)
        dao.olcumYaz(eski?.copy(deger = deger) ?: Olcum(gun = gun, tur = tur, deger = deger))
    }
}
