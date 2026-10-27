package com.eignex.kpermute

/**
 * Bijection on `[0, size)`, or on all 32-bit values when `size == -1`.
 * [size] is read as unsigned. [decode] is the exact inverse of [encode].
 *
 * Not cryptographic. Construct with [intPermutation].
 */
interface IntPermutation : Iterable<Int> {

    /** Domain size, read as unsigned; `-1` means all 32-bit values. */
    val size: Int

    /** [encode] without range checks. Out-of-domain input is unspecified and may not terminate. */
    fun encodeUnchecked(value: Int): Int

    /** [decode] without range checks. Out-of-domain input is unspecified and may not terminate. */
    fun decodeUnchecked(encoded: Int): Int

    /** Maps [value] to its permuted position; throws if [value] is outside the domain. */
    fun encode(value: Int): Int {
        if (size != -1) {
            require(value.toUInt() < size.toUInt()) {
                "value $value out of range [0, ${size.toUInt()})"
            }
        }
        return encodeUnchecked(value)
    }

    /** Inverse of [encode]; throws if [encoded] is outside the domain. */
    fun decode(encoded: Int): Int {
        if (size != -1) {
            require(encoded.toUInt() < size.toUInt()) {
                "encoded $encoded out of range [0, ${size.toUInt()})"
            }
        }
        return decodeUnchecked(encoded)
    }

    /** Iterates `encode(i)` over the whole domain in index order. */
    override fun iterator(): IntIterator = iterator(0)

    /** Iterates `encode(i)` for unsigned indices `i` from [offset], which must be in `0..size`. */
    fun iterator(offset: Int): IntIterator
}
