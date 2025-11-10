package com.eigenity.kpermute

import kotlin.random.Random
import kotlin.test.*

class IntPermutationsTest {

    @Test
    fun intFactoryRejectsNegativeRounds() {
        assertFailsWith<IllegalArgumentException> {
            intPermutation(size = 10, rng = Random(1), rounds = -1)
        }
    }

    @Test
    fun intFactorySelectsImplementationsBySizeAndSign() {
        assertTrue(
            intPermutation(
                size = -1, rng = Random(1)
            ) is FullIntPermutation
        )
        assertTrue(
            intPermutation(
                size = -5, rng = Random(1)
            ) is UIntPermutation
        )
        assertTrue(
            intPermutation(
                size = 0, rng = Random(1)
            ) is ArrayIntPermutation
        )
        assertTrue(
            intPermutation(
                size = 16, rng = Random(1)
            ) is ArrayIntPermutation
        )
        assertTrue(
            intPermutation(
                size = 17, rng = Random(1)
            ) is HalfIntPermutation
        )
    }

    @Test
    fun intFactoryDefaultRoundsForHalfAllSizeBands() {
        // <= 2^10 branch
        val small = 600          // > 16 and <= 2^10
        val pSmall = intPermutation(size = small, rng = Random(1), rounds = 0)
        assertEquals(small, pSmall.size)
        assertTrue(pSmall is HalfIntPermutation)

        // (2^10, 2^20] branch
        val medium = (1 shl 10) + 100
        val pMedium = intPermutation(size = medium, rng = Random(2), rounds = 0)
        assertEquals(medium, pMedium.size)
        assertTrue(pMedium is HalfIntPermutation)

        // > 2^20 branch
        val large = (1 shl 20) + 100
        val pLarge = intPermutation(size = large, rng = Random(3), rounds = 0)
        assertEquals(large, pLarge.size)
        assertTrue(pLarge is HalfIntPermutation)
    }

    @Test
    fun intFactoryUsesExplicitRoundsWhenNonZero() {
        val p = intPermutation(size = 128, rng = Random(4), rounds = 5)
        assertTrue(p is HalfIntPermutation)
        val x = 42
        assertEquals(x, p.decode(p.encode(x)))
    }

    @Test
    fun intRangeFactoryNormalCase() {
        val range = 10..19
        val p = intPermutation(range, rng = Random(1), rounds = 0)

        assertEquals(range.count(), p.size)

        for (v in range) {
            val enc = p.encode(v)
            assertTrue(enc in range)
            assertEquals(v, p.decode(enc))
        }
    }

    @Test
    fun intRangeFactoryRejectsEmptyOrDecreasingRange() {
        val range = 10..5
        assertFailsWith<IllegalArgumentException> {
            intPermutation(range, rng = Random(1), rounds = 0)
        }
    }

    @Test
    fun intRangeFactorySeedOverloadUsesSameMapping() {
        val range = 100..129
        val p1 = intPermutation(range, seed = 1234L, rounds = 0)
        val p2 = intPermutation(range, seed = 1234L, rounds = 0)

        assertEquals(p1.toList(), p2.toList())
    }

    @Test
    fun selectsImplementationBySize() {
        assertTrue(intPermutation(8) is ArrayIntPermutation)
        assertTrue(intPermutation(17) is HalfIntPermutation)
        assertTrue(intPermutation(-1) is FullIntPermutation)
        assertTrue(intPermutation(-10) is UIntPermutation)
    }

    @Test
    fun factoryRepeatableSeed() {
        val p1 = intPermutation(32, seed = 1234L)
        val p2 = intPermutation(32, seed = 1234L)
        assertEquals(p1.toList(), p2.toList())
    }

    @Test
    fun smallSizesUseArrayPermutationAndAreBijections() {
        for (n in 0..16) {
            val p = intPermutation(n)
            assertTrue(p is ArrayIntPermutation)
            assertEquals(n, p.toList().toSet().size)
        }
    }

    @Test
    fun halfIntPermutationLargeStillBijection() {
        val p = HalfIntPermutation(512, Random(88))
        CommonAssertsInt.assertBijectionOverDomain(p, 512)
    }

    @Test
    fun respectsRoundsParameterAcrossFactory() {
        val p1 = intPermutation(64, seed = 123, rounds = 1)
        val p2 = intPermutation(64, seed = 123, rounds = 5)
        CommonAssertsInt.assertDifferentMapping(p1, p2, sample = 10)
    }

    @Test
    fun fullSelectedForMaxUIntExplicit() {
        val p = intPermutation(-1)
        assertTrue(p is FullIntPermutation)
    }

    @Test
    fun permutedByRoundTrip() {
        val p = ArrayIntPermutation(5, Random(7))
        val data = listOf("a", "b", "c", "d", "e")
        val shuffled = data.permuted(p)
        val restored = shuffled.unpermuted(p)
        assertEquals(data, restored)
        assertEquals(data.toSet(), shuffled.toSet())
    }

    @Test
    fun rangeWrapperRoundTrip() {
        val base = intPermutation(10, seed = 7)
        val rp = base.range(20..29)
        for (v in 20..29) assertEquals(v, rp.decode(rp.encode(v)))
    }

    @Test
    fun rangeWrapperIterator() {
        val base = intPermutation(5, seed = 1)
        val rp = base.range(5..9)
        val list = rp.toList()
        assertEquals(5, list.size)
        assertTrue(list.all { it in 5..9 })
        assertEquals(list.toSet().size, 5)
    }

}
