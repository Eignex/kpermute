package com.eignex.kpermute

import kotlin.test.*

class PermutationsTest {
    private val smallSizes = (0..17).toList() + listOf(31, 32, 33, 63, 64, 65, 255, 256, 257, 1023, 1024, 1025)
    private val seeds = listOf(0L, 42L, Long.MIN_VALUE)

    @Test
    fun intFactoryIsBijectiveAcrossArrayAndCycleWalkingBoundaries() {
        for (size in smallSizes) {
            for (seed in seeds) {
                for (rounds in listOf(1, 3, 6)) {
                    val perm = intPermutation(size, seed = seed, rounds = rounds)
                    val context = "size=$size seed=$seed rounds=$rounds"
                    val encoded = (0 until size).map { perm.encode(it) }
                    assertEquals((0 until size).toList(), encoded.sorted(), context)
                    for (value in 0 until size) {
                        assertEquals(value, perm.decode(encoded[value]), context)
                        assertEquals(value, perm.encode(perm.decode(value)), context)
                    }
                    assertEquals(encoded, perm.toList(), context)
                    val iterator = perm.iterator(size / 2)
                    assertEquals(encoded.drop(size / 2), iterator.asSequence().toList(), context)
                    assertFailsWith<NoSuchElementException>(context) { iterator.nextInt() }
                }
            }
        }
    }

    @Test
    fun longFactoryIsBijectiveAcrossArrayAndCycleWalkingBoundaries() {
        for (size in smallSizes) {
            for (seed in seeds) {
                for (rounds in listOf(1, 3, 6)) {
                    val perm = longPermutation(size.toLong(), seed = seed, rounds = rounds)
                    val context = "size=$size seed=$seed rounds=$rounds"
                    val domain = (0 until size).map { it.toLong() }
                    val encoded = domain.map { perm.encode(it) }
                    assertEquals(domain, encoded.sorted(), context)
                    for (value in domain) {
                        assertEquals(value, perm.decode(encoded[value.toInt()]), context)
                        assertEquals(value, perm.encode(perm.decode(value)), context)
                    }
                    assertEquals(encoded, perm.toList(), context)
                    val iterator = perm.iterator(size.toLong() / 2)
                    assertEquals(encoded.drop(size / 2), iterator.asSequence().toList(), context)
                    assertFailsWith<NoSuchElementException>(context) { iterator.nextLong() }
                }
            }
        }
    }

    @Test
    fun emptyDomainsRejectValuesAndHaveExhaustedIterators() {
        val ints = intPermutation(0, seed = 42)
        val longs = longPermutation(0L, seed = 42)
        for (value in listOf(-1, 0, 1)) {
            assertFailsWith<IllegalArgumentException> { ints.encode(value) }
            assertFailsWith<IllegalArgumentException> { ints.decode(value) }
            assertFailsWith<IllegalArgumentException> { longs.encode(value.toLong()) }
            assertFailsWith<IllegalArgumentException> { longs.decode(value.toLong()) }
        }
        val intIterator = ints.iterator()
        val longIterator = longs.iterator()
        repeat(2) {
            assertFalse(intIterator.hasNext())
            assertFalse(longIterator.hasNext())
            assertFailsWith<NoSuchElementException> { intIterator.nextInt() }
            assertFailsWith<NoSuchElementException> { longIterator.nextLong() }
        }
    }

