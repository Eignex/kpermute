package com.eignex.kpermute

/**
 * Reversible permutation over a 32-bit integer domain.
 *
 * An [IntPermutation] defines a bijection on either:
 * - a finite domain `[0, size)` when `size >= 0`, or
 * - the full signed 32-bit space when `size == -1`.
 *
 * Implementations are deterministic: the same instance always maps the same
 * input to the same output, and [decode] is the exact inverse of [encode].
 * They are designed for tasks such as data shuffling, masking, and index
 * remapping without storing full lookup tables.
 *
 * Security note:
 * These permutations are not cryptographic. They are not PRPs and are not
 * intended to resist adversarial inversion or analysis.
 *
 * Contract:
 * - For finite domains (`size >= 0`), valid inputs to [encode] and [decode]
 *   are in `[0, size)`. Out-of-range values trigger [IllegalArgumentException].
 * - [encodeUnchecked] and [decodeUnchecked] skip range checks and must only
 *   be called with valid domain values when `size >= 0`.
 * - [iterator] yields `encode(i)` for all valid `i` in index order.
 *
 * Use [intPermutation] to construct concrete implementations.
 */
interface IntPermutation : Iterable<Int> {

    /**
     * Domain size of the permutation.
     *
     * - `size >= 0`: finite domain `[0, size)`.
     * - `size == -1`: full signed 32-bit domain.
     */
    val size: Int

    /**
     * Encodes a [value] without range checks.
     */
    fun encodeUnchecked(value: Int): Int

    /**
     * Decodes a previously [encoded] value without range checks.
     */
    fun decodeUnchecked(encoded: Int): Int

    /**
     * Encodes an integer in the permutation domain into its permuted value.
     *
     * For finite domains (`size >= 0`), [value] must be in `[0, size)`.
     */
    fun encode(value: Int): Int {
        if (size >= 0) {
            require(value in 0 until size) {
                "value $value out of range [0, $size)"
            }
        }
        return encodeUnchecked(value)
    }

    /**
     * Decodes a previously encoded integer back to its original value.
     *
     * For finite domains (`size >= 0`), [encoded] must be in `[0, size)`.
     */
    fun decode(encoded: Int): Int {
        if (size >= 0) {
            require(encoded in 0 until size) {
                "encoded $encoded out of range [0, $size)"
            }
        }
        return decodeUnchecked(encoded)
    }

    /**
     * Returns an iterator over `encode(i)` for all `i` in `[0, size)` for
     * finite domains, or over the full 32-bit space when `size == -1`.
     */
    override fun iterator(): IntIterator = iterator(0)

    /**
     * Returns an iterator over `encode(i)` for indices in `[offset, size)`.
     *
     * For finite domains, [offset] is an index in `0..size`. For full-domain
     * implementations, semantics are defined by the implementation.
     */
    fun iterator(offset: Int): IntIterator
}
