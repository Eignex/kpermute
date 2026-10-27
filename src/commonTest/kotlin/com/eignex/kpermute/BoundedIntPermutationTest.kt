package com.eignex.kpermute

import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalUnsignedTypes::class)
class BoundedIntPermutationTest {

    @Test
    fun bijectionVariousSizes() {
        for (n in listOf(1, 2, 3, 4, 7, 15, 64, 1000)) {
            val p = BoundedIntPermutation(n, Random(55))
            CommonAssertsInt.assertBijectionOverDomain(p, n)
        }
    }

    @Test
    fun iteratorFullAndOffsetsEdgeCases() {
        val n = 5
        val p = BoundedIntPermutation(n, Random(1))
        // full
        CommonAssertsInt.assertIteratorMatchesEncode(p, n)
        // offset == last index → one element then end
        val itLast = p.iterator(n - 1)
        assertTrue(itLast.hasNext())
        assertEquals(p.encode(n - 1), itLast.nextInt())
        assertFalse(itLast.hasNext())
        assertFailsWith<NoSuchElementException> { itLast.nextInt() }
        // offset == size → empty and throws on next
        val itEmpty = p.iterator(n)
        assertFalse(itEmpty.hasNext())
        assertFailsWith<NoSuchElementException> { itEmpty.nextInt() }
    }

    @Test
    fun deterministicForSameSeed() {
        val n = 20
        val factory = { BoundedIntPermutation(n, Random(9)) }
        CommonAssertsInt.assertDeterministic(factory)
    }

    @Test
    fun roundsAndConstConstraintsAndVariation() {
        assertFailsWith<IllegalArgumentException> {
            BoundedIntPermutation(32, Random(1), const = 4u)
        }
        val p1 = BoundedIntPermutation(128, Random(5), rounds = 1)
        val p2 = BoundedIntPermutation(128, Random(5), rounds = 5)
        CommonAssertsInt.assertDifferentMapping(p1, p2)
    }

    @Test
    fun rejectsOutOfRange() {
        val n = 10
        val p = BoundedIntPermutation(n, Random(1))
        CommonAssertsInt.assertRangeChecks(p, n)
    }

    @Test
    fun unsignedIntDomainsPreserveValuesOnBothSidesOfTheSignBit() {
        for (size in listOf(Int.MIN_VALUE, Int.MIN_VALUE + 1, -2)) {
            for (seed in listOf(0L, 42L, Long.MIN_VALUE)) {
                val perm = BoundedIntPermutation(size, Random(seed))
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
    fun rejectsOutOfRangeForUnsignedSizes() {
        for (size in listOf(Int.MIN_VALUE, Int.MIN_VALUE + 1, -10, -2, -1)) {
            val permutations = mutableListOf<IntPermutation>(BoundedIntPermutation(size, Random(1)))
            if (size != -1) permutations += intPermutation(size, seed = 1L)
            for (p in permutations) {
                for (invalid in listOf(size, -1).distinct()) {
                    assertFailsWith<IllegalArgumentException> { p.encode(invalid) }
                    assertFailsWith<IllegalArgumentException> { p.decode(invalid) }
                }
                val valid = mutableListOf(0, 1, Int.MAX_VALUE, (size.toUInt() - 1u).toInt())
                if (Int.MIN_VALUE.toUInt() < size.toUInt()) valid += Int.MIN_VALUE
                for (value in valid.distinct()) {
                    val encoded = p.encode(value)
                    assertTrue(encoded.toUInt() < size.toUInt())
                    assertEquals(value, p.decode(encoded))
                    assertEquals(encoded, p.encodeUnchecked(value))
                    assertEquals(value, p.decodeUnchecked(encoded))
                }
            }
        }
    }

    @Test
    fun rejectsZeroSize() {
        assertFailsWith<IllegalArgumentException> { BoundedIntPermutation(0, Random(1)) }
    }
}
