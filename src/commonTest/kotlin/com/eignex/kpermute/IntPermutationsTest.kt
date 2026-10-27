package com.eignex.kpermute

import kotlin.random.Random
import kotlin.test.*

class IntPermutationsTest {

    @Test
    fun intFactoryRejectsNegativeRounds() {
        assertFailsWith<IllegalArgumentException> {
            intPermutation(size = 10, rng = Random(1), rounds = -1)
        }
    }

    @Test
    fun intFactorySelectsImplementationsBySizeAndSign() {
        assertTrue(
            intPermutation(
                size = -1,
                rng = Random(1),
            ) is FullIntPermutation,
        )
        assertTrue(
            intPermutation(
                size = -5,
                rng = Random(1),
            ) is UIntPermutation,
        )
        assertTrue(
            intPermutation(
                size = 0,
                rng = Random(1),
            ) is ArrayIntPermutation,
        )
        assertTrue(
            intPermutation(
                size = 16,
                rng = Random(1),
            ) is ArrayIntPermutation,
        )
        assertTrue(
            intPermutation(
                size = 17,
                rng = Random(1),
            ) is HalfIntPermutation,
        )
    }

    @Test
    fun intFactoryDefaultRoundsForHalfAllSizeBands() {
        // <= 2^10 branch
        val small = 600 // > 16 and <= 2^10
        val pSmall = intPermutation(size = small, rng = Random(1), rounds = 0)
        assertEquals(small, pSmall.size)
        assertTrue(pSmall is HalfIntPermutation)

        // (2^10, 2^20] branch
        val medium = (1 shl 10) + 100
        val pMedium = intPermutation(size = medium, rng = Random(2), rounds = 0)
        assertEquals(medium, pMedium.size)
        assertTrue(pMedium is HalfIntPermutation)

        // > 2^20 branch
        val large = (1 shl 20) + 100
        val pLarge = intPermutation(size = large, rng = Random(3), rounds = 0)
        assertEquals(large, pLarge.size)
        assertTrue(pLarge is HalfIntPermutation)
    }

    @Test
    fun intFactoryUsesExplicitRoundsWhenNonZero() {
        val p = intPermutation(size = 128, rng = Random(4), rounds = 5)
        assertTrue(p is HalfIntPermutation)
        val x = 42
        assertEquals(x, p.decode(p.encode(x)))
    }

    @Test
    fun intRangeFactoryNormalCase() {
        val range = 10..19
        val p = intPermutation(range, rng = Random(1), rounds = 0)

        assertEquals(range.count(), p.size)

        for (v in range) {
            val enc = p.encode(v)
            assertTrue(enc in range)
            assertEquals(v, p.decode(enc))
        }
    }

    @Test
    fun intRangeFactoryRejectsEmptyOrDecreasingRange() {
        assertFailsWith<IllegalArgumentException> {
            intPermutation(IntRange.EMPTY, rng = Random(1), rounds = 0)
        }
    }

    @Test
    fun intRangeFactorySeedOverloadUsesSameMapping() {
        val range = 100..129
        val p1 = intPermutation(range, seed = 1234L, rounds = 0)
        val p2 = intPermutation(range, seed = 1234L, rounds = 0)

        assertEquals(p1.toList(), p2.toList())
    }

    @Test
    fun selectsImplementationBySize() {
        assertTrue(intPermutation(8) is ArrayIntPermutation)
        assertTrue(intPermutation(17) is HalfIntPermutation)
        assertTrue(intPermutation(-1) is FullIntPermutation)
        assertTrue(intPermutation(-10) is UIntPermutation)
    }

    @Test
    fun factoryRepeatableSeed() {
        val p1 = intPermutation(32, seed = 1234L)
        val p2 = intPermutation(32, seed = 1234L)
        assertEquals(p1.toList(), p2.toList())
    }

    @Test
    fun smallSizesUseArrayPermutationAndAreBijections() {
        for (n in 0..16) {
            val p = intPermutation(n)
            assertTrue(p is ArrayIntPermutation)
            assertEquals(n, p.toList().toSet().size)
        }
    }

    @Test
    fun halfIntPermutationLargeStillBijection() {
        val p = HalfIntPermutation(512, Random(88))
        CommonAssertsInt.assertBijectionOverDomain(p, 512)
    }

    @Test
    fun respectsRoundsParameterAcrossFactory() {
        val p1 = intPermutation(64, seed = 123, rounds = 1)
        val p2 = intPermutation(64, seed = 123, rounds = 5)
        CommonAssertsInt.assertDifferentMapping(p1, p2, sample = 10)
    }

