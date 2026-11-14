package com.zincir.veri

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Veritabanı tabloları. Her sınıf bir tablo, her alan bir sütundur.
 * Tarihler "gün numarası" (LocalDate.toEpochDay) olarak saklanır: karşılaştırması ve aralık sorgusu kolaydır.
 */

/** Tek satırlık tablo: hedefler, sıra konumu, bildirim tercihleri. */
@Entity(tableName = "ayarlar")
data class Ayarlar(
    @PrimaryKey val id: Int = 1,
    val baslangicGunu: Long,            // program başlangıç tarihi
    val baslangicKilo: Double = 96.0,
    val hedefKiloAlt: Double = 80.0,
    val hedefKiloUst: Double = 83.0,
    val kaloriHedef: Int = 2250,
    val proteinMin: Int = 150,
    val proteinMax: Int = 160,
    val adimMin: Int = 8000,
    val adimMax: Int = 10000,
    val suHedefMl: Int = 3000,
    val germeHedefDk: Int = 10,
    val uykuHedefSaat: Double = 7.0,
    val haftalikSeansHedef: Int = 3,
    val haftalikGermeHedef: Int = 5,
    // Sıra: döngüde kaçıncı işteyiz ve bu iş hangi günden beri bekliyor.
    val siraIndex: Int = 0,
    val siraGunu: Long,
    // Hafif hafta hangi program haftasında uygulanıyor (-1: hiçbiri).
    val hafifHaftaIndex: Int = -1,
    // Bildirimler (saatler gece yarısından itibaren dakika cinsinden).
    val bildirimTarti: Boolean = false,
    val tartiDakika: Int = 7 * 60 + 30,
    val bildirimGerme: Boolean = false,
    val germeDakika: Int = 21 * 60,
    val bildirimOgun: Boolean = false,
    val ogunAralikSaat: Int = 5,
    val tema: Int = 0,                  // 0 sistem, 1 açık, 2 koyu
)

/** Programdaki bir hareket (Seans A ya da B). */
@Entity(tableName = "hareket")
data class Hareket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seans: String,                  // "A" ya da "B"
    val sira: Int,
    val ad: String,
    val alternatifAd: String? = null,   // "makine yok" seçeneği
    val setSayisi: Int,
    val tekrarMin: Int,
    val tekrarMax: Int,
    val birim: String = "tekrar",       // "tekrar" ya da "sn"
    val ek: String = "",                // "her bacak", "her biri" gibi
    val dinlenmeSn: Int,
    val agirlikli: Boolean = true,      // kg girilir mi
)

/** Yapılan bir seans: ağırlık (A/B) ya da kardiyo. */
@Entity(tableName = "seans", indices = [Index("gun")])
data class Seans(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gun: Long,
    val tur: String,                    // "A", "B", "KARDIYO"
    val kisaMod: Boolean = false,       // 15 dakika modu
    val kardiyoTuru: String? = null,
    val sureDk: Int? = null,
    val tamamlandi: Boolean = false,
    val bitis: Long? = null,            // bitiş anı (milisaniye)
)

/** Bir seansta kaydedilen tek set. */
@Entity(tableName = "set_kaydi", indices = [Index("seansId"), Index("hareketAdi")])
data class SetKaydi(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seansId: Long,
    val hareketId: Long,
    val hareketAdi: String,             // alternatif seçildiyse onun adı
    val setNo: Int,
    val kg: Double,
    val tekrar: Int,
)

/** Plandaki bir öğün. */
@Entity(tableName = "ogun")
data class Ogun(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sira: Int,
    val ad: String,
    val icerik: String,
    val kcal: Int,
    val protein: Int,
    val pay: Boolean = false,           // gerçek öğün değil, pay (pişirme yağı); öğün hatırlatmasında sayılmaz
)

/** Hızlı ekleme için referans yiyecek: [bazGram] gram = [kcal] kcal, [protein] g protein. */
@Entity(tableName = "yiyecek")
data class Yiyecek(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ad: String,
    val bazGram: Double,
    val kcal: Double,
    val protein: Double,
)

/** Yenen bir şey: planlı öğün (ogunId dolu) ya da hızlı ekleme. */
@Entity(tableName = "yemek_kaydi", indices = [Index("gun")])
data class YemekKaydi(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gun: Long,
    val zaman: Long,
    val ad: String,
    val gram: Double? = null,
    val kcal: Double,
    val protein: Double,
    val ogunId: Long? = null,
)

/** Rutindeki bir germe. */
@Entity(tableName = "germe")
data class Germe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sira: Int,
    val ad: String,
    val bolge: String,
    val sureSn: Int = 60,
    val ikiTaraf: Boolean = false,
)

/** Bir günün kaydı. */
@Entity(tableName = "gun_kaydi")
data class GunKaydi(
    @PrimaryKey val gun: Long,
    val ucusGunu: Boolean = false,
    val ertelendi: Boolean = false,     // ağırlık seansı yarına kaydırıldı
    val uykuSaat: Double? = null,
    val adim: Int = 0,
    val suMl: Int = 0,
    val germeDk: Int = 0,
)

/** Ölçüm: tur = "KILO", "BEL" ya da "ESNEKLIK" (parmak ucu–yer, cm). */
@Entity(tableName = "olcum", indices = [Index("tur", "gun")])
data class Olcum(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gun: Long,
    val tur: String,
    val deger: Double,
)

/** "Haftada en fazla 1" listesinden yenen şey. */
@Entity(tableName = "kacamak")
data class Kacamak(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gun: Long,
    val ad: String,
)

/** "Eğer-o zaman" planı. */
@Entity(tableName = "eger_plan")
data class EgerPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sira: Int,
    val metin: String,
)

/** Haftalık gözden geçirme. [haftaBasi] gözden geçirilen haftanın ilk günü. */
@Entity(tableName = "hafta_gozden")
data class HaftaGozden(
    @PrimaryKey val haftaBasi: Long,
    val isaretler: String = "",         // işaretlenen maddelerin numaraları: "0,2,3"
    val planGunleri: String = "",       // gelecek hafta seans günleri (gün numaraları): "20790,20792"
    val notMetni: String = "",
)

object Tur {
    const val A = "A"
    const val B = "B"
    const val KARDIYO = "KARDIYO"
    const val KILO = "KILO"
    const val BEL = "BEL"
    const val ESNEKLIK = "ESNEKLIK"
}
