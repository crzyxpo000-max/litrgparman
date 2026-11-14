package com.zincir.veri

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

/**
 * Yedekleme: bütün tablolar tek bir JSON dosyasına yazılır ve oradan geri yüklenir.
 * Tablolar sütun adlarıyla, olduğu gibi aktarılır; yeni bir tablo eklersen adını [TABLOLAR] listesine yaz.
 */
object Yedek {
    const val UYGULAMA = "zincir"
    const val SURUM = 1

    val TABLOLAR = listOf(
        "ayarlar", "hareket", "seans", "set_kaydi", "ogun", "yiyecek", "yemek_kaydi",
        "germe", "gun_kaydi", "olcum", "kacamak", "eger_plan", "hafta_gozden",
    )

    suspend fun disaAktar(db: Veritabani): String = withContext(Dispatchers.IO) {
        val sdb = db.openHelper.readableDatabase
        val tablolar = JSONObject()
        for (tablo in TABLOLAR) {
            val satirlar = JSONArray()
            sdb.query("SELECT * FROM $tablo").use { c ->
                while (c.moveToNext()) {
                    val satir = JSONObject()
                    for (i in 0 until c.columnCount) {
                        val ad = c.getColumnName(i)
                        when (c.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> satir.put(ad, JSONObject.NULL)
                            Cursor.FIELD_TYPE_INTEGER -> satir.put(ad, c.getLong(i))
                            Cursor.FIELD_TYPE_FLOAT -> satir.put(ad, c.getDouble(i))
                            else -> satir.put(ad, c.getString(i))
                        }
                    }
                    satirlar.put(satir)
                }
            }
            tablolar.put(tablo, satirlar)
        }
        JSONObject()
            .put("uygulama", UYGULAMA)
            .put("surum", SURUM)
            .put("tarih", LocalDateTime.now().toString())
            .put("tablolar", tablolar)
            .toString(2)
    }

    /** Mevcut veriyi siler ve yedektekini yazar. Dosya geçersizse hata fırlatır ve hiçbir şey değişmez. */
    suspend fun iceAktar(db: Veritabani, json: String) = withContext(Dispatchers.IO) {
        val kok = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Dosya okunamadı: geçerli bir JSON değil.")
        }
        require(kok.optString("uygulama") == UYGULAMA) { "Bu dosya bir Zincir yedeği değil." }
        val tablolar = kok.optJSONObject("tablolar") ?: throw IllegalArgumentException("Yedekte tablo bulunamadı.")
        require(tablolar.optJSONArray("ayarlar")?.length() == 1) { "Yedekte ayarlar eksik." }

        db.runInTransaction {
            val sdb = db.openHelper.writableDatabase
            for (tablo in TABLOLAR) {
                val satirlar = tablolar.optJSONArray(tablo) ?: continue
                // Tablodaki gerçek sütunlar: yedekteki bilinmeyen alanlar atlanır.
                val sutunlar = buildSet {
                    sdb.query("PRAGMA table_info($tablo)").use { c -> while (c.moveToNext()) add(c.getString(1)) }
                }
                sdb.execSQL("DELETE FROM $tablo")
                for (i in 0 until satirlar.length()) {
                    val satir = satirlar.getJSONObject(i)
                    val degerler = ContentValues()
                    for (ad in satir.keys()) {
                        if (ad !in sutunlar) continue
                        when (val v = satir.get(ad)) {
                            JSONObject.NULL -> degerler.putNull(ad)
                            is Boolean -> degerler.put(ad, if (v) 1 else 0)
                            is Int -> degerler.put(ad, v.toLong())
                            is Long -> degerler.put(ad, v)
                            is Number -> degerler.put(ad, v.toDouble())
                            else -> degerler.put(ad, v.toString())
                        }
                    }
                    sdb.insert(tablo, SQLiteDatabase.CONFLICT_REPLACE, degerler)
                }
            }
        }
    }
}
