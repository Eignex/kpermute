package com.eignex.kpermute

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class SmallIntPermutationTest {

    @Test
    fun bijectionSmallDomains() {
        for (n in 1..16) {
            val p = SmallIntPermutation(n, Random(123))
            CommonAssertsInt.assertBijectionOverDomain(p, n)
        }
    }

    @Test
    fun iteratorCoversAllInOrderOfEncode() {
        val n = 10
        val p = SmallIntPermutation(n, Random(1))
        CommonAssertsInt.assertIteratorMatchesEncode(p, n)
    }

    @Test
    fun iteratorWithOffset() {
        val n = 12
        val off = 5
        val p = SmallIntPermutation(n, Random(2))
        CommonAssertsInt.assertIteratorMatchesEncode(p, n, off)
    }

    @Test
    fun deterministicForSameSeed() {
        val factory = { SmallIntPermutation(16, Random(42)) }
        CommonAssertsInt.assertDeterministic(factory)
    }

    @Test
    fun decodeIsInverseOfEncodeSample() {
        val p = SmallIntPermutation(32, Random(99))
        for (x in 0 until 32) assertEquals(x, p.decode(p.encode(x)))
    }

    @Test
    fun rejectsOutOfRange() {
        val n = 7
        val p = SmallIntPermutation(n, Random(3))
        CommonAssertsInt.assertRangeChecks(p, n)
    }
}
