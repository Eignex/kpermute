package com.eignex.kpermute

/**
 * Returns a view of this permutation that operates on [range].
 * Requires a non-empty range with the same number of values as this
 * permutation's domain, which may be unsigned or the full 64-bit space.
 * Calling this on an existing range view replaces its range while preserving
 * the underlying permutation.
 *
 * Useful for permuting values within numeric subranges such as dataset shards,
 * sliding windows, or bounded ID segments without manual offset math.
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

// Largest index of a non-empty domain; size -1 is the full domain unless it is
// a bounded permutation, where it means 2^64 - 1 values.
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

    // Offsets are unsigned indices in 0..size, independent of the range's start.
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
