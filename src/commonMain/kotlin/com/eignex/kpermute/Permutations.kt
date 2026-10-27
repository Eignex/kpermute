@file:JvmName("Permutations")

package com.eignex.kpermute

import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.random.Random

// Below this, cycle-walking correlates neighbouring outputs, so use a table.
private const val SMALL_MAX = 4096

private fun defaultRounds(size: ULong): Int = if (size <= 1uL shl 20) 4 else 6

private fun Int.orDefault(default: Int): Int = if (this == 0) default else this

/**
 * Creates an [IntPermutation] over `[0, size)`.
 *
 * - `size == -1`: [FullIntPermutation] over all 32-bit values.
 * - other negative `size`: [BoundedIntPermutation], size read as unsigned.
 * - `size <= 4096`: [SmallIntPermutation].
 * - otherwise: [BoundedIntPermutation].
 *
 * A [rounds] of `0` picks a size-dependent default.
 */
@JvmOverloads
fun intPermutation(size: Int = Int.MAX_VALUE, rng: Random = Random.Default, rounds: Int = 0): IntPermutation {
    require(rounds >= 0) { "rounds must be >= 0" }

    return when {
        size == -1 -> FullIntPermutation(rng, rounds.orDefault(2))
        size in 0..SMALL_MAX -> SmallIntPermutation(size, rng)
        else -> BoundedIntPermutation(size, rng, rounds.orDefault(defaultRounds(size.toUInt().toULong())))
    }
}

/** Seeded variant of [intPermutation]; deterministic for a given `size`, `seed` and `rounds`. */
// No @JvmOverloads: a seed-only Java overload would read like longPermutation(size).
fun intPermutation(size: Int = Int.MAX_VALUE, seed: Long, rounds: Int = 0): IntPermutation =
    intPermutation(size, Random(seed), rounds)

/**
 * Creates an [IntPermutation] whose domain is the non-empty [range].
 * Implementation and default [rounds] are chosen from the range length.
 */
@JvmOverloads
fun intPermutation(range: IntRange, rng: Random = Random.Default, rounds: Int = 0): IntPermutation {
    require(!range.isEmpty()) { "range must be non-empty and increasing: $range" }
    require(rounds >= 0) { "rounds must be >= 0" }
    val lastIndex = range.last.toUInt() - range.first.toUInt()
    val base = when (lastIndex) {
        UInt.MAX_VALUE -> intPermutation(-1, rng, rounds)

        // 2^32 - 1 values; size -1 would mean the full domain.
        UInt.MAX_VALUE - 1u -> BoundedIntPermutation(
            -1,
            rng,
            rounds.orDefault(defaultRounds(UInt.MAX_VALUE.toULong())),
        )

        else -> intPermutation((lastIndex + 1u).toInt(), rng, rounds)
    }
    return base.range(range)
}

/** Seeded variant of [intPermutation] over [range]. */
@JvmOverloads
fun intPermutation(range: IntRange, seed: Long, rounds: Int = 0): IntPermutation =
    intPermutation(range, Random(seed), rounds)

/**
 * Creates a [LongPermutation] over `[0, size)`.
 *
 * - `size == -1L`: [FullLongPermutation] over all 64-bit values.
 * - other negative `size`: [BoundedLongPermutation], size read as unsigned.
 * - `size <= 4096`: [SmallLongPermutation].
 * - otherwise: [BoundedLongPermutation].
 *
 * A [rounds] of `0` picks a size-dependent default.
 */
@JvmOverloads
fun longPermutation(size: Long = Long.MAX_VALUE, rng: Random = Random.Default, rounds: Int = 0): LongPermutation {
    require(rounds >= 0) { "rounds must be >= 0" }

    return when {
        size == -1L -> FullLongPermutation(rng, rounds.orDefault(2))

        size < 0L -> BoundedLongPermutation(
            size,
            rng,
            rounds.orDefault(defaultRounds(size.toULong())),
        )

        size <= SMALL_MAX -> SmallLongPermutation(size, rng)

        // Legacy constant, kept for output stability.
        else -> BoundedLongPermutation(
            size,
            rng,
            rounds.orDefault(defaultRounds(size.toULong())),
            0x52A531B54E4EC5CBuL,
        )
    }
}

/** Seeded variant of [longPermutation]; deterministic for a given `size`, `seed` and `rounds`. */
fun longPermutation(size: Long = Long.MAX_VALUE, seed: Long, rounds: Int = 0): LongPermutation =
    longPermutation(size, Random(seed), rounds)

/**
 * Creates a [LongPermutation] whose domain is the non-empty [range].
 * Implementation and default [rounds] are chosen from the range length.
 */
@JvmOverloads
fun longPermutation(range: LongRange, rng: Random = Random.Default, rounds: Int = 0): LongPermutation {
    require(!range.isEmpty()) { "range must be non-empty and increasing: $range" }
    require(rounds >= 0) { "rounds must be >= 0" }
    val lastIndex = range.last.toULong() - range.first.toULong()
    val base = when (lastIndex) {
        ULong.MAX_VALUE -> longPermutation(-1L, rng, rounds)

        // 2^64 - 1 values; size -1 would mean the full domain.
        ULong.MAX_VALUE - 1uL -> BoundedLongPermutation(
            -1L,
            rng,
            rounds.orDefault(defaultRounds(ULong.MAX_VALUE)),
        )

        else -> longPermutation((lastIndex + 1uL).toLong(), rng, rounds)
    }
    return base.range(range)
}

/** Seeded variant of [longPermutation] over [range]. */
@JvmOverloads
fun longPermutation(range: LongRange, seed: Long, rounds: Int = 0): LongPermutation =
    longPermutation(range, Random(seed), rounds)
