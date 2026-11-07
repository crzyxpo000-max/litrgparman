package com.yukselis.uslup

import com.yukselis.uslup.Checker.StyleRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckerTest {
    private fun yaml(tr: String, en: String = "You walk.", comment: String = "# yorum") = """
        $comment
        sahneler:
          - id: "s1"
            paragraflar:
              - metin: { tr: "$tr", en: "$en" }
                kosul: { bolge: "orman" }
    """.trimIndent()

    private fun errors(r: Checker.FileReport) = r.findings.filter { it.error }.map { it.message }

    @Test
    fun `yalnız tr değişince hata yok`() {
        val r = Checker.compare("a.yaml", yaml("Kapıyı açıyorsun. İçeri giriyorsun."), yaml("Kapıyı itip içeri giriyorsun."))
        assertEquals(emptyList<String>(), errors(r))
        assertEquals(1, r.changed)
        assertEquals(1, r.total)
    }

    @Test
    fun `en değişirse hata`() {
        val r = Checker.compare("a.yaml", yaml("Yürüyorsun."), yaml("Yürüyorsun.", en = "You stroll."))
        assertTrue(errors(r).isNotEmpty())
    }

    @Test
    fun `yorum değişirse hata`() {
        val r = Checker.compare("a.yaml", yaml("Yürüyorsun."), yaml("Yürüyorsun.", comment = "# başka"))
        assertTrue(errors(r).any { "tr dışında" in it })
    }

    @Test
    fun `paragraf eklenirse hata`() {
        val old = yaml("Yürüyorsun.")
        val new = old + "\n      - metin: { tr: \"Ek.\", en: \"Extra.\" }"
        val r = Checker.compare("a.yaml", old, new)
        assertTrue(errors(r).any { "satır sayısı" in it })
        assertTrue(errors(r).any { "liste uzunluğu" in it })
    }

    @Test
    fun `belirteç eki değişebilir ama belirteç kaybolamaz`() {
        val ok = Checker.compare("a.yaml", yaml("{npc:deniz_arman} sana bakıyor."), yaml("Gözünü {npc:deniz_arman|e} dikiyorsun."))
        assertEquals(emptyList<String>(), errors(ok))
        val bad = Checker.compare("a.yaml", yaml("{npc:deniz_arman} sana bakıyor."), yaml("Sana bakıyor."))
        assertTrue(errors(bad).any { "belirteç" in it })
        val dup = Checker.compare("a.yaml", yaml("{npc:deniz_arman} bakıyor."), yaml("{npc:deniz_arman} {npc:deniz_arman|e} bakıyor."))
        assertTrue(errors(dup).any { "belirteç" in it })
    }

    @Test
    fun `Yankı satırı büyük harfe dönerse hata`() {
        val r = Checker.compare("a.yaml", yaml("tesadüf, diyor ses. hadi."), yaml("Tesadüf, diyor ses; hadi."))
        assertTrue(errors(r).any { "Yankı" in it })
        val ok = Checker.compare("a.yaml", yaml("{npc:yanki} fısıldıyor: kaç."), yaml("{npc:yanki} fısıldıyor: kaç artık."))
        assertEquals(emptyList<String>(), errors(ok))
    }

    @Test
    fun `bozuk yazı korunmalı`() {
        val r = Checker.compare("a.yaml", yaml("s̷aklanmayı bırak."), yaml("saklanmayı bırak."))
        assertTrue(errors(r).any { "bozuk" in it })
        val ok = Checker.compare("a.yaml", yaml("s̷aklanmayı bırak ████."), yaml("████, s̷aklanmayı bırak artık."))
        assertEquals(emptyList<String>(), errors(ok))
    }

    @Test
    fun `boş tr hata, çok kısa tr uyarı`() {
        assertTrue(errors(Checker.compare("a.yaml", yaml("Uzun bir cümle burada duruyor."), yaml(""))).any { "boş" in it })
        val r = Checker.compare("a.yaml", yaml("Uzun bir cümle burada duruyor, dahası var."), yaml("Kısa."))
        assertTrue(r.findings.any { !it.error && "kısa" in it.message })
    }

    @Test
    fun `üslup ölçümleri`() {
        assertTrue(StyleRule.YOR_RUN in Checker.style("Yürüyorsun. Bakıyorsun. Duruyorsun."))
        assertFalse(StyleRule.YOR_RUN in Checker.style("Yürüyorsun. Sis çökmüş. Duruyorsun."))
        assertTrue(StyleRule.YOR_RUN in Checker.style("Gidiyorlar! {npc:deniz_arman} koşuyor. \"Bekliyorum.\""))
        assertTrue(StyleRule.SEMICOLON in Checker.style("a; b; c."))
        assertFalse(StyleRule.SEMICOLON in Checker.style("[Sistem; kuru; ses]"))
        assertTrue(StyleRule.BU_COMMA in Checker.style("Güveniyor. Bu, korkudan ağır."))
        assertTrue(StyleRule.ETRAFINDA in Checker.style("Yolunu Ateş'in etrafında kurdun."))
        assertTrue(StyleRule.KOMIK in Checker.style("neredeyse komik derecede kolay"))
        assertTrue(StyleRule.AYNI_ANDA in Checker.style("Gülüyor ve ağlıyor, aynı anda."))
        assertTrue(StyleRule.VE_COMMA in Checker.style("Elma, armut, ve kiraz."))
        assertTrue(StyleRule.SHORT_RUN in Checker.style("Üç adım. Dört. Sis seni yutuyor ve bacakların koşuyor."))
        assertTrue(StyleRule.SHORT_RUN in Checker.style("Acelesi yok. Hiç olmadı."))
        assertFalse(StyleRule.SHORT_RUN in Checker.style("Üç adım, dört adım… Sis seni içine çekerken bacakların kaçıyor."))
        assertTrue(Checker.style("Sis toprağa çökmüş, ağır ağır sürünüyor.").isEmpty())
    }

    @Test
    fun `kronik en yalniz izinle degisebilir`() {
        val old = "secenekler:\n  - id: \"a\"\n    kronik: { tr: \"Kaçtın.\", en: \"You ran.\" }\n"
        val new = "secenekler:\n  - id: \"a\"\n    kronik: { tr: \"Kaçıp gittin.\", en: \"You ran for it.\" }\n"
        try {
            assertTrue(errors(Checker.compare("a.yaml", old, new)).isNotEmpty())
            Checker.allowKronikEn = true
            assertEquals(emptyList<String>(), errors(Checker.compare("a.yaml", old, new)))
            val para = "p:\n  - { tr: \"A.\", en: \"A.\" }\n"
            assertTrue(errors(Checker.compare("a.yaml", para, para.replace("en: \"A.\"", "en: \"B.\""))).isNotEmpty())
        } finally {
            Checker.allowKronikEn = false
        }
    }

    @Test
    fun `belirteç ayrıştırma`() {
        assertEquals(listOf("npc:@", "npc:deniz_arman", "oyuncu"), Checker.tokens("{oyuncu|in} {npc:deniz_arman|tam} {npc:@}"))
    }
}
