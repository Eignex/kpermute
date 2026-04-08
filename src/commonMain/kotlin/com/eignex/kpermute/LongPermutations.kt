package com.eignex.kpermute

/**
 * Returns a view of this permutation that operates on [range] instead of `[0, size)`.
 * Only valid for finite domains where `range.count() == size`.
 *
 * Useful for permuting values within numeric subranges such as dataset shards,
 * sliding windows, or bounded ID segments without manual offset math.
 */
fun LongPermutation.range(range: LongRange): LongPermutation {
    val n = range.last - range.first + 1L
    require(size >= 0L) { "range() requires a finite base permutation" }
    require(size == n) { "base size ($size) must equal range length ($n)" }

    val start = range.first
    return object : LongPermutation {
        override val size: Long = n

        // Unchecked operate on *range values*.
        override fun encodeUnchecked(value: Long): Long =
            start + this@range.encodeUnchecked(value - start)

        override fun decodeUnchecked(encoded: Long): Long =
            start + this@range.decodeUnchecked(encoded - start)

        // Checked wrappers validate range membership.
        override fun encode(value: Long): Long {
            require(value in range) { "value $value out of $range" }
            return encodeUnchecked(value)
        }

        override fun decode(encoded: Long): Long {
            require(encoded in range) { "encoded $encoded out of $range" }
            return decodeUnchecked(encoded)
        }

        // Iterator yields permuted values for inputs in [range.first+offset, range.last].
        // offset is measured in *indices* (0..n), consistent with base contract.
        override fun iterator(offset: Long): LongIterator {
            require(offset in 0..n) { "offset $offset out of [0, $n]" }
            var i = offset
            return object : LongIterator() {
                override fun hasNext() = i < n
                override fun nextLong(): Long {
                    if (!hasNext()) throw NoSuchElementException()
                    return start + this@range.encodeUnchecked(i++)
                }
            }
        }
    }
}
