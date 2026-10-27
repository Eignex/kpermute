package com.eignex.kpermute

/**
 * Bijection on `[0, size)`, or on all 64-bit values when `size == -1L`.
 * [size] is read as unsigned. [decode] is the exact inverse of [encode].
 *
 * Not cryptographic. Construct with [longPermutation].
 */
interface LongPermutation : Iterable<Long> {

    /** Domain size, read as unsigned; `-1L` means all 64-bit values. */
    val size: Long

    /** [encode] without range checks. Out-of-domain input is unspecified and may not terminate. */
    fun encodeUnchecked(value: Long): Long

    /** [decode] without range checks. Out-of-domain input is unspecified and may not terminate. */
    fun decodeUnchecked(encoded: Long): Long

    /** Maps [value] to its permuted position; throws if [value] is outside the domain. */
    fun encode(value: Long): Long {
        if (size != -1L) {
            require(value.toULong() < size.toULong()) {
                "value $value out of range [0, ${size.toULong()})"
            }
        }
        return encodeUnchecked(value)
    }

    /** Inverse of [encode]; throws if [encoded] is outside the domain. */
    fun decode(encoded: Long): Long {
        if (size != -1L) {
            require(encoded.toULong() < size.toULong()) {
                "encoded $encoded out of range [0, ${size.toULong()})"
            }
        }
        return decodeUnchecked(encoded)
    }

    /** Iterates `encode(i)` over the whole domain in index order. */
    override fun iterator(): LongIterator = iterator(0)

    /** Iterates `encode(i)` for unsigned indices `i` from [offset], which must be in `0..size`. */
    fun iterator(offset: Long): LongIterator
}
