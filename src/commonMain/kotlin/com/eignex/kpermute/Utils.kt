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

internal object PermMath32 {
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

internal object PermMath64 {
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
