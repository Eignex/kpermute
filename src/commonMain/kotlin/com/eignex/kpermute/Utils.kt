package com.eignex.kpermute

/** Requires [offset] in `0..size`, unsigned. */
internal fun checkOffset(offset: Int, size: Int) {
    require(offset.toUInt() <= size.toUInt()) {
        "offset ${offset.toUInt()} out of range [0, ${size.toUInt()}]"
    }
}

/** Requires [offset] in `0..size`, unsigned. */
internal fun checkOffset(offset: Long, size: Long) {
    require(offset.toULong() <= size.toULong()) {
        "offset ${offset.toULong()} out of range [0, ${size.toULong()}]"
    }
}

internal object PermMathInt {
    /** Smallest k with 2^k >= size; returns (2^k - 1, k, k*3/7). */
    fun block(sizeExclusive: Int): Triple<Int, Int, Int> {
        val k = if (sizeExclusive <= 1) {
            1
        } else {
            32 - (sizeExclusive - 1).countLeadingZeroBits()
        }
        val mask = -1 ushr (32 - k)
        val rshift = ((k * 3) / 7).coerceAtLeast(1)
        return Triple(mask, k, rshift)
    }

    /** Inverse of v ^= v >>> s on k-bit words. */
    fun invXorShift(v0: Int, s0: Int, kBits: Int, mask: Int): Int {
        var v = v0 and mask
        var s = s0
        while (s < kBits) {
            v = v xor (v ushr s)
            s = s shl 1
        }
        return v and mask
    }

    /** Inverse of odd [a] modulo `mask + 1`. */
    fun invOdd32(a: Int, mask: Int): Int {
        var inv = 1
        repeat(6) { inv *= (2 - a * inv) }
        return inv and mask
    }
}

internal object PermMathUInt {
    /** Smallest k with 2^k >= size; returns (2^k - 1, k, k*3/7). */
    fun block(sizeExclusive: UInt): Triple<UInt, Int, Int> {
        val k = if (sizeExclusive <= 1u) {
            1
        } else {
            32 - ((sizeExclusive - 1u).toInt()).countLeadingZeroBits()
        }
        val mask = if (k == 32) 0xFFFF_FFFFu else (1u shl k) - 1u
        val rshift = ((k * 3) / 7).coerceAtLeast(1)
        return Triple(mask, k, rshift)
    }

    /** Inverse of v ^= v >>> s on k-bit words. */
    fun invXorShift(v0: UInt, s0: Int, kBits: Int, mask: UInt): UInt {
        var v = v0 and mask
        var s = s0
        while (s < kBits) {
            v = v xor (v shr s)
            s = s shl 1
        }
        return v and mask
    }

    /** Inverse of odd [a] modulo `mask + 1`. */
    fun invOdd32(a: UInt, mask: UInt): UInt {
        var inv = 1u
        repeat(6) { inv = (inv * (2u - a * inv)) and mask }
        return inv and mask
    }
}

internal object PermMathLong {
    /** Smallest k with 2^k >= size; returns (2^k - 1, k, k*3/7). */
    fun block(sizeExclusive: Long): Triple<Long, Int, Int> {
        val k = if (sizeExclusive <= 1L) {
            1
        } else {
            64 - (sizeExclusive - 1L).countLeadingZeroBits()
        }
        val mask = if (k == 64) -1L else -1L ushr (64 - k)
        val rshift = ((k * 3) / 7).coerceAtLeast(1)
        return Triple(mask, k, rshift)
    }

    /** Inverse of v ^= v >>> s on k-bit words. */
    fun invXorShift(v0: Long, s0: Int, kBits: Int, mask: Long): Long {
        var v = v0 and mask
        var s = s0
        while (s < kBits) {
            v = v xor (v ushr s)
            s = s shl 1
        }
        return v and mask
    }

    /** Inverse of odd [a] modulo `mask + 1`. */
    fun invOdd64(a: Long, mask: Long): Long {
        var inv = 1L
        repeat(6) { inv *= (2L - a * inv) }
        return inv and mask
    }
}

internal object PermMathULong {
    /** Smallest k with 2^k >= size; returns (2^k - 1, k, k*3/7). */
    fun block(sizeExclusive: ULong): Triple<ULong, Int, Int> {
        val k = if (sizeExclusive <= 1uL) {
            1
        } else {
            64 - ((sizeExclusive - 1uL).toLong()).countLeadingZeroBits()
        }
        val mask = if (k == 64) 0xFFFF_FFFF_FFFF_FFFFuL else (1uL shl k) - 1uL
        val rshift = ((k * 3) / 7).coerceAtLeast(1)
        return Triple(mask, k, rshift)
    }

    /** Inverse of v ^= v >>> s on k-bit words. */
    fun invXorShift(v0: ULong, s0: Int, kBits: Int, mask: ULong): ULong {
        var v = v0 and mask
        var s = s0
        while (s < kBits) {
            v = v xor (v shr s)
            s = s shl 1
        }
        return v and mask
    }

    /** Inverse of odd [a] modulo `mask + 1`. */
    fun invOdd64(a: ULong, mask: ULong): ULong {
        var inv = 1uL
        repeat(6) { inv = (inv * (2uL - a * inv)) and mask }
        return inv and mask
    }
}
