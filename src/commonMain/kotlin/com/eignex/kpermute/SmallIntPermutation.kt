package com.eignex.kpermute

import kotlin.random.Random

/** Permutation over `[0, size)` backed by shuffled lookup tables. */
class SmallIntPermutation(override val size: Int, rng: Random) : IntPermutation {
    private val array: IntArray
    private val inverse: IntArray

    init {
        require(size >= 0) { "size must be >= 0" }
        array = IntArray(size) { it }
        array.shuffle(rng)
        inverse = IntArray(size)
        for (i in 0..<size) {
            inverse[array[i]] = i
        }
    }

    override fun encodeUnchecked(value: Int): Int = array[value]

    override fun decodeUnchecked(encoded: Int): Int = inverse[encoded]

    override fun iterator(offset: Int): IntIterator {
        checkOffset(offset, size)
        return if (offset == 0) array.iterator() else array.sliceArray(offset..<size).iterator()
    }

    override fun toString(): String = "SmallIntPermutation(size=$size)"
}
