package com.eignex.kpermute

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
        val range = 10L..5L // last < first, nULong wraps
        assertFailsWith<IllegalArgumentException> {
            longPermutation(range, rng = Random(1), rounds = 0)
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
                size = -1L,
                rng = Random(1),
            ) is FullLongPermutation,
        )
        assertTrue(
            longPermutation(
                size = -5L,
                rng = Random(1),
            ) is BoundedLongPermutation,
        )
        assertTrue(
            longPermutation(
                size = 0L,
                rng = Random(1),
            ) is SmallLongPermutation,
        )
        assertTrue(
            longPermutation(
                size = 4096L,
                rng = Random(1),
            ) is SmallLongPermutation,
        )
        assertTrue(
            longPermutation(
                size = 4097L,
                rng = Random(1),
            ) is BoundedLongPermutation,
        )
    }

    @Test
    fun longFactoryDefaultRoundsForHalfAllSizeBands() {
        // (4096, 2^20]
        val medium = 5000L
        val pMedium =
            longPermutation(size = medium, rng = Random(2), rounds = 0)
        assertEquals(medium, pMedium.size)
        assertTrue(pMedium is BoundedLongPermutation)

        // > 2^20
        val large = (1L shl 20) + 100L
        val pLarge = longPermutation(size = large, rng = Random(3), rounds = 0)
        assertEquals(large, pLarge.size)
        assertTrue(pLarge is BoundedLongPermutation)
    }

    @Test
    fun longFactoryUsesLargestDefaultRoundsForUnsignedSizes() {
        // Unsigned sizes are always above 2^63, so they get the top band.
        for (size in listOf(-2L, -10L, -((1L shl 16) + 100L), Long.MIN_VALUE)) {
            val p = longPermutation(size = size, rng = Random(1), rounds = 0)
            val expected = longPermutation(size = size, rng = Random(1), rounds = 6)
            for (v in listOf(0L, 42L, Long.MAX_VALUE, size - 1)) {
                assertEquals(expected.encode(v), p.encode(v), "size=$size v=$v")
            }
        }
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
        assertTrue(longPermutation(-10L) is BoundedLongPermutation)
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
    fun boundedLongPermutationLargeStillBijection() {
        val p = BoundedLongPermutation(512L, Random(88))
        CommonAssertsLong.assertBijectionOverDomain(p, 512L)
    }

    @Test
    fun respectsRoundsParameterAcrossFactory() {
        val p1 = longPermutation(5000L, seed = 123, rounds = 1)
        val p2 = longPermutation(5000L, seed = 123, rounds = 5)
        CommonAssertsLong.assertDifferentMapping(p1, p2, sample = 10L)
    }

    @Test
    fun fullSelectedForMaxULongExplicit() {
        val p = longPermutation(-1L)
        assertTrue(p is FullLongPermutation)
    }

    @Test
    fun rangeWrapperRoundTrip() {
        val base = BoundedLongPermutation(10L, Random(7))
        val rp = base.range(20L..29L)
        for (v in 20L..29L) {
            assertEquals(v, rp.decode(rp.encode(v)))
        }
    }

    @Test
    fun rangeWrapperIterator() {
        val base = BoundedLongPermutation(5L, Random(1))
        val rp = base.range(5L..9L)
        val list = rp.toList()
        assertEquals(5, list.size)
        assertTrue(list.all { it in 5L..9L })
        assertEquals(5, list.toSet().size)
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
        assertFailsWith<IllegalArgumentException> { longPermutation(5L, seed = 42).range(-2L..3L) }
        for (size in listOf(-1L, -2L)) {
            assertFailsWith<IllegalArgumentException> { longPermutation(size, seed = 42).range(0L..4L) }
        }
    }

    @Test
    fun rangeViewsCanBeRebasedRepeatedly() {
        for (n in listOf(4L, 17L, 64L)) {
            val base = longPermutation(n, seed = 1L)
            val original = base.range(10L until 10L + n)
            val originalValues = original.toList()
            var rebased = original
            val ranges = listOf(
                20L until 20L + n,
                -n..-1L,
                Long.MIN_VALUE until Long.MIN_VALUE + n,
                (Long.MAX_VALUE - n + 1L)..Long.MAX_VALUE,
                0L until n,
            )
            for (range in ranges) {
                rebased = rebased.range(range)
                val expected = base.map { range.first + it }
                assertEquals(n, rebased.size)
                assertEquals(expected, rebased.toList())
                assertEquals(range.toSet(), expected.toSet())
                for (value in range) {
                    val encoded = rebased.encode(value)
                    assertTrue(encoded in range)
                    assertEquals(value, rebased.decode(encoded))
                    assertEquals(encoded, rebased.encodeUnchecked(value))
                    assertEquals(value, rebased.decodeUnchecked(encoded))
                }
                for (offset in listOf(0L, 1L, n - 1L, n)) {
                    assertEquals(expected.drop(offset.toInt()), rebased.iterator(offset).asSequence().toList())
                }
                val outside = if (range.first == Long.MIN_VALUE) range.last + 1L else range.first - 1L
                assertFailsWith<IllegalArgumentException> { rebased.encode(outside) }
                assertFailsWith<IllegalArgumentException> { rebased.decode(outside) }
            }
            assertEquals(originalValues, original.toList())
        }
    }

    @Test
    fun rejectsEmptyAndOverflowingRangeDomains() {
        val ranges = listOf(
            LongRange(10L, 5L),
            LongRange(5L, 4L),
            Long.MAX_VALUE..Long.MIN_VALUE,
        )
        for (range in ranges) {
            assertFailsWith<IllegalArgumentException> { longPermutation(range, seed = 1L) }
        }
        for (range in ranges + listOf(Long.MIN_VALUE..Long.MAX_VALUE, 0L..Long.MAX_VALUE)) {
            for (size in listOf(0L, 2L)) {
                assertFailsWith<IllegalArgumentException> { longPermutation(size, seed = 1L).range(range) }
            }
        }
    }

    @Test
    fun factoryOutputsAreStableAcrossReleases() {
        val expected = mapOf(
            (17L to 42L) to listOf(7L, 12L, 3L, 2L, 15L, 1L, 10L, 9L),
            (1000L to 7L) to listOf(304L, 614L, 929L, 344L, 161L, 36L, 335L, 151L),
            ((1L shl 40) + 3 to 123L) to listOf(
                848850590364L,
                670083459576L,
                20548540401L,
                142523264832L,
                11130046528L,
                985301304384L,
                150170275084L,
                566319598052L,
            ),
            (Long.MAX_VALUE to 1L) to listOf(
                1699888474470711497L,
                1618433055327283669L,
                8633390165967847255L,
                2340819345493507091L,
                6546443303700477298L,
                8439873622653727069L,
                7333081397773360970L,
                8413434911055314065L,
            ),
        )
        for ((key, values) in expected) {
            val (size, seed) = key
            val p = longPermutation(size, seed)
            assertEquals(values, (0L until 8L).map { p.encode(it) }, "size=$size seed=$seed")
        }
    }
}
