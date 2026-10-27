package com.eignex.kpermute

/**
 * Returns a read-only view of this list with its elements permuted by [perm].
 * The original list is not modified or copied. The view's size is fixed when
 * it is created, so the source list must not change size afterwards.
 *
 * [perm] must have the domain `[0, size)`; a range view of another domain is
 * rejected.
 *
 * Example:
 * ```
 * val perm = intPermutation(5, seed = 42)
 * val shuffled = listOf("a","b","c","d","e").permuted(perm)
 * ```
 */
fun <T> List<T>.permuted(perm: IntPermutation = intPermutation(size)): List<T> {
    val n = size
    requireIndexDomain(perm, n)
    return object : AbstractList<T>() {
        override val size: Int get() = n
        override fun get(index: Int): T = this@permuted[perm.decode(checkIndex(index, n))]
    }
}

/**
 * Applies the inverse of [perm] as a read-only view, restoring the original
 * order. The same requirements as [permuted] apply.
 */
fun <T> List<T>.unpermuted(perm: IntPermutation): List<T> {
    val n = size
    requireIndexDomain(perm, n)
    return object : AbstractList<T>() {
        override val size: Int get() = n
        override fun get(index: Int): T = this@unpermuted[perm.encode(checkIndex(index, n))]
    }
}

private fun requireIndexDomain(perm: IntPermutation, n: Int) {
    require(perm.size == n) {
        "Permutation domain (${perm.size}) must equal list size ($n)"
    }
    require(perm !is IntRangePermutation || perm.start == 0) {
        "Permutation domain must start at 0, not ${(perm as IntRangePermutation).start}"
    }
}

private fun checkIndex(index: Int, n: Int): Int {
    if (index !in 0 until n) throw IndexOutOfBoundsException("index $index out of bounds for size $n")
    return index
}

/**
 * Returns a view of this permutation that operates on [range].
 * Requires a non-empty range with the same number of values as this
 * permutation's domain, which may be unsigned or the full 32-bit space.
 * Calling this on an existing range view replaces its range while preserving
 * the underlying permutation.
 *
 * Useful for permuting values within numeric subranges such as dataset shards,
 * sliding windows, or bounded ID segments without manual offset math.
 */
fun IntPermutation.range(range: IntRange): IntPermutation {
    require(!range.isEmpty()) { "range must be non-empty: $range" }
    require(size != 0) { "range() requires a non-empty base permutation" }
    val lastIndex = range.last.toUInt() - range.first.toUInt()
    require(lastIndex() == lastIndex) {
        "base size (${lastIndex().toLong() + 1L}) must equal range length (${lastIndex.toLong() + 1L})"
    }

    val base = if (this is IntRangePermutation) this.base else this
    return IntRangePermutation(base, range)
}

// Largest index of a non-empty domain; size -1 is the full domain unless it is
// a bounded permutation, where it means 2^32 - 1 values.
private fun IntPermutation.lastIndex(): UInt = when {
    this is IntRangePermutation -> lastIndex
    size == -1 && this !is BoundedIntPermutation -> UInt.MAX_VALUE
    else -> size.toUInt() - 1u
}

private class IntRangePermutation(val base: IntPermutation, private val range: IntRange) : IntPermutation {
    override val size: Int = base.size
    val start = range.first
    val lastIndex: UInt = range.last.toUInt() - range.first.toUInt()

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

    // Offsets are unsigned indices in 0..size, independent of the range's start.
    override fun iterator(offset: Int): IntIterator {
        var i = offset.toUInt()
        require(lastIndex == UInt.MAX_VALUE || i <= lastIndex + 1u) {
            "offset $i out of range [0, ${lastIndex.toLong() + 1L}]"
        }
        var exhausted = i > lastIndex
        return object : IntIterator() {
            override fun hasNext() = !exhausted
            override fun nextInt(): Int {
                if (!hasNext()) throw NoSuchElementException()
                exhausted = i == lastIndex
                return start + base.encodeUnchecked((i++).toInt())
            }
        }
    }
}
