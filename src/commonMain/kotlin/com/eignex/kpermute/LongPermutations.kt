package com.eignex.kpermute

/**
 * Returns a view of this permutation that operates on [range].
 * Only valid for finite domains where `range.count() == size`.
 * Calling this on an existing range view replaces its range while preserving
 * the underlying permutation.
 *
 * Useful for permuting values within numeric subranges such as dataset shards,
 * sliding windows, or bounded ID segments without manual offset math.
 */
fun LongPermutation.range(range: LongRange): LongPermutation {
    val n = range.last - range.first + 1L
    require(size >= 0L) { "range() requires a finite base permutation" }
    require(size == n) { "base size ($size) must equal range length ($n)" }

    val base = if (this is LongRangePermutation) this.base else this
    return LongRangePermutation(base, range)
}

private class LongRangePermutation(val base: LongPermutation, private val range: LongRange) : LongPermutation {
    override val size: Long = base.size
    private val start = range.first

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

    // Offsets are indices in 0..size, independent of the range's start.
    override fun iterator(offset: Long): LongIterator {
        require(offset in 0L..size) { "offset $offset out of [0, $size]" }
        var i = offset
        return object : LongIterator() {
            override fun hasNext() = i < size
            override fun nextLong(): Long {
                if (!hasNext()) throw NoSuchElementException()
                return start + base.encodeUnchecked(i++)
            }
        }
    }
}
