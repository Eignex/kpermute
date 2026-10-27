package com.eignex.kpermute

import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalUnsignedTypes::class)
class BoundedLongPermutationTest {

    @Test
    fun bijectionVariousSizes() {
        for (n in listOf(1L, 2L, 3L, 4L, 7L, 15L, 64L, 1000L)) {
            val p = BoundedLongPermutation(n, Random(55))
            CommonAssertsLong.assertBijectionOverDomain(p, n)
        }
    }

    @Test
    fun iteratorFullAndOffsetsEdgeCases() {
        val n = 5L
        val p = BoundedLongPermutation(n, Random(1))

        CommonAssertsLong.assertIteratorMatchesEncode(p, n)

        val itLast = p.iterator(n - 1L)
        assertTrue(itLast.hasNext())
        assertEquals(p.encode(n - 1L), itLast.nextLong())
        assertFalse(itLast.hasNext())
        assertFailsWith<NoSuchElementException> { itLast.nextLong() }

        val itEmpty = p.iterator(n)
        assertFalse(itEmpty.hasNext())
        assertFailsWith<NoSuchElementException> { itEmpty.nextLong() }
    }

    @Test
    fun deterministicForSameSeed() {
        val n = 20L
        val factory = { BoundedLongPermutation(n, Random(9)) }
        CommonAssertsLong.assertDeterministic(factory)
    }

    @Test
    fun roundsAndConstConstraintsAndVariation() {
        assertFailsWith<IllegalArgumentException> {
            BoundedLongPermutation(32L, Random(1), const = 4uL)
        }
        val p1 = BoundedLongPermutation(128L, Random(5), rounds = 1)
        val p2 = BoundedLongPermutation(128L, Random(5), rounds = 5)
        CommonAssertsLong.assertDifferentMapping(p1, p2)
    }

    @Test
    fun rejectsOutOfRange() {
        val n = 10L
        val p = BoundedLongPermutation(n, Random(1))
        CommonAssertsLong.assertRangeChecks(p, n)
    }

    @Test
    fun unsignedLongDomainsPreserveValuesOnBothSidesOfTheSignBit() {
        for (size in listOf(Long.MIN_VALUE, Long.MIN_VALUE + 1, -2L)) {
            for (seed in listOf(0L, 42L, Long.MIN_VALUE)) {
                val perm = BoundedLongPermutation(size, Random(seed))
                val samples = listOf(0L, 1L, Long.MAX_VALUE - 1, Long.MAX_VALUE, Long.MIN_VALUE, size - 1)
                    .filter { it.toULong() < size.toULong() }
                for (value in samples) {
                    val context = "size=$size seed=$seed value=$value"
                    assertTrue(perm.encode(value).toULong() < size.toULong(), context)
                    assertTrue(perm.decode(value).toULong() < size.toULong(), context)
                    assertEquals(value, perm.decode(perm.encode(value)), context)
                    assertEquals(value, perm.encode(perm.decode(value)), context)
                }
                val iterator = perm.iterator(size - 2)
                for (value in listOf(size - 2, size - 1)) {
                    assertTrue(iterator.hasNext())
                    assertEquals(perm.encode(value), iterator.nextLong())
                }
                assertFalse(iterator.hasNext())
                assertFailsWith<NoSuchElementException> { iterator.nextLong() }
                val end = perm.iterator(size)
                assertFalse(end.hasNext())
                assertFailsWith<NoSuchElementException> { end.nextLong() }
            }
        }
    }

    @Test
    fun rejectsOutOfRangeForUnsignedSizes() {
        for (size in listOf(Long.MIN_VALUE, Long.MIN_VALUE + 1L, -10L, -2L, -1L)) {
            val permutations = mutableListOf<LongPermutation>(BoundedLongPermutation(size, Random(1)))
            if (size != -1L) permutations += longPermutation(size, seed = 1L)
            for (p in permutations) {
                for (invalid in listOf(size, -1L).distinct()) {
                    assertFailsWith<IllegalArgumentException> { p.encode(invalid) }
                    assertFailsWith<IllegalArgumentException> { p.decode(invalid) }
                }
                val valid = mutableListOf(0L, 1L, Long.MAX_VALUE, (size.toULong() - 1uL).toLong())
                if (Long.MIN_VALUE.toULong() < size.toULong()) valid += Long.MIN_VALUE
                for (value in valid.distinct()) {
                    val encoded = p.encode(value)
                    assertTrue(encoded.toULong() < size.toULong())
                    assertEquals(value, p.decode(encoded))
                    assertEquals(encoded, p.encodeUnchecked(value))
                    assertEquals(value, p.decodeUnchecked(encoded))
                }
            }
        }
    }

    @Test
    fun rejectsZeroSize() {
        assertFailsWith<IllegalArgumentException> { BoundedLongPermutation(0L, Random(1)) }
    }

    @Test
    fun rejectsNegativeRoundsBeforeAllocating() {
        assertFailsWith<IllegalArgumentException> { BoundedLongPermutation(100L, Random(1), rounds = -1) }
    }

    @Test
    fun rejectsOffsetsOutsideTheDomain() {
        val p = BoundedLongPermutation(17L, Random(1))
        for (offset in listOf(-1L, 18L)) {
            assertFailsWith<IllegalArgumentException>("offset=$offset") { p.iterator(offset) }
        }
        assertFalse(p.iterator(17L).hasNext())
        val unsigned = BoundedLongPermutation(-2L, Random(1))
        assertTrue(unsigned.iterator(-3L).hasNext())
        assertFailsWith<IllegalArgumentException> { unsigned.iterator(-1L) }
    }
}