    @Test
    fun factoriesAcceptRangesBeyondTheSignedSize() {
        val intRanges = listOf(
            Int.MIN_VALUE..Int.MAX_VALUE,
            Int.MIN_VALUE + 1..Int.MAX_VALUE,
            Int.MIN_VALUE..-1,
            0..Int.MAX_VALUE,
        )
        for (range in intRanges) {
            val perm = intPermutation(range, seed = 42)
            for (value in listOf(range.first, range.first + 1, range.last - 1, range.last)) {
                assertTrue(perm.encode(value) in range, "range=$range value=$value")
                assertTrue(perm.decode(value) in range, "range=$range value=$value")
                assertEquals(value, perm.decode(perm.encode(value)))
            }
            val lastIndex = range.last - range.first
            assertEquals(
                listOf(perm.encode(range.last - 1), perm.encode(range.last)),
                perm.iterator(lastIndex - 1).asSequence().toList(),
            )
        }
        assertTrue(intPermutation(Int.MIN_VALUE..Int.MAX_VALUE, seed = 42).size == -1)

        val longRanges = listOf(
            Long.MIN_VALUE..Long.MAX_VALUE,
            Long.MIN_VALUE + 1..Long.MAX_VALUE,
            Long.MIN_VALUE..-1L,
            0L..Long.MAX_VALUE,
        )
        for (range in longRanges) {
            val perm = longPermutation(range, seed = 42)
            for (value in listOf(range.first, range.first + 1, range.last - 1, range.last)) {
                assertTrue(perm.encode(value) in range, "range=$range value=$value")
                assertTrue(perm.decode(value) in range, "range=$range value=$value")
                assertEquals(value, perm.decode(perm.encode(value)))
            }
            val lastIndex = range.last - range.first
            assertEquals(
                listOf(perm.encode(range.last - 1), perm.encode(range.last)),
                perm.iterator(lastIndex - 1).asSequence().toList(),
            )
        }
        assertTrue(longPermutation(Long.MIN_VALUE..Long.MAX_VALUE, seed = 42).size == -1L)
    }

    @Test
    fun unsignedAndFullPermutationsCanBeRebased() {
        val full = intPermutation(-1, seed = 7)
        val shifted = full.range(Int.MIN_VALUE..Int.MAX_VALUE)
        assertEquals(Int.MIN_VALUE + full.encode(5 - Int.MIN_VALUE), shifted.encode(5))
        val bounded = intPermutation(Int.MIN_VALUE, seed = 7).range(Int.MIN_VALUE..-1)
        assertTrue(bounded.encode(-1) < 0)
        assertFailsWith<IllegalArgumentException> { intPermutation(-1, seed = 7).range(0..Int.MAX_VALUE) }
        assertFailsWith<IllegalArgumentException> { BoundedIntPermutation(-1).range(Int.MIN_VALUE..Int.MAX_VALUE) }

        val fullLong = longPermutation(-1L, seed = 7)
        assertEquals(
            Long.MIN_VALUE + fullLong.encode(5L - Long.MIN_VALUE),
            fullLong.range(Long.MIN_VALUE..Long.MAX_VALUE).encode(5L),
        )
        assertFailsWith<IllegalArgumentException> { BoundedLongPermutation(-1L).range(Long.MIN_VALUE..Long.MAX_VALUE) }
    }

    @Test
    fun largestSupportedIntRangeIncludesBothEndpoints() {
        val range = Int.MIN_VALUE..-2
        val perm = intPermutation(range, seed = 42)
        assertEquals(Int.MAX_VALUE, perm.size)
        for (value in listOf(range.first, range.first + 1, -3, range.last)) {
            assertTrue(perm.encode(value) in range)
            assertTrue(perm.decode(value) in range)
            assertEquals(value, perm.decode(perm.encode(value)))
            assertEquals(value, perm.encode(perm.decode(value)))
        }
        assertEquals(listOf(perm.encode(-3), perm.encode(-2)), perm.iterator(perm.size - 2).asSequence().toList())
    }

    @Test
    fun largestSupportedLongRangeIncludesBothEndpoints() {
        val range = Long.MIN_VALUE..-2L
        val perm = longPermutation(range, seed = 42)
        assertEquals(Long.MAX_VALUE, perm.size)
        for (value in listOf(range.first, range.first + 1, -3L, range.last)) {
            assertTrue(perm.encode(value) in range)
            assertTrue(perm.decode(value) in range)
            assertEquals(value, perm.decode(perm.encode(value)))
            assertEquals(value, perm.encode(perm.decode(value)))
        }
        assertEquals(listOf(perm.encode(-3L), perm.encode(-2L)), perm.iterator(perm.size - 2).asSequence().toList())
    }
}
