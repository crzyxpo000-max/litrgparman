package com.yukselis.engine.story

import com.yukselis.engine.i18n.LText

/**
 * Motorun ürettiği Sistem bildirimlerinin kalıp metinleri (content/system.json).
 * Yer tutucular adlandırılmıştır: "+{n} XP", "Seviye {from} → {to}".
 * Yer tutucu değerleri iki dilde ayrı verilebilir (unvan adı gibi).
 */
class SystemTexts(private val texts: Map<String, LText>) {

    val keys: Set<String> get() = texts.keys

    operator fun get(key: String): LText? = texts[key]

    /** Değerler dilden bağımsız (sayılar). */
    fun format(key: String, vararg args: Pair<String, Any>): LText =
        formatL(key, *args.map { (k, v) -> k to LText(v.toString(), v.toString()) }.toTypedArray())

    /** Değerler dile göre değişir (unvan, sınıf, yetenek adları). */
    fun formatL(key: String, vararg args: Pair<String, LText>): LText {
        val template = texts[key] ?: LText("[$key]", "[$key]")
        var tr = template.tr
        var en = template.en
        for ((name, value) in args) {
            tr = tr.replace("{$name}", value.tr)
            en = en.replace("{$name}", value.en)
        }
        return LText(tr, en)
    }

    companion object {
        val EMPTY = SystemTexts(emptyMap())

        /** Motorun kullandığı anahtarlar ve beklenen yer tutucular; doğrulayıcı denetler. */
        val REQUIRED: Map<String, Set<String>> = mapOf(
            "xp_kazanildi" to setOf("n"),
            "seviye_atladin" to emptySet(),
            "seviye_satiri" to setOf("from", "to"),
            "serbest_puan" to setOf("n"),
            "can_mana_yenilendi" to emptySet(),
            "stat_degisti" to setOf("stat", "delta"),
            "unvan_kazanildi" to setOf("name"),
            "unvan_bonus" to setOf("bonus"),
            "unvan_alinmis" to setOf("name"),
            "sinif_teklifi" to emptySet(),
            "sinif_teklifi_satir" to emptySet(),
            "sinif_kazanildi" to setOf("name"),
            "seviye_durdu" to setOf("grade"),
            "evrim_gorevi" to setOf("grade"),
            "evrim_gorevi_satir" to setOf("grade"),
            "evrim_tamam" to setOf("name", "grade"),
            "genel_evrim" to setOf("grade"),
            "genel_evrim_aciklama" to setOf("n"),
            "yetenek_kazanildi" to setOf("name"),
            "yetenek_seviye" to setOf("name", "level"),
            "yetenek_evrim_teklifi" to setOf("name"),
            "yetenek_evrildi" to setOf("from", "to"),
            "kaynastirma_teklifi" to setOf("a", "b", "result"),
            "kaynastirma_tamam" to setOf("result"),
            "ceza_yetenegi" to setOf("penalty"),
            "esya_kazanildi" to setOf("name", "n"),
            "esya_kaybedildi" to setOf("name", "n"),
            "kredi" to setOf("n"),
            "oz_kristali" to setOf("n"),
            "oz_kavrayis" to setOf("name", "n"),
            "oz_baglama_teklifi" to setOf("name"),
            "oz_baglandi" to setOf("name"),
            "meslek_teklifi" to emptySet(),
            "meslek_teklifi_satir" to emptySet(),
            "meslek_secildi" to setOf("name"),
            "meslek_seviye" to setOf("name", "level"),
            "uretildi" to setOf("name", "n"),
            "kullanildi" to setOf("name"),
            "gunluk_yeni" to setOf("title"),
            "gunluk_tamam" to emptySet(),
            "gunluk_odul" to setOf("n", "xp"),
            "gunluk_kacirildi" to emptySet(),
            "yanki_onerisi" to emptySet(),
            "benzersiz_yetenek" to emptySet(),
            "benzersiz_yetenek_satir" to emptySet(),
            "sekme_yetenekler" to emptySet(),
            "sekme_canta" to emptySet(),
            "sekme_harita" to emptySet(),
            "kabul" to emptySet(),
            "red" to emptySet(),
        )
    }
}
