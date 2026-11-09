package com.yukselis.engine

import com.yukselis.engine.core.WorldState
import com.yukselis.engine.story.Condition
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Susturulmuş ya da silinmiş Yankı hiçbir sahnede konuşmaz: `yanki_bagi` koşulu yanlış döner (Paket 1, D4). */
class YankiSusturmaTest {
    private fun state(vararg flags: String): WorldState = WorldState(seed = 1L, rngState = 1L, playerName = "Test", mode = com.yukselis.engine.core.GameMode.NORMAL, sceneId = "x", flags = flags.toSet())

    @Test
    fun `bağ varken yanki_bagi doğru`() {
        val s = state(WorldState.ECHO_BOND)
        assertTrue(s.hasFlag("yanki_bagi"))
        assertTrue(Condition.HasFlag("yanki_bagi").test(s))
        assertFalse(Condition.LacksFlag("yanki_bagi").test(s))
    }

    @Test
    fun `susturulmuş Yankı için yanki_bagi yanlış, bayrak_yok doğru`() {
        val s = state(WorldState.ECHO_BOND, WorldState.ECHO_SILENCED)
        assertFalse(s.hasFlag("yanki_bagi"))
        assertTrue(Condition.LacksFlag("yanki_bagi").test(s))
        // Susturma bayrağının kendisi görünür kalır (yapısal sahneler ona bakar).
        assertTrue(s.hasFlag("yanki_susturuldu"))
    }

    @Test
    fun `silinmiş Yankı için yanki_bagi yanlış`() {
        val s = state(WorldState.ECHO_BOND, WorldState.ECHO_DELETED)
        assertFalse(s.hasFlag("yanki_bagi"))
    }

    @Test
    fun `kapı aralanınca bağ geri gelir`() {
        val s = state(WorldState.ECHO_BOND, WorldState.ECHO_SILENCED)
        val again = s.copy(flags = s.flags - WorldState.ECHO_SILENCED)
        assertTrue(again.hasFlag("yanki_bagi"))
    }

    @Test
    fun `öteki bayraklar etkilenmez`() {
        val s = state("deniz_kurtarildi", WorldState.ECHO_SILENCED)
        assertTrue(s.hasFlag("deniz_kurtarildi"))
        assertFalse(s.hasFlag("yok_boyle_bayrak"))
    }
}
