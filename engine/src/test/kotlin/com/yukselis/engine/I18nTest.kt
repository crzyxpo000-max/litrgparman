package com.yukselis.engine

import com.yukselis.engine.i18n.EnglishGrammar
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.i18n.NameEntry
import com.yukselis.engine.i18n.NameRegistry
import com.yukselis.engine.i18n.TokenResolver
import com.yukselis.engine.i18n.TurkishSuffix
import com.yukselis.engine.i18n.TurkishSuffix.Case
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurkishSuffixTest {
    private fun s(word: String, case: Case) = TurkishSuffix.apply(word, case)

    @Test
    fun unsuzleBitenKalinVeIncelUnluler() {
        assertEquals("Deniz'e", s("Deniz", Case.DATIVE))
        assertEquals("Deniz'i", s("Deniz", Case.ACCUSATIVE))
        assertEquals("Deniz'in", s("Deniz", Case.GENITIVE))
        assertEquals("Deniz'de", s("Deniz", Case.LOCATIVE))
        assertEquals("Deniz'den", s("Deniz", Case.ABLATIVE))
        assertEquals("Deniz'le", s("Deniz", Case.INSTRUMENTAL))
        assertEquals("Thessar'a", s("Thessar", Case.DATIVE))
        assertEquals("Thessar'ın", s("Thessar", Case.GENITIVE))
    }

    @Test
    fun unluyleBitenlerdeKaynastirma() {
        assertEquals("Kaya'ya", s("Kaya", Case.DATIVE))
        assertEquals("Kaya'yı", s("Kaya", Case.ACCUSATIVE))
        assertEquals("Kaya'nın", s("Kaya", Case.GENITIVE))
        assertEquals("Kaya'yla", s("Kaya", Case.INSTRUMENTAL))
        assertEquals("Yankı'ya", s("Yankı", Case.DATIVE))
        assertEquals("Yankı'nın", s("Yankı", Case.GENITIVE))
    }

    @Test
    fun sertlesme() {
        assertEquals("Aras'ta", s("Aras", Case.LOCATIVE))
        assertEquals("Aras'tan", s("Aras", Case.ABLATIVE))
        assertEquals("Mert'te", s("Mert", Case.LOCATIVE))
        assertEquals("Mert'ten", s("Mert", Case.ABLATIVE))
        assertEquals("Elif'te", s("Elif", Case.LOCATIVE))
    }

    @Test
    fun yuvarlakUnluler() {
        assertEquals("Umut'u", s("Umut", Case.ACCUSATIVE))
        assertEquals("Ölmez'in", s("Ölmez", Case.GENITIVE))
        assertEquals("Gönül'ün", s("Gönül", Case.GENITIVE))
        assertEquals("Kıyıkent'e", s("Kıyıkent", Case.DATIVE))
    }

    @Test
    fun tamAdlardaEkSonKelimeyeGore() {
        assertEquals("Deniz Arman'a", s("Deniz Arman", Case.DATIVE))
        assertEquals("Kaya Demir'in", s("Kaya Demir", Case.GENITIVE))
    }
}

class EnglishGrammarTest {
    @Test
    fun article() {
        assertEquals("a rat", EnglishGrammar.article("rat"))
        assertEquals("an envoy", EnglishGrammar.article("envoy"))
        assertEquals("an hour", EnglishGrammar.article("hour"))
        assertEquals("a unique skill", EnglishGrammar.article("unique skill"))
    }

    @Test
    fun plural() {
        assertEquals("gate", EnglishGrammar.plural("gate", 1))
        assertEquals("gates", EnglishGrammar.plural("gate", 2))
        assertEquals("allies", EnglishGrammar.plural("ally", 3))
        assertEquals("boxes", EnglishGrammar.plural("box", 0))
        assertEquals("days", EnglishGrammar.plural("day", 29))
    }
}

class TokenResolverTest {
    private val names = NameRegistry(
        listOf(
            NameEntry("deniz_arman", NameEntry.Kind.NPC, LText("Deniz Arman", "Devon Hale"), LText("Deniz", "Devon")),
            NameEntry("kiyikent", NameEntry.Kind.PLACE, LText("Kıyıkent", "Port Harlow"), LText("Kıyıkent", "Port Harlow")),
        ),
    )
    private val r = TokenResolver(names)

    @Test
    fun isimlerDileGoreDegisir() {
        assertEquals("Deniz geldi.", r.resolve("{npc:deniz_arman} geldi.", Lang.TR, "Arda"))
        assertEquals("Devon is here.", r.resolve("{npc:deniz_arman} is here.", Lang.EN, "Alex"))
        assertEquals("Port Harlow", r.resolve("{yer:kiyikent}", Lang.EN, "Alex"))
    }

    @Test
    fun ekleVeTamAd() {
        assertEquals("Deniz'in eli", r.resolve("{npc:deniz_arman|in} eli", Lang.TR, "x"))
        assertEquals("Deniz Arman'a", r.resolve("{npc:deniz_arman|tam|e}", Lang.TR, "x"))
        assertEquals("Devon Hale", r.resolve("{npc:deniz_arman|tam|e}", Lang.EN, "x"))
        assertEquals("Devon's hand", r.resolve("{npc:deniz_arman|pos} hand", Lang.EN, "x"))
        assertEquals("Kıyıkent'e", r.resolve("{yer:kiyikent|e}", Lang.TR, "x"))
    }

    @Test
    fun oyuncuAdi() {
        assertEquals("Arda'nın", r.resolve("{oyuncu|in}", Lang.TR, "Arda"))
        assertEquals("Hi Alex", r.resolve("Hi {oyuncu}", Lang.EN, "Alex"))
    }

    @Test
    fun cozulemeyenBelirtecGorunurKalirVeRaporlanir() {
        assertEquals("[?npc:yok] geldi", r.resolve("{npc:yok} geldi", Lang.TR, "x"))
        assertEquals(listOf("{npc:yok}"), r.unresolvedTokens("{npc:yok} ve {npc:deniz_arman}"))
        assertTrue(r.unresolvedTokens("{oyuncu} {yer:kiyikent}").isEmpty())
    }

    @Test
    fun dilKodlari() {
        assertEquals(Lang.TR, Lang.fromCode("tr-TR"))
        assertEquals(Lang.EN, Lang.fromCode("en_US"))
        assertEquals(Lang.EN, Lang.fromCode("de"))
        assertEquals(Lang.EN, Lang.fromCode(null))
    }

    @Test
    fun eksikCeviriDigerDileDuser() {
        val t = LText("", "Only English")
        assertEquals("Only English", t[Lang.TR])
        assertTrue(!t.isComplete)
    }
}
