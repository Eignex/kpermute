package com.eignex.kpermute

import kotlin.random.Random
import kotlin.test.*

class FullLongPermutationTest {

    @Test
    fun sizeIsFullLongDomainSentinel() {
        val p = FullLongPermutation()
        assertEquals(-1L, p.size)
    }

    @Test
    fun roundTripForRepresentativeValues() {
        val p = FullLongPermutation(Random(11), rounds = 3)
        val reps = longArrayOf(
            0L,
            1L,
            -1L,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            123_456_789L,
            -987_654_321L,
        )
        for (v in reps) assertEquals(v, p.decode(p.encode(v)))
    }

    @Test
    fun roundsParameterAffectsMapping() {
        val p1 = FullLongPermutation(Random(1), rounds = 1)
        val p2 = FullLongPermutation(Random(1), rounds = 3)
        CommonAssertsLong.assertDifferentMapping(p1, p2)
    }

    @Test
    fun deterministicForSameSeed() {
        val factory = { FullLongPermutation(Random(1234), rounds = 2) }
        val p1 = factory()
        val p2 = factory()
        val samples = listOf(
            0L,
            1L,
            2L,
            100L,
            -1L,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
        )
        for (x in samples) {
            assertEquals(p1.encode(x), p2.encode(x))
        }
    }

    @Test
    fun iteratorOverflowTerminates() {
        val p = FullLongPermutation(Random(2))
        for (start in listOf(-6L, -1L)) {
            val iterator = p.iterator(start)
            val expected = (start..-1L).map { p.encode(it) }
            assertEquals(expected, iterator.asSequence().toList())
            assertFalse(iterator.hasNext())
            assertFailsWith<NoSuchElementException> { iterator.nextLong() }
        }
    }

    @Test
    fun iteratorExhaustionThrowsAfterOverflow() {
        val p = FullLongPermutation(Random(1))
        val start = ULong.MAX_VALUE.toLong() - 3L // -4L
        val itr = p.iterator(start)
        repeat(4) { itr.nextLong() }
        assertFailsWith<NoSuchElementException> { itr.nextLong() }
    }

    @Test
    fun rejectsInvalidConstructorArguments() {
        assertFailsWith<IllegalArgumentException> { FullLongPermutation(Random(1), rounds = 0) }
        assertFailsWith<IllegalArgumentException> { FullLongPermutation(Random(1), rounds = -1) }
        assertFailsWith<IllegalArgumentException> { FullLongPermutation(Random(1), c1 = 2L) }
        assertFailsWith<IllegalArgumentException> { FullLongPermutation(Random(1), c2 = 0L) }
    }
}
