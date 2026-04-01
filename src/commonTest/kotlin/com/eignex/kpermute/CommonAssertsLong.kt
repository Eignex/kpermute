package com.eignex.kpermute

import kotlin.test.*

object CommonAssertsLong {

    /** Verify encode/decode is a bijection on [0, size). */
    fun assertBijectionOverDomain(p: LongPermutation, size: Long) {
        val n = size.toInt()
        val image = LongArray(n) { p.encode(it.toLong()) }

        // Range check
        for (v in image) assertTrue(v in 0L until size)

        // Injective / surjective
        assertEquals(n, image.toSet().size)

        // Decode correctness
        for (i in 0 until n) {
            assertEquals(i.toLong(), p.decode(image[i]))
        }
    }

    /** Verify iterator yields encode(i) for i in [offset, size). */
    fun assertIteratorMatchesEncode(
        p: LongPermutation,
        size: Long,
        offset: Long = 0L
    ) {
        val it = p.iterator(offset)
        val actual = it.asSequence().toList()

        val expected = mutableListOf<Long>()
        var i = offset
        while (i < size) {
            expected += p.encode(i)
            i++
        }

        assertEquals(expected, actual)
    }

    /** Verify deterministic behavior for same seed. */
    fun assertDeterministic(factory: () -> LongPermutation) {
        val p1 = factory()
        val p2 = factory()
        val n: Int = when (val size = p1.size) {
            -1L -> 128
            else -> minOf(size, 128L).toInt()
        }
        for (i in 0 until n) {
            assertEquals(
                p1.encode(i.toLong()),
                p2.encode(i.toLong())
            )
        }
    }

    /** Verify different seeds or rounds change mapping. */
    fun assertDifferentMapping(
        p1: LongPermutation,
        p2: LongPermutation,
        sample: Long = 42L
    ) {
        assertNotEquals(p1.encode(sample), p2.encode(sample))
    }

    /** Verify encode/decode reject out-of-range when a finite size exists. */
    fun assertRangeChecks(p: LongPermutation, size: Long) {
        assertFailsWith<IllegalArgumentException> { p.encode(-1L) }
        assertFailsWith<IllegalArgumentException> { p.encode(size) }
        assertFailsWith<IllegalArgumentException> { p.decode(-1L) }
        assertFailsWith<IllegalArgumentException> { p.decode(size) }
    }
}
