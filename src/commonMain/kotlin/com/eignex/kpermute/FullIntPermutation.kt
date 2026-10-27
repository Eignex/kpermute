package com.eignex.kpermute

import kotlin.random.Random

/**
 * Permutation over all 32-bit values using keyed xor-shift-multiply rounds.
 *
 * @param c1 odd multiplier.
 * @param c2 odd multiplier.
 */
class FullIntPermutation(
    rng: Random = Random.Default,
    private val rounds: Int = 2,
    private val c1: Int = -2048144789,
    private val c2: Int = -1028477387,
) : IntPermutation {

    override val size: Int get() = -1

    init {
        require(rounds > 0) { "rounds must be > 0" }
        require(c1 and 1 == 1) { "c1 must be odd" }
        require(c2 and 1 == 1) { "c2 must be odd" }
    }

    private val k1: IntArray = IntArray(rounds) { rng.nextInt() }
    private val k2: IntArray = IntArray(rounds) { rng.nextInt() }
    private val c1Inv: Int = PermMathInt.invOdd32(c1, -1)
    private val c2Inv: Int = PermMathInt.invOdd32(c2, -1)

    override fun encodeUnchecked(value: Int): Int {
        var x = value
        repeat(rounds) { r ->
            x = x xor k1[r]
            x = x xor (x ushr 16)
            x *= c1
            x = x xor (x ushr 15)
            x *= c2
            x = x xor (x ushr 16)
            x = x xor k2[r]
        }
        return x
    }

    override fun decodeUnchecked(encoded: Int): Int {
        var y = encoded
        for (r in rounds - 1 downTo 0) {
            y = y xor k2[r]
            y = PermMathInt.invXorShift(y, 16, 32, -1)
            y *= c2Inv
            y = PermMathInt.invXorShift(y, 15, 32, -1)
            y *= c1Inv
            y = PermMathInt.invXorShift(y, 16, 32, -1)
            y = y xor k1[r]
        }
        return y
    }

    /** Iterates unsigned indices from [offset] through `-1`. */
    override fun iterator(offset: Int): IntIterator {
        var i = offset
        var exhausted = false
        return object : IntIterator() {
            override fun hasNext() = !exhausted
            override fun nextInt(): Int {
                if (!hasNext()) throw NoSuchElementException()
                val result = encodeUnchecked(i++)
                exhausted = i == 0
                return result
            }
        }
    }

    override fun toString(): String = "FullIntPermutation(size=$size)"
}
