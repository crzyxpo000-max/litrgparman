package com.zincir.veri

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Veritabanı sorguları. "Flow" dönenler canlıdır: tablo değişince ekran kendiliğinden yenilenir.
 * "suspend" olanlar bir kez çalışır.
 */
@Dao
interface TakipDao {
    // ---- Ayarlar
    @Query("SELECT * FROM ayarlar WHERE id = 1") fun ayarlarAkisi(): Flow<Ayarlar?>
    @Query("SELECT * FROM ayarlar WHERE id = 1") suspend fun ayarlar(): Ayarlar?
    @Upsert suspend fun ayarlarYaz(a: Ayarlar)

    // ---- Hareketler
    @Query("SELECT * FROM hareket ORDER BY seans, sira, id") fun hareketlerAkisi(): Flow<List<Hareket>>
    @Query("SELECT * FROM hareket WHERE seans = :seans ORDER BY sira, id") suspend fun hareketler(seans: String): List<Hareket>
    @Query("SELECT COUNT(*) FROM hareket") suspend fun hareketSayisi(): Int
    @Upsert suspend fun hareketYaz(h: Hareket)
    @Insert suspend fun hareketleriEkle(h: List<Hareket>)
    @Delete suspend fun hareketSil(h: Hareket)

    // ---- Seanslar ve setler
    @Query("SELECT * FROM seans ORDER BY gun DESC, id DESC") fun seanslarAkisi(): Flow<List<Seans>>
    @Query("SELECT * FROM seans WHERE tamamlandi = 0 AND tur IN ('A','B') ORDER BY id DESC LIMIT 1")
    fun aktifSeansAkisi(): Flow<Seans?>
    @Query("SELECT * FROM seans WHERE id = :id") suspend fun seans(id: Long): Seans?
    @Query("SELECT COUNT(*) FROM seans") suspend fun seansSayisi(): Int
    @Insert suspend fun seansEkle(s: Seans): Long
    @Update suspend fun seansGuncelle(s: Seans)
    @Query("DELETE FROM seans WHERE id = :id") suspend fun seansSil(id: Long)
    @Query("DELETE FROM set_kaydi WHERE seansId = :seansId") suspend fun seansSetleriniSil(seansId: Long)

    @Query("SELECT * FROM set_kaydi WHERE seansId = :seansId ORDER BY hareketId, setNo") fun setlerAkisi(seansId: Long): Flow<List<SetKaydi>>
    @Query("SELECT COUNT(*) FROM set_kaydi WHERE seansId = :seansId") suspend fun setSayisi(seansId: Long): Int
    @Insert suspend fun setEkle(s: SetKaydi)
    @Delete suspend fun setSil(s: SetKaydi)

    /** Bu adla kaydedilen hareketin, verilen seanstan önceki son tamamlanmış seanstaki setleri. */
    @Query(
        """SELECT * FROM set_kaydi WHERE hareketAdi = :ad AND seansId = (
             SELECT sk.seansId FROM set_kaydi sk JOIN seans s ON s.id = sk.seansId
             WHERE sk.hareketAdi = :ad AND s.tamamlandi = 1 AND s.id != :haricSeansId
             ORDER BY s.gun DESC, s.id DESC LIMIT 1)
           ORDER BY setNo"""
    )
    suspend fun oncekiSetler(ad: String, haricSeansId: Long): List<SetKaydi>

    @Query("SELECT MAX(gun) FROM seans WHERE tamamlandi = 1 AND tur IN ('A','B')")
    fun sonAgirlikGunuAkisi(): Flow<Long?>

    @Query("SELECT DISTINCT hareketAdi FROM set_kaydi ORDER BY hareketAdi") fun kayitliHareketAdlari(): Flow<List<String>>

    /** Hareket geçmişi grafiği: seans başına en yüksek kg (ağırlıksız hareketlerde en yüksek tekrar/süre). */
    @Query(
        """SELECT s.gun AS gun, MAX(sk.kg) AS kg, MAX(sk.tekrar) AS tekrar FROM set_kaydi sk
           JOIN seans s ON s.id = sk.seansId
           WHERE sk.hareketAdi = :ad AND s.tamamlandi = 1 GROUP BY s.id ORDER BY s.gun, s.id"""
    )
    fun hareketGecmisi(ad: String): Flow<List<HareketNoktasi>>

    // ---- Öğünler, yiyecekler, yemek kayıtları
    @Query("SELECT * FROM ogun ORDER BY sira, id") fun ogunlerAkisi(): Flow<List<Ogun>>
    @Upsert suspend fun ogunYaz(o: Ogun)
    @Insert suspend fun ogunleriEkle(o: List<Ogun>)
    @Delete suspend fun ogunSil(o: Ogun)

