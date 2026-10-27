package com.eignex.kpermute

/**
 * Returns a new list whose elements are permuted by [perm].
 * The original list is not modified.
 *
 * Example:
 * ```
 * val perm = intPermutation(5, seed = 42)
 * val shuffled = listOf("a","b","c","d","e").permuted(perm)
 * ```
 */
fun <T> List<T>.permuted(perm: IntPermutation = intPermutation(size)): List<T> {
    val n = size
    require(perm.size == n) {
        "Permutation domain (${perm.size}) must equal list size ($n)"
    }
    return object : AbstractList<T>() {
        override val size: Int get() = n
        override fun get(index: Int): T = this@permuted[perm.decode(index)]
    }
}

/**
 * Applies the inverse of [perm] as a view, restoring the original order.
 */
fun <T> List<T>.unpermuted(perm: IntPermutation): List<T> {
    val n = size
    require(perm.size == n) {
        "Permutation domain (${perm.size}) must equal list size ($n)"
    }
    return object : AbstractList<T>() {
        override val size: Int get() = n
        override fun get(index: Int): T = this@unpermuted[perm.encode(index)]
    }
}

/**
 * Returns a view of this permutation that operates on [range].
 * Only valid for finite domains where `range.count() == size`.
 * Calling this on an existing range view replaces its range while preserving
 * the underlying permutation.
 *
 * Useful for permuting values within numeric subranges such as dataset shards,
 * sliding windows, or bounded ID segments without manual offset math.
 */
fun IntPermutation.range(range: IntRange): IntPermutation {
    val n = range.last - range.first + 1
    require(size >= 0) { "range() requires a finite base permutation" }
    require(size == n) { "base size ($size) must equal range length ($n)" }

    val base = if (this is IntRangePermutation) this.base else this
    return IntRangePermutation(base, range)
}

private class IntRangePermutation(val base: IntPermutation, private val range: IntRange) : IntPermutation {
    override val size: Int = base.size
    private val start = range.first

    override fun encodeUnchecked(value: Int): Int = start + base.encodeUnchecked(value - start)

    override fun decodeUnchecked(encoded: Int): Int = start + base.decodeUnchecked(encoded - start)

    override fun encode(value: Int): Int {
        require(value in range) { "value $value out of $range" }
        return encodeUnchecked(value)
    }

    override fun decode(encoded: Int): Int {
        require(encoded in range) { "encoded $encoded out of $range" }
        return decodeUnchecked(encoded)
    }

    // Offsets are indices in 0..size, independent of the range's start.
    override fun iterator(offset: Int): IntIterator {
        require(offset in 0..size) { "offset $offset out of [0, $size]" }
        var i = offset
        return object : IntIterator() {
            override fun hasNext() = i < size
            override fun nextInt(): Int {
                if (!hasNext()) throw NoSuchElementException()
                return start + base.encodeUnchecked(i++)
            }
        }
    }
}
