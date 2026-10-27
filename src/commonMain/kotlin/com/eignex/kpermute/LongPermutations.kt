package com.eignex.kpermute

/**
 * Returns a view of this permutation shifted onto [range], which must hold
 * exactly [size] values. On an existing range view, replaces its range.
 */
fun LongPermutation.range(range: LongRange): LongPermutation {
    require(!range.isEmpty()) { "range must be non-empty: $range" }
    require(size != 0L) { "range() requires a non-empty base permutation" }
    val lastIndex = range.last.toULong() - range.first.toULong()
    require(lastIndex() == lastIndex) {
        "base size (${lastIndex() + 1uL}) must equal range length (${lastIndex + 1uL})"
    }

    val base = if (this is LongRangePermutation) this.base else this
    return LongRangePermutation(base, range)
}

// Bounded permutations use size -1 for 2^64 - 1 values, not the full domain.
private fun LongPermutation.lastIndex(): ULong = when {
    this is LongRangePermutation -> lastIndex
    size == -1L && this !is BoundedLongPermutation -> ULong.MAX_VALUE
    else -> size.toULong() - 1uL
}

private class LongRangePermutation(val base: LongPermutation, private val range: LongRange) : LongPermutation {
    override val size: Long = base.size
    private val start = range.first
    val lastIndex: ULong = range.last.toULong() - range.first.toULong()

    override fun encodeUnchecked(value: Long): Long = start + base.encodeUnchecked(value - start)

    override fun decodeUnchecked(encoded: Long): Long = start + base.decodeUnchecked(encoded - start)

    override fun encode(value: Long): Long {
        require(value in range) { "value $value out of $range" }
        return encodeUnchecked(value)
    }

    override fun decode(encoded: Long): Long {
        require(encoded in range) { "encoded $encoded out of $range" }
        return decodeUnchecked(encoded)
    }

    // Offsets are indices, not range values.
    override fun iterator(offset: Long): LongIterator {
        var i = offset.toULong()
        require(lastIndex == ULong.MAX_VALUE || i <= lastIndex + 1uL) {
            "offset $i out of range [0, ${lastIndex + 1uL}]"
        }
        var exhausted = i > lastIndex
        return object : LongIterator() {
            override fun hasNext() = !exhausted
            override fun nextLong(): Long {
                if (!hasNext()) throw NoSuchElementException()
                exhausted = i == lastIndex
                return start + base.encodeUnchecked((i++).toLong())
            }
        }
    }
}
