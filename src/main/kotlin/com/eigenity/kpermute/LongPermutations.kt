package com.eigenity.kpermute

/**
 * Represents a reversible integer permutation over a finite or full 64-bit domain.
 *
 * Implementations provide deterministic bijections for integer sets, allowing
 * repeatable shuffling, masking, or indexing without storing lookup tables.
 *
 * ## Security note
 * These permutations are **not cryptographic**. They use lightweight avalanche
 * and cycle-walking techniques for uniform dispersion, but provide no
 * pseudorandom permutation (PRP) or resistance to inversion by an adversary.
 *
 * ## Domain semantics
 * - For finite domains, `size` defines the valid range `[0, size)`.
 * - A `size` of `-1L` represents the full signed 64-bit integer space.
 * - `encodeUnchecked`/`decodeUnchecked` skip bounds checks for performance;
 *   callers must ensure arguments are within the domain when `size >= 0L`.
 *
 * Implementations are iterable and yield `encode(i)` for all valid `i`.
 *
 * Use the factory method [longPermutation] for instantiation.
 */
interface LongPermutation : Iterable<Long> {

    /**
     * Domain of the permutation.
     */
    val size: Long


    /** Fast path. No range checks. Precondition: if size >= 0 then arg ∈ [0, size). */
    fun encodeUnchecked(value: Long): Long

    /** Fast path. No range checks. Precondition: if size >= 0 then arg ∈ [0, size). */
    fun decodeUnchecked(encoded: Long): Long

    /**
     * Encode an integer in [0, [size]) into its permuted value.
     */
    fun encode(value: Long): Long {
        if (size >= 0) require(value in 0 until size) {
            "value $value out of range [0, $size)"
        }
        return encodeUnchecked(value)
    }

    /**
     *  Decode a previously encoded integer back to its original value.
     */
    fun decode(encoded: Long): Long {
        if (size >= 0) require(encoded in 0 until size) {
            "encoded $encoded out of range [0, $size)"
        }
        return decodeUnchecked(encoded)
    }

    /**
     * Iterator that yields encoded values for [0, size).
     */
    override fun iterator(): LongIterator = iterator(0)

    /**
     * Iterator that yields encoded values for [offset, size).
     */
    fun iterator(offset: Long): LongIterator
}

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
