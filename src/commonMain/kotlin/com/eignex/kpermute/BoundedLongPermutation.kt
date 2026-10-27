package com.eignex.kpermute

import kotlin.random.Random

/**
 * Finite permutation over `[0, size)` using cycle-walking over a 2^k block.
 *
 * Applies reversible affine steps and XOR-shift mixing within the smallest
 * power-of-two block containing the domain, and walks outputs outside
 * `[0, size)` until they fall back inside. [size] is read as unsigned, so
 * negative values select domains larger than the signed Long maximum.
 * [encode] and [decode] reject inputs whose unsigned representation is outside
 * `[0, size.toULong())`.
 *
 * @property [size] Domain size, read as an unsigned 64-bit value.
 * @param [rng] Random generator used to derive per-round keys.
 * @param [rounds] Number of mixing rounds; higher values increase dispersion.
 * @param [const] Odd multiplicative constant used in each affine step.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class BoundedLongPermutation(
    override val size: Long,
    rng: Random = Random.Default,
    private val rounds: Int = 3,
    private val const: ULong = 0x9E3779B97F4A7C15U,
) : LongPermutation {

    private val usize: ULong = size.toULong()
    private val mask: ULong
    private val kBits: Int
    private val rshift: Int
    private val invConst: ULong

    init {
        require(usize != 0uL) { "size must be != 0" }
        require(rounds > 0) { "rounds must be > 0" }
        require(const % 2uL == 1uL) { "const must be odd" }

        val (m, k, r) = PermMathULong.block(usize)
        mask = m
        kBits = k
        rshift = r
        invConst = PermMathULong.invOdd64(const, mask)
    }

    // Declared after init so invalid rounds fail its require first.
    private val keys: ULongArray =
        ULongArray(rounds) { rng.nextLong().toULong() }

    // Overrides the interface check, which treats size == -1 as the full
    // domain; here -1 means 2^64 - 1 values like any other unsigned size.
    override fun encode(value: Long): Long {
        require(value.toULong() < usize) {
            "value $value out of range [0, $usize)"
        }
        return encodeUnchecked(value)
    }

    override fun decode(encoded: Long): Long {
        require(encoded.toULong() < usize) {
            "encoded $encoded out of range [0, $usize)"
        }
        return decodeUnchecked(encoded)
    }

    override fun encodeUnchecked(value: Long): Long {
        val u = value.toULong()
        var x = u and mask
        do {
            repeat(rounds) { r ->
                x = (x * const + keys[r]) and mask
                x = x xor (x shr rshift)
            }
        } while (x >= usize)
        return x.toLong()
    }

    override fun decodeUnchecked(encoded: Long): Long {
        val u = encoded.toULong()
        var x = u and mask
        do {
            for (r in rounds - 1 downTo 0) {
                x = PermMathULong.invXorShift(x, rshift, kBits, mask)
                x = ((x - keys[r]) and mask) * invConst and mask
            }
        } while (x >= usize)
        return x.toLong()
    }

    override fun iterator(offset: Long): LongIterator {
        checkOffset(offset, size)
        var i = offset.toULong()
        return object : LongIterator() {
            override fun hasNext() = i < usize
            override fun nextLong(): Long {
                if (!hasNext()) throw NoSuchElementException()
                return encodeUnchecked((i++).toLong())
            }
        }
    }

    override fun toString(): String = "BoundedLongPermutation(size=$size)"
}
