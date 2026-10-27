package com.eignex.kpermute

import kotlin.random.Random
import kotlin.test.*

class FullIntPermutationTest {

    @Test
    fun sizeIsFullIntDomainSentinel() {
        val p = FullIntPermutation()
        assertEquals(-1, p.size)
    }

    @Test
    fun roundTripForRepresentativeValues() {
        val p = FullIntPermutation(Random(11), rounds = 3)
        val reps = intArrayOf(
            0,
            1,
            -1,
            Int.MAX_VALUE,
            Int.MIN_VALUE,
            123456789,
            -987654321,
        )
        for (v in reps) assertEquals(v, p.decode(p.encode(v)))
    }

    @Test
    fun roundsParameterAffectsMapping() {
        val p1 = FullIntPermutation(Random(1), rounds = 1)
        val p2 = FullIntPermutation(Random(1), rounds = 3)
        CommonAssertsInt.assertDifferentMapping(p1, p2)
    }

    @Test
    fun deterministicForSameSeed() {
        val factory = { FullIntPermutation(Random(1234), rounds = 2) }
        // Only sample a small set because domain is all Int
        val p1 = factory()
        val p2 = factory()
        val samples = listOf(0, 1, 2, 100, -1, Int.MAX_VALUE, Int.MIN_VALUE)
        for (x in samples) assertEquals(p1.encode(x), p2.encode(x))
    }

    @Test
    fun iteratorOverflowTerminates() {
        val p = FullIntPermutation(Random(2))
        for (start in listOf(-6, -1)) {
            val iterator = p.iterator(start)
            val expected = (start..-1).map { p.encode(it) }
            assertEquals(expected, iterator.asSequence().toList())
            assertFalse(iterator.hasNext())
            assertFailsWith<NoSuchElementException> { iterator.nextInt() }
        }
    }

    @Test
    fun iteratorExhaustionThrowsAfterOverflow() {
        val p = FullIntPermutation(Random(1))
        val start = UInt.MAX_VALUE.toInt() - 3
        val itr = p.iterator(start)
        repeat(4) { itr.nextInt() }
        assertFailsWith<NoSuchElementException> { itr.nextInt() }
    }
}
