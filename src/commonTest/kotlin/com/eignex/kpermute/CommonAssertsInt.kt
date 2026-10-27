package com.eignex.kpermute

import kotlin.test.*

object CommonAssertsInt {

    fun assertBijectionOverDomain(p: IntPermutation, size: Int) {
        val image = IntArray(size) { p.encode(it) }
        for (v in image) assertTrue(v in 0 until size)
        assertEquals(size, image.toSet().size)
        for (i in 0 until size) {
            assertEquals(i, p.decode(image[i]))
        }
    }

    fun assertIteratorMatchesEncode(p: IntPermutation, size: Int, offset: Int = 0) {
        val it = p.iterator(offset)
        val actual = it.asSequence().toList()
        val expected = (offset until size).map { p.encode(it) }
        assertEquals(expected, actual)
    }

    fun assertDeterministic(factory: () -> IntPermutation) {
        val p1 = factory()
        val p2 = factory()
        val n = when (val size = p1.size) {
            -1 -> 128
            else -> minOf(size, 128)
        }
        for (i in 0 until n) assertEquals(p1.encode(i), p2.encode(i))
    }

    fun assertDifferentMapping(p1: IntPermutation, p2: IntPermutation, sample: Int = 42) {
        assertNotEquals(p1.encode(sample), p2.encode(sample))
    }

    fun assertRangeChecks(p: IntPermutation, size: Int) {
        assertFailsWith<IllegalArgumentException> { p.encode(-1) }
        assertFailsWith<IllegalArgumentException> { p.encode(size) }
        assertFailsWith<IllegalArgumentException> { p.decode(-1) }
        assertFailsWith<IllegalArgumentException> { p.decode(size) }
    }
}
