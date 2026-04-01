package com.eignex.kpermute

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFailsWith

class HalfLongPermutationTest {

    @Test
    fun bijectionVariousSizes() {
        for (n in listOf(5L, 31L, 64L, 128L)) {
            val p = HalfLongPermutation(n, Random(7))
            CommonAssertsLong.assertBijectionOverDomain(p, n)
        }
    }

    @Test
    fun iteratorFullAndOffset() {
        val n = 50L
        val p = HalfLongPermutation(n, Random(321))
        CommonAssertsLong.assertIteratorMatchesEncode(p, n)
        CommonAssertsLong.assertIteratorMatchesEncode(p, n, offset = 13L)
    }

    @Test
    fun deterministicForSameSeed() {
        val n = 100L
        val factory = { HalfLongPermutation(n, Random(77)) }
        CommonAssertsLong.assertDeterministic(factory)
    }

    @Test
    fun roundsParameterAffectsMapping() {
        val n = 64L
        val seed = 99L
        val p1 = HalfLongPermutation(n, Random(seed), rounds = 2)
        val p2 = HalfLongPermutation(n, Random(seed), rounds = 4)
        CommonAssertsLong.assertDifferentMapping(p1, p2, sample = 17L)
    }

    @Test
    fun rejectsInvalidInputs() {
        assertFailsWith<IllegalArgumentException> {
            HalfLongPermutation(0L, Random(1))
        }
        assertFailsWith<IllegalArgumentException> {
            HalfLongPermutation(10L, Random(1), rounds = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            HalfLongPermutation(10L, Random(1), const = 2L)
        }
    }

    @Test
    fun rejectsOutOfRange() {
        val n = 10L
        val p = HalfLongPermutation(n, Random(2))
        CommonAssertsLong.assertRangeChecks(p, n)
    }
}