    @Test
    fun fullSelectedForMaxUIntExplicit() {
        val p = intPermutation(-1)
        assertTrue(p is FullIntPermutation)
    }

    @Test
    fun permutedByRoundTrip() {
        val p = ArrayIntPermutation(5, Random(7))
        val data = listOf("a", "b", "c", "d", "e")
        val shuffled = data.permuted(p)
        val restored = shuffled.unpermuted(p)
        assertEquals(data, restored)
        assertEquals(data.toSet(), shuffled.toSet())
    }

    @Test
    fun rangeWrapperRoundTrip() {
        val base = intPermutation(10, seed = 7)
        val rp = base.range(20..29)
        for (v in 20..29) assertEquals(v, rp.decode(rp.encode(v)))
    }

    @Test
    fun rangeWrapperIterator() {
        val base = intPermutation(5, seed = 1)
        val rp = base.range(5..9)
        val list = rp.toList()
        assertEquals(5, list.size)
        assertTrue(list.all { it in 5..9 })
        assertEquals(list.toSet().size, 5)
    }

    // A three-cycle has different forward and inverse mappings, so swapping
    // encode/decode cannot pass just because both views still round-trip.
    private val cycle = object : IntPermutation {
        override val size = 3
        override fun encodeUnchecked(value: Int) = intArrayOf(1, 2, 0)[value]
        override fun decodeUnchecked(encoded: Int) = intArrayOf(2, 0, 1)[encoded]
        override fun iterator(offset: Int): IntIterator = (offset until size).map { encode(it) }.toIntArray().iterator()
    }

    @Test
    fun permutedMovesEachElementToItsEncodedIndex() {
        val source = listOf("a", "b", "c")
        assertEquals(listOf("c", "a", "b"), source.permuted(cycle))
        assertEquals(listOf("a", "b", "c"), source)
    }

    @Test
    fun unpermutedUsesTheOppositeDirection() {
        assertEquals(listOf("b", "c", "a"), listOf("a", "b", "c").unpermuted(cycle))
        assertEquals(listOf("a", "b", "c"), listOf("c", "a", "b").unpermuted(cycle))
    }

    @Test
    fun viewsReadOnlyTheRequestedElementOfAHugeList() {
        val reads = mutableListOf<Int>()
        val source = object : AbstractList<Int>() {
            override val size = Int.MAX_VALUE
            override fun get(index: Int): Int {
                reads += index
                return index
            }
        }
        val perm = intPermutation(source.size, seed = 42)
        val shuffled = source.permuted(perm)
        val unshuffled = source.unpermuted(perm)
        assertEquals(source.size, shuffled.size)
        assertEquals(source.size, unshuffled.size)
        assertTrue(reads.isEmpty(), "Creating a view and reading its size must not read source elements")

        val index = Int.MAX_VALUE - 2
        assertEquals(perm.decode(index), shuffled[index])
        assertEquals(listOf(perm.decode(index)), reads)
        reads.clear()
        assertEquals(perm.encode(index), unshuffled[index])
        assertEquals(listOf(perm.encode(index)), reads)
    }

    @Test
    fun viewsReflectElementUpdatesWithoutMutatingTheSource() {
        val source = mutableListOf("a", "b", "c")
        val shuffled = source.permuted(cycle)
        val unshuffled = source.unpermuted(cycle)
        source[0] = "updated"

        assertEquals(listOf("c", "updated", "b"), shuffled)
        assertEquals(listOf("b", "c", "updated"), unshuffled)
        assertEquals(listOf("updated", "b", "c"), source)
    }

    @Test
    fun bothViewsRejectSmallerAndLargerPermutationDomains() {
        val source = listOf("a", "b", "c")
        for (size in listOf(0, 2, 4, -1)) {
            val perm = intPermutation(size, seed = 42)
            assertFailsWith<IllegalArgumentException> { source.permuted(perm) }
            assertFailsWith<IllegalArgumentException> { source.unpermuted(perm) }
        }
    }

    @Test
    fun emptyAndSingletonListsArePreserved() {
        for (source in listOf(emptyList(), listOf("only"))) {
            val perm = intPermutation(source.size, seed = 42)
            assertEquals(source, source.permuted(perm))
            assertEquals(source, source.unpermuted(perm))
        }
    }

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
    fun wrappersRejectMismatchedLengthsAndNonFiniteBases() {
        assertFailsWith<IllegalArgumentException> { intPermutation(5, seed = 42).range(-2..3) }
        for (size in listOf(-1, -2)) {
            assertFailsWith<IllegalArgumentException> { intPermutation(size, seed = 42).range(0..4) }
        }
    }
}
