package com.yukselis.engine.core

/**
 * Oyundaki tüm rastgelelik bu tohumlanmış üreteçten geçer (SplitMix64).
 * Durumu tek bir Long'dur; WorldState.rngState içinde saklanır, böylece aynı kayıt aynı sonucu üretir.
 */
class Rng(var state: Long) {

    fun nextLong(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        return z xor (z ushr 31)
    }

    /** [0, bound) aralığında tam sayı. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound pozitif olmalı: $bound" }
        return ((nextLong() ushr 1) % bound).toInt()
    }

    /** [from, toInclusive] aralığında tam sayı. */
    fun nextInt(from: Int, toInclusive: Int): Int {
        require(toInclusive >= from) { "Geçersiz aralık: $from..$toInclusive" }
        return from + nextInt(toInclusive - from + 1)
    }

    /** [0, 1) aralığında ondalık sayı. */
    fun nextDouble(): Double = (nextLong() ushr 11) * DOUBLE_UNIT

    fun chance(probability: Double): Boolean = nextDouble() < probability

    fun d20(): Int = nextInt(1, 20)

    companion object {
        private const val GOLDEN_GAMMA = -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
        private const val MIX_1 = -0x40a7b892e31b1a47L        // 0xBF58476D1CE4E5B9
        private const val MIX_2 = -0x6b2fb644ecceee15L        // 0x94D049BB133111EB
        private const val DOUBLE_UNIT = 1.0 / (1L shl 53)
    }
}
