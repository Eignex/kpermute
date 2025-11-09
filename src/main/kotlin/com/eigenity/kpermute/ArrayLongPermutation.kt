package com.eigenity.kpermute

import kotlin.random.Random

class ArrayLongPermutation(
    override val size: Int,
    rng: Random
) : IntPermutation {
    private val array = IntArray(size) { it }
    private val inverse: IntArray

    init {
        array.shuffle(rng)
        inverse = IntArray(size)
        for (i in 0..<size) {
            inverse[array[i]] = i
        }
    }

    override fun encodeUnchecked(value: Int): Int = array[value]

    override fun decodeUnchecked(encoded: Int): Int = inverse[encoded]

    override fun iterator(offset: Int): IntIterator =
        if (offset == 0) array.iterator()
        else array.sliceArray(offset..<size).iterator()

    override fun toString(): String = "ArrayIntPermutation(size=$size)"
}
