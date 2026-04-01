package com.eignex.kpermute

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFailsWith

class HalfIntPermutationTest {
    @Test
    fun bijectionVariousSizes() {
        for (n in listOf(5, 31, 64, 128)) {
            val p = HalfIntPermutation(n, Random(7))
            CommonAssertsInt.assertBijectionOverDomain(p, n)
        }
    }

    @Test
    fun iteratorFullAndOffset() {
        val n = 50
        val p = HalfIntPermutation(n, Random(321))
        CommonAssertsInt.assertIteratorMatchesEncode(p, n)
        CommonAssertsInt.assertIteratorMatchesEncode(p, n, offset = 13)
    }

    @Test
    fun deterministicForSameSeed() {
        val n = 100
        val factory = { HalfIntPermutation(n, Random(77)) }
        CommonAssertsInt.assertDeterministic(factory)
    }

    @Test
    fun roundsParameterAffectsMapping() {
        val n = 64
        val seed = 99L
        val p1 = HalfIntPermutation(n, Random(seed), rounds = 2)
        val p2 = HalfIntPermutation(n, Random(seed), rounds = 4)
        CommonAssertsInt.assertDifferentMapping(p1, p2, sample = 17)
    }

    @Test
    fun rejectsInvalidInputs() {
        assertFailsWith<IllegalArgumentException> {
            HalfIntPermutation(
                0,
                Random(1)
            )
        }
        assertFailsWith<IllegalArgumentException> {
            HalfIntPermutation(
                10,
                Random(1),
                rounds = 0
            )
        }
        assertFailsWith<IllegalArgumentException> {
            HalfIntPermutation(
                10,
                Random(1),
                const = 2
            )
        }
    }

    @Test
    fun rejectsOutOfRange() {
        val n = 10
        val p = HalfIntPermutation(n, Random(2))
        CommonAssertsInt.assertRangeChecks(p, n)
    }
}
