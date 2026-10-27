package com.eignex.kpermute

import kotlin.test.*

class ListPermutationTest {
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
}
