package com.eigenity.kpermute

import kotlin.random.Random

@OptIn(ExperimentalUnsignedTypes::class)
class ULongPermutation(
    override val size: Long,
    rng: Random = Random.Default,
    private val rounds: Int = 3,
    private val const: ULong = 0x52A531B54E4EC5CBU
) : LongPermutation {

    private val usize: ULong = size.toULong()
    private val mask: ULong
    private val kBits: Int
    private val rshift: Int
    private val keys: ULongArray = ULongArray(rounds) { rng.nextLong().toULong() }
    private val invConst: ULong

    init {
        require(rounds > 0) { "rounds must be > 0" }
        require(const % 2uL == 1uL) { "const must be odd" }

        val (m, k, r) = PermMathULong.block(usize)
        mask = m; kBits = k; rshift = r
        invConst = PermMathULong.invOdd64(const, mask)
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
        var i = offset.toULong()
        return object : LongIterator() {
            override fun hasNext() = i < usize
            override fun nextLong(): Long {
                if (!hasNext()) throw NoSuchElementException()
                return encodeUnchecked((i++).toLong())
            }
        }
    }

    override fun toString(): String = "ULongPermutation(size=$size)"
}
