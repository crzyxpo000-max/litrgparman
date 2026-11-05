package com.yukselis.engine

import com.yukselis.engine.core.GameClock
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.MoralAxis
import com.yukselis.engine.core.Morality
import com.yukselis.engine.core.Rng
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RngTest {
    @Test
    fun ayniTohumAyniDiziyiUretir() {
        val a = Rng(42)
        val b = Rng(42)
        repeat(1000) { assertEquals(a.nextLong(), b.nextLong()) }
    }

    @Test
    fun farkliTohumFarkliDiziUretir() {
        assertNotEquals(Rng(1).nextLong(), Rng(2).nextLong())
    }

    @Test
    fun durumKaydedilipDevamEttirilebilir() {
        val a = Rng(7)
        repeat(10) { a.nextLong() }
        val saved = a.state
        val expected = List(20) { a.nextInt(100) }
        val restored = Rng(saved)
        assertEquals(expected, List(20) { restored.nextInt(100) })
    }

    @Test
    fun araliklarSinirlarinIcindeKalir() {
        val r = Rng(123)
        repeat(10_000) {
            assertTrue(r.d20() in 1..20)
            assertTrue(r.nextInt(5) in 0..4)
            val d = r.nextDouble()
            assertTrue(d >= 0.0 && d < 1.0)
        }
    }

    @Test
    fun d20DagilimiKabacaEsit() {
        val r = Rng(99)
        val counts = IntArray(21)
        repeat(20_000) { counts[r.d20()]++ }
        for (face in 1..20) assertTrue("yüz $face: ${counts[face]}", counts[face] in 800..1200)
    }
}

class ProgressionTest {
    @Test
    fun xpFormuluGddIleAyni() {
        assertEquals(100L, Progression.xpToNext(1))
        assertEquals(348L, Progression.xpToNext(2)) // ⌊100 · 2^1.8⌋ = ⌊348.22⌋
        assertEquals(6309L, Progression.xpToNext(10))
    }

    @Test
    fun turetilmisDegerler() {
        val s = Stats()
        assertEquals(50 + 10 * 5 + 5 * 1, Progression.maxHp(s, 1))
        assertEquals(30 + 8 * 5 + 3 * 5, Progression.maxMp(s))
        assertEquals(40 + 5 * 5 + 5 * 5, Progression.maxStamina(s))
    }

    @Test
    fun kademeEsikleri() {
        assertEquals(Grade.F, Grade.forLevel(1))
        assertEquals(Grade.F, Grade.forLevel(24))
        assertEquals(Grade.E, Grade.forLevel(25))
        assertEquals(Grade.C, Grade.forLevel(199))
        assertEquals(Grade.S, Grade.forLevel(800))
    }

    @Test
    fun seviyeFarkiCarpaniSinirli() {
        assertEquals(1.0, Progression.xpMultiplier(5, 5), 1e-9)
        assertEquals(1.15, Progression.xpMultiplier(6, 5), 1e-9)
        assertEquals(5.0, Progression.xpMultiplier(100, 1), 1e-9)
        assertEquals(0.1, Progression.xpMultiplier(1, 100), 1e-9)
    }
}

class ClockAndMoralityTest {
    private val base = WorldState.newGame(1, "Test", GameMode.NORMAL, "x")

    @Test
    fun perdelerGunlereGoreBelirlenir() {
        assertEquals(1, GameClock.actFor(0))
        assertEquals(1, GameClock.actFor(30))
        assertEquals(2, GameClock.actFor(31))
        assertEquals(3, GameClock.actFor(181))
        assertEquals(4, GameClock.actFor(365))
    }

    @Test
    fun takvim365GundeDurur() {
        assertEquals(365, GameClock.advance(base, 1000).day)
        assertEquals(0, GameClock.advance(base, -5).day)
    }

    @Test
    fun ahlakEksenleriSinirli() {
        val m = Morality().shift(MoralAxis.MERCY, 250).shift(MoralAxis.ORDER, -300)
        assertEquals(100, m.mercy)
        assertEquals(-100, m.order)
        assertEquals(0, m.humanity)
    }
}
