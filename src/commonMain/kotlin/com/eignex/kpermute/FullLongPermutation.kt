package com.eignex.kpermute

import kotlin.random.Random

/**
 * Permutation over all 64-bit values using keyed xor-shift-multiply rounds.
 *
 * @param c1 odd multiplier.
 * @param c2 odd multiplier.
 */
class FullLongPermutation(
    rng: Random = Random.Default,
    private val rounds: Int = 2,
    private val c1: Long = -4658895280553007687,
    private val c2: Long = -7723592293110705685,
) : LongPermutation {

    override val size: Long get() = -1L

    init {
        require(rounds > 0) { "rounds must be > 0" }
        require(c1 and 1L == 1L) { "c1 must be odd" }
        require(c2 and 1L == 1L) { "c2 must be odd" }
    }

    private val k1: LongArray = LongArray(rounds) { rng.nextLong() }
    private val k2: LongArray = LongArray(rounds) { rng.nextLong() }
    private val c1Inv: Long = PermMathLong.invOdd64(c1, -1L)
    private val c2Inv: Long = PermMathLong.invOdd64(c2, -1L)

    override fun encodeUnchecked(value: Long): Long {
        var x = value
        repeat(rounds) { r ->
            x = x xor k1[r]
            x = x xor (x ushr 30)
            x *= c1
            x = x xor (x ushr 27)
            x *= c2
            x = x xor (x ushr 31)
            x = x xor k2[r]
        }
        return x
    }

    override fun decodeUnchecked(encoded: Long): Long {
        var y = encoded
        for (r in rounds - 1 downTo 0) {
            y = y xor k2[r]
            y = PermMathLong.invXorShift(y, 31, 64, -1L)
            y *= c2Inv
            y = PermMathLong.invXorShift(y, 27, 64, -1L)
            y *= c1Inv
            y = PermMathLong.invXorShift(y, 30, 64, -1L)
            y = y xor k1[r]
        }
        return y
    }

    /** Iterates unsigned indices from [offset] through `-1`. */
    override fun iterator(offset: Long): LongIterator {
        var i = offset
        var exhausted = false
        return object : LongIterator() {
            override fun hasNext() = !exhausted
            override fun nextLong(): Long {
                if (!hasNext()) throw NoSuchElementException()
                val result = encodeUnchecked(i++)
                exhausted = i == 0L
                return result
            }
        }
    }

    override fun toString(): String = "FullLongPermutation(size=$size)"
}
