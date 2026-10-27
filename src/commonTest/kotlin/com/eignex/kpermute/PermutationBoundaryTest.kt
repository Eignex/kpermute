package com.eignex.kpermute

import kotlin.test.*

class PermutationBoundaryTest {
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
    fun unsignedIntDomainsPreserveValuesOnBothSidesOfTheSignBit() {
        for (size in listOf(Int.MIN_VALUE, Int.MIN_VALUE + 1, -2)) {
            for (seed in seeds) {
                val perm = intPermutation(size, seed = seed)
                val samples = listOf(0, 1, Int.MAX_VALUE - 1, Int.MAX_VALUE, Int.MIN_VALUE, size - 1)
                    .filter { it.toUInt() < size.toUInt() }
                for (value in samples) {
                    val context = "size=$size seed=$seed value=$value"
                    assertTrue(perm.encode(value).toUInt() < size.toUInt(), context)
                    assertTrue(perm.decode(value).toUInt() < size.toUInt(), context)
                    assertEquals(value, perm.decode(perm.encode(value)), context)
                    assertEquals(value, perm.encode(perm.decode(value)), context)
                }
                val iterator = perm.iterator(size - 2)
                for (value in listOf(size - 2, size - 1)) {
                    assertTrue(iterator.hasNext())
                    assertEquals(perm.encode(value), iterator.nextInt())
                }
                assertFalse(iterator.hasNext())
                assertFailsWith<NoSuchElementException> { iterator.nextInt() }
                val end = perm.iterator(size)
                assertFalse(end.hasNext())
                assertFailsWith<NoSuchElementException> { end.nextInt() }
            }
        }
    }

    @Test
    fun unsignedLongDomainsPreserveValuesOnBothSidesOfTheSignBit() {
        for (size in listOf(Long.MIN_VALUE, Long.MIN_VALUE + 1, -2L)) {
            for (seed in seeds) {
                val perm = longPermutation(size, seed = seed)
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
}
