package com.eignex.kpermute

/**
 * Returns a read-only view of this list reordered by [perm], whose domain must
 * be `[0, size)`. The view does not copy, so the source must not change size.
 */
fun <T> List<T>.permuted(perm: IntPermutation = intPermutation(size)): List<T> {
    val n = size
    requireIndexDomain(perm, n)
    return object : AbstractList<T>() {
        override val size: Int get() = n
        override fun get(index: Int): T = this@permuted[perm.decode(checkIndex(index, n))]
    }
}

/** Inverse of [permuted]. */
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
 * Returns a view of this permutation shifted onto [range], which must hold
 * exactly [IntPermutation.size] values. On an existing range view, replaces its range.
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

// Bounded permutations use size -1 for 2^32 - 1 values, not the full domain.
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

    // Offsets are indices, not range values.
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

    override fun toString(): String = "IntRangePermutation(range=$range, base=$base)"
}
