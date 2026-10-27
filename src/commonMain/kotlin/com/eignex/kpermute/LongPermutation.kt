package com.eignex.kpermute

/**
 * Reversible permutation over a 64-bit integer domain.
 *
 * A [LongPermutation] defines a bijection on either:
 * - a finite domain `[0, size)` when `size != -1L`, with [size] read as
 *   unsigned so negative values select domains above the signed maximum, or
 * - the full signed 64-bit space when `size == -1L`.
 *
 * Implementations are deterministic: the same instance always maps the same
 * input to the same output, and [decode] is the exact inverse of [encode].
 * They are suitable for repeatable shuffling, masking, and index remapping
 * without full lookup tables.
 *
 * Security note:
 * These permutations are not cryptographic. They are not PRPs and are not
 * intended to resist adversarial inversion or analysis.
 *
 * Contract:
 * - For finite domains (`size != -1L`), valid inputs to [encode] and [decode]
 *   are in `[0, size)`, compared as unsigned. Out-of-range values trigger
 *   [IllegalArgumentException].
 * - [encodeUnchecked] and [decodeUnchecked] skip range checks and must only
 *   be called with valid domain values when `size != -1L`.
 * - [iterator] yields `encode(i)` for all valid `i` in index order.
 *
 * Use [longPermutation] to construct concrete implementations.
 */
interface LongPermutation : Iterable<Long> {

    /**
     * Domain size of the permutation.
     *
     * - `size != -1L`: finite domain `[0, size)`, with [size] read as unsigned.
     * - `size == -1L`: full signed 64-bit domain.
     */
    val size: Long

    /**
     * Encodes a [value] without range checks.
     */
    fun encodeUnchecked(value: Long): Long

    /**
     * Decodes a previously [encoded] value without range checks.
     */
    fun decodeUnchecked(encoded: Long): Long

    /**
     * Encodes a long in the permutation domain into its permuted value.
     *
     * For finite domains (`size != -1L`), [value] must be in `[0, size)`,
     * compared as unsigned.
     */
    fun encode(value: Long): Long {
        if (size != -1L) {
            require(value.toULong() < size.toULong()) {
                "value $value out of range [0, ${size.toULong()})"
            }
        }
        return encodeUnchecked(value)
    }

    /**
     * Decodes a previously encoded long back to its original value.
     *
     * For finite domains (`size != -1L`), [encoded] must be in `[0, size)`,
     * compared as unsigned.
     */
    fun decode(encoded: Long): Long {
        if (size != -1L) {
            require(encoded.toULong() < size.toULong()) {
                "encoded $encoded out of range [0, ${size.toULong()})"
            }
        }
        return decodeUnchecked(encoded)
    }

    /**
     * Returns an iterator over `encode(i)` for all `i` in `[0, size)` for
     * finite domains, or over the full 64-bit space when `size == -1L`.
     */
    override fun iterator(): LongIterator = iterator(0)

    /**
     * Returns an iterator over `encode(i)` for indices in `[offset, size)`.
     *
     * For finite domains, [offset] is an index in `0..size`, compared as
     * unsigned; other offsets throw [IllegalArgumentException]. For full-domain
     * implementations, semantics are defined by the implementation.
     */
    fun iterator(offset: Long): LongIterator
}