    @Query("SELECT * FROM yiyecek ORDER BY id") fun yiyeceklerAkisi(): Flow<List<Yiyecek>>
    @Upsert suspend fun yiyecekYaz(y: Yiyecek)
    @Insert suspend fun yiyecekleriEkle(y: List<Yiyecek>)
    @Delete suspend fun yiyecekSil(y: Yiyecek)

    @Query("SELECT * FROM yemek_kaydi WHERE gun = :gun ORDER BY zaman, id") fun yemekKayitlariAkisi(gun: Long): Flow<List<YemekKaydi>>
    @Query("SELECT * FROM yemek_kaydi WHERE gun BETWEEN :ilk AND :son") fun yemekAraligiAkisi(ilk: Long, son: Long): Flow<List<YemekKaydi>>
    @Query("SELECT * FROM yemek_kaydi WHERE gun = :gun") suspend fun yemekKayitlari(gun: Long): List<YemekKaydi>
    /** Son yemek zamanı (pişirme yağı payı gibi "pay" satırları sayılmaz). */
    @Query("SELECT MAX(zaman) FROM yemek_kaydi WHERE ogunId IS NULL OR ogunId NOT IN (SELECT id FROM ogun WHERE pay = 1)")
    suspend fun sonYemekZamani(): Long?
    @Query("SELECT * FROM ogun ORDER BY sira, id") suspend fun ogunler(): List<Ogun>
    @Insert suspend fun yemekEkle(y: YemekKaydi)
    @Delete suspend fun yemekSil(y: YemekKaydi)
    @Query("DELETE FROM yemek_kaydi WHERE gun = :gun AND ogunId = :ogunId") suspend fun ogunKaydiniSil(gun: Long, ogunId: Long)

    // ---- Germeler
    @Query("SELECT * FROM germe ORDER BY sira, id") fun germelerAkisi(): Flow<List<Germe>>
    @Upsert suspend fun germeYaz(g: Germe)
    @Insert suspend fun germeleriEkle(g: List<Germe>)
    @Delete suspend fun germeSil(g: Germe)

    // ---- Gün kayıtları
    @Query("SELECT * FROM gun_kaydi WHERE gun = :gun") fun gunAkisi(gun: Long): Flow<GunKaydi?>
    @Query("SELECT * FROM gun_kaydi WHERE gun = :gun") suspend fun gun(gun: Long): GunKaydi?
    @Query("SELECT * FROM gun_kaydi WHERE gun BETWEEN :ilk AND :son ORDER BY gun") fun gunAraligiAkisi(ilk: Long, son: Long): Flow<List<GunKaydi>>
    @Query("SELECT * FROM gun_kaydi WHERE gun BETWEEN :ilk AND :son ORDER BY gun") suspend fun gunAraligi(ilk: Long, son: Long): List<GunKaydi>
    @Upsert suspend fun gunYaz(g: GunKaydi)

    // ---- Ölçümler
    @Query("SELECT * FROM olcum WHERE tur = :tur ORDER BY gun, id") fun olcumlerAkisi(tur: String): Flow<List<Olcum>>
    @Query("SELECT * FROM olcum WHERE tur = :tur AND gun = :gun LIMIT 1") suspend fun olcum(tur: String, gun: Long): Olcum?
    @Upsert suspend fun olcumYaz(o: Olcum)
    @Delete suspend fun olcumSil(o: Olcum)

    // ---- Kaçamaklar
    @Query("SELECT * FROM kacamak WHERE gun BETWEEN :ilk AND :son ORDER BY gun, id") fun kacamaklarAkisi(ilk: Long, son: Long): Flow<List<Kacamak>>
    @Insert suspend fun kacamakEkle(k: Kacamak)
    @Delete suspend fun kacamakSil(k: Kacamak)

    // ---- Eğer-o zaman planları
    @Query("SELECT * FROM eger_plan ORDER BY sira, id") fun egerPlanlarAkisi(): Flow<List<EgerPlan>>
    @Upsert suspend fun egerPlanYaz(p: EgerPlan)
    @Insert suspend fun egerPlanlariEkle(p: List<EgerPlan>)
    @Delete suspend fun egerPlanSil(p: EgerPlan)

    // ---- Haftalık gözden geçirme
    @Query("SELECT * FROM hafta_gozden WHERE haftaBasi = :haftaBasi") fun gozdenAkisi(haftaBasi: Long): Flow<HaftaGozden?>
    @Upsert suspend fun gozdenYaz(g: HaftaGozden)
}

/** [TakipDao.hareketGecmisi] sorgusunun bir satırı. */
data class HareketNoktasi(val gun: Long, val kg: Double, val tekrar: Int)
