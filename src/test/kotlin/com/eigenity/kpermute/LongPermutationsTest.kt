package com.eigenity.kpermute

import kotlin.random.Random
import kotlin.test.*

class LongPermutationsTest {


    @Test
    fun longRangeFactoryNormalCase() {
        val range = 20L..29L
        val p = longPermutation(range, rng = Random(1), rounds = 0)

        assertEquals(range.count().toLong(), p.size)

        for (v in range) {
            val enc = p.encode(v)
            assertTrue(enc in range)
            assertEquals(v, p.decode(enc))
        }
    }

    @Test
    fun longRangeFactoryRejectsNonIncreasingRange() {
        val range = 10L..5L  // last < first, nULong wraps
        assertFailsWith<IllegalArgumentException> {
            longPermutation(range, rng = Random(1), rounds = 0)
        }
    }

    @Test
    fun longRangeFactoryRejectsTooLargeRange() {
        val huge = Long.MIN_VALUE..Long.MAX_VALUE
        assertFailsWith<IllegalArgumentException> {
            longPermutation(huge, rng = Random(1), rounds = 0)
        }
    }

    @Test
    fun longRangeFactorySeedOverloadRepeatable() {
        val range = 100L..129L
        val p1 = longPermutation(range, seed = 5678L, rounds = 0)
        val p2 = longPermutation(range, seed = 5678L, rounds = 0)
        assertEquals(p1.toList(), p2.toList())
    }

    @Test
    fun longFactoryRejectsNegativeRounds() {
        assertFailsWith<IllegalArgumentException> {
            longPermutation(size = 10L, rng = Random(1), rounds = -1)
        }
    }

    @Test
    fun longFactorySelectsImplementationBySizeAndSign() {
        assertTrue(
            longPermutation(
                size = -1L, rng = Random(1)
            ) is FullLongPermutation
        )
        assertTrue(
            longPermutation(
                size = -5L, rng = Random(1)
            ) is ULongPermutation
        )
        assertTrue(
            longPermutation(
                size = 0L, rng = Random(1)
            ) is ArrayLongPermutation
        )
        assertTrue(
            longPermutation(
                size = 16L, rng = Random(1)
            ) is ArrayLongPermutation
        )
        assertTrue(
            longPermutation(
                size = 17L, rng = Random(1)
            ) is HalfLongPermutation
        )
    }

    @Test
    fun longFactoryDefaultRoundsForHalfAllSizeBands() {
        // <= 2^10
        val small = 600L
        val pSmall = longPermutation(size = small, rng = Random(1), rounds = 0)
        assertEquals(small, pSmall.size)
        assertTrue(pSmall is HalfLongPermutation)

        // (2^10, 2^20]
        val medium = (1L shl 10) + 100L
        val pMedium =
            longPermutation(size = medium, rng = Random(2), rounds = 0)
        assertEquals(medium, pMedium.size)
        assertTrue(pMedium is HalfLongPermutation)

        // > 2^20
        val large = (1L shl 20) + 100L
        val pLarge = longPermutation(size = large, rng = Random(3), rounds = 0)
        assertEquals(large, pLarge.size)
        assertTrue(pLarge is HalfLongPermutation)
    }

    @Test
    fun longFactoryDefaultRoundsForULongAllBands() {
        // <= 2^16
        val smallNeg = -10L
        val pSmall =
            longPermutation(size = smallNeg, rng = Random(1), rounds = 0)
        assertTrue(pSmall is ULongPermutation)

        // (2^16, 2^24]
        val midNeg = -((1L shl 16) + 100L)
        val pMid = longPermutation(size = midNeg, rng = Random(2), rounds = 0)
        assertTrue(pMid is ULongPermutation)

        // > 2^24
        val largeNeg = -((1L shl 24) + 100L)
        val pLarge =
            longPermutation(size = largeNeg, rng = Random(3), rounds = 0)
        assertTrue(pLarge is ULongPermutation)

        val sample = 42L
        assertEquals(sample, pSmall.decode(pSmall.encode(sample)))
        assertEquals(sample, pMid.decode(pMid.encode(sample)))
        assertEquals(sample, pLarge.decode(pLarge.encode(sample)))
    }

    @Test
    fun longFactorySeedOverloadRepeatable() {
        val p1 = longPermutation(size = 32L, seed = 1234L, rounds = 0)
        val p2 = longPermutation(size = 32L, seed = 1234L, rounds = 0)
        assertEquals(p1.toList(), p2.toList())
    }

    @Test
    fun selectsImplementationBySizeSentinels() {
        // Full domain sentinel
        assertTrue(longPermutation(-1L) is FullLongPermutation)

        // Negative non-sentinel uses unsigned-domain variant
        assertTrue(longPermutation(-10L) is ULongPermutation)
    }

    @Test
    fun factoryReturnsFiniteSizeForPositive() {
        val size = 32L
        val p = longPermutation(size, seed = 1234L)
        assertEquals(size, p.size)
        // Check bijection on small domain
        CommonAssertsLong.assertBijectionOverDomain(p, size)
    }

    @Test
    fun factoryRepeatableSeed() {
        val p1 = longPermutation(32L, seed = 1234L)
        val p2 = longPermutation(32L, seed = 1234L)
        assertEquals(p1.toList(), p2.toList())
    }

    @Test
    fun halfLongPermutationLargeStillBijection() {
        val p = HalfLongPermutation(512L, Random(88))
        CommonAssertsLong.assertBijectionOverDomain(p, 512L)
    }

    @Test
    fun respectsRoundsParameterAcrossFactory() {
        val p1 = longPermutation(64L, seed = 123, rounds = 1)
        val p2 = longPermutation(64L, seed = 123, rounds = 5)
        CommonAssertsLong.assertDifferentMapping(p1, p2, sample = 10L)
    }

    @Test
    fun fullSelectedForMaxULongExplicit() {
        val p = longPermutation(-1L)
        assertTrue(p is FullLongPermutation)
    }

    @Test
    fun rangeWrapperRoundTrip() {
        val base = HalfLongPermutation(10L, Random(7))
        val rp = base.range(20L..29L)
        for (v in 20L..29L) {
            assertEquals(v, rp.decode(rp.encode(v)))
        }
    }

    @Test
    fun rangeWrapperIterator() {
        val base = HalfLongPermutation(5L, Random(1))
        val rp = base.range(5L..9L)
        val list = rp.toList()
        assertEquals(5, list.size)
        assertTrue(list.all { it in 5L..9L })
        assertEquals(5, list.toSet().size)
    }
}
