package com.eignex.kpermute

import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalUnsignedTypes::class)
class ULongPermutationTest {

    @Test
    fun bijectionVariousSizes() {
        for (n in listOf(7L, 15L, 64L, 1000L)) {
            val p = ULongPermutation(n, Random(55))
            CommonAssertsLong.assertBijectionOverDomain(p, n)
        }
    }

    @Test
    fun iteratorFullAndOffsetsEdgeCases() {
        val n = 5L
        val p = ULongPermutation(n, Random(1))

        // full
        CommonAssertsLong.assertIteratorMatchesEncode(p, n)

        // offset == last index → one element then end
        val itLast = p.iterator(n - 1L)
        assertTrue(itLast.hasNext())
        assertEquals(p.encode(n - 1L), itLast.nextLong())
        assertFalse(itLast.hasNext())
        assertFailsWith<NoSuchElementException> { itLast.nextLong() }

        // offset == size → empty and throws on next
        val itEmpty = p.iterator(n)
        assertFalse(itEmpty.hasNext())
        assertFailsWith<NoSuchElementException> { itEmpty.nextLong() }
    }

    @Test
    fun deterministicForSameSeed() {
        val n = 20L
        val factory = { ULongPermutation(n, Random(9)) }
        CommonAssertsLong.assertDeterministic(factory)
    }

    @Test
    fun roundsAndConstConstraintsAndVariation() {
        assertFailsWith<IllegalArgumentException> {
            ULongPermutation(32L, Random(1), const = 4uL)
        }
        val p1 = ULongPermutation(128L, Random(5), rounds = 1)
        val p2 = ULongPermutation(128L, Random(5), rounds = 5)
        CommonAssertsLong.assertDifferentMapping(p1, p2)
    }

    @Test
    fun rejectsOutOfRange() {
        val n = 10L
        val p = ULongPermutation(n, Random(1))
        CommonAssertsLong.assertRangeChecks(p, n)
    }
}
