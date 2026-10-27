package com.eignex.kpermute

import kotlin.test.*

class RangePermutationTest {
    @Test
    fun intRangesTranslateBothDirectionsAtSignedBoundaries() {
        val ranges = listOf(
            Int.MIN_VALUE..Int.MIN_VALUE + 16,
            -8..8,
            Int.MAX_VALUE - 16..Int.MAX_VALUE,
            Int.MIN_VALUE..Int.MIN_VALUE,
            Int.MAX_VALUE..Int.MAX_VALUE,
        )
        for (range in ranges) {
            val base = intPermutation(range.count(), seed = 42)
            val wrapped = base.range(range)
            val factory = intPermutation(range, seed = 42)
            for ((index, value) in range.withIndex()) {
                val encoded = range.first + base.encode(index)
                val decoded = range.first + base.decode(index)
                assertEquals(encoded, wrapped.encode(value), "range=$range value=$value")
                assertEquals(decoded, wrapped.decode(value), "range=$range value=$value")
                assertEquals(encoded, factory.encode(value))
                assertEquals(decoded, factory.decode(value))
                assertEquals(encoded, wrapped.encodeUnchecked(value))
                assertEquals(decoded, wrapped.decodeUnchecked(value))
            }
            for (outside in listOf(-9, 9, Int.MIN_VALUE, Int.MAX_VALUE).filter { it !in range }) {
                assertFailsWith<IllegalArgumentException> { wrapped.encode(outside) }
                assertFailsWith<IllegalArgumentException> { wrapped.decode(outside) }
            }
        }
    }

    @Test
    fun longRangesTranslateBothDirectionsAtSignedBoundaries() {
        val ranges = listOf(
            Long.MIN_VALUE..Long.MIN_VALUE + 16,
            -8L..8L,
            Long.MAX_VALUE - 16..Long.MAX_VALUE,
            Long.MIN_VALUE..Long.MIN_VALUE,
            Long.MAX_VALUE..Long.MAX_VALUE,
        )
        for (range in ranges) {
            val base = longPermutation(range.count().toLong(), seed = 42)
            val wrapped = base.range(range)
            val factory = longPermutation(range, seed = 42)
            for ((index, value) in range.withIndex()) {
                val encoded = range.first + base.encode(index.toLong())
                val decoded = range.first + base.decode(index.toLong())
                assertEquals(encoded, wrapped.encode(value), "range=$range value=$value")
                assertEquals(decoded, wrapped.decode(value), "range=$range value=$value")
                assertEquals(encoded, factory.encode(value))
                assertEquals(decoded, factory.decode(value))
                assertEquals(encoded, wrapped.encodeUnchecked(value))
                assertEquals(decoded, wrapped.decodeUnchecked(value))
            }
            for (outside in listOf(-9L, 9L, Long.MIN_VALUE, Long.MAX_VALUE).filter { it !in range }) {
                assertFailsWith<IllegalArgumentException> { wrapped.encode(outside) }
                assertFailsWith<IllegalArgumentException> { wrapped.decode(outside) }
            }
        }
    }

    @Test
    fun intRangeIteratorUsesAnIndexOffsetAndStaysExhausted() {
        val range = -20..-4
        val perm = intPermutation(range, seed = 42)
        for (offset in listOf(0, 3, 16, 17)) {
            val iterator = perm.iterator(offset)
            for (value in range.drop(offset)) {
                assertTrue(iterator.hasNext())
                assertTrue(iterator.hasNext(), "hasNext must not consume an element")
                assertEquals(perm.encode(value), iterator.nextInt())
            }
            repeat(2) {
                assertFalse(iterator.hasNext())
                assertFailsWith<NoSuchElementException> { iterator.nextInt() }
            }
        }
        assertFailsWith<IllegalArgumentException> { perm.iterator(-1) }
        assertFailsWith<IllegalArgumentException> { perm.iterator(18) }
    }

    @Test
    fun longRangeIteratorUsesAnIndexOffsetAndStaysExhausted() {
        val range = Long.MAX_VALUE - 16..Long.MAX_VALUE
        val perm = longPermutation(range, seed = 42)
        for (offset in listOf(0, 3, 16, 17)) {
            val iterator = perm.iterator(offset.toLong())
            for (value in range.drop(offset)) {
                assertTrue(iterator.hasNext())
                assertTrue(iterator.hasNext(), "hasNext must not consume an element")
                assertEquals(perm.encode(value), iterator.nextLong())
            }
            repeat(2) {
                assertFalse(iterator.hasNext())
                assertFailsWith<NoSuchElementException> { iterator.nextLong() }
            }
        }
        assertFailsWith<IllegalArgumentException> { perm.iterator(-1L) }
        assertFailsWith<IllegalArgumentException> { perm.iterator(18L) }
    }

    @Test
    fun wrappersRejectMismatchedLengthsAndNonFiniteBases() {
        assertFailsWith<IllegalArgumentException> { intPermutation(5, seed = 42).range(-2..3) }
        assertFailsWith<IllegalArgumentException> { longPermutation(5L, seed = 42).range(-2L..3L) }
        for (size in listOf(-1, -2)) {
            assertFailsWith<IllegalArgumentException> { intPermutation(size, seed = 42).range(0..4) }
            assertFailsWith<IllegalArgumentException> { longPermutation(size.toLong(), seed = 42).range(0L..4L) }
        }
    }

    @Test
    fun factoriesRejectRangesWhoseLengthsOverflowTheSignedSize() {
        for (range in listOf(Int.MIN_VALUE..Int.MAX_VALUE, Int.MIN_VALUE..-1, 0..Int.MAX_VALUE)) {
            assertFailsWith<IllegalArgumentException> { intPermutation(range, seed = 42) }
        }
        for (range in listOf(Long.MIN_VALUE..Long.MAX_VALUE, Long.MIN_VALUE..-1L, 0L..Long.MAX_VALUE)) {
            assertFailsWith<IllegalArgumentException> { longPermutation(range, seed = 42) }
        }
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
