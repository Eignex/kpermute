@file:JvmName("Permutations")

package com.eignex.kpermute

import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.random.Random

// Largest domain backed by a lookup table. Cycle-walking shows measurable
// correlation between neighbouring outputs below this size.
private const val SMALL_MAX = 4096

// Default rounds for cycle-walking permutations by unsigned domain size.
private fun defaultRounds(size: ULong): Int = when {
    // up to 1 M
    size <= 1uL shl 20 -> 4

    // larger domains
    else -> 6
}

/**
 * Creates an [IntPermutation] for a contiguous integer domain.
 *
 * The concrete implementation depends on [size]:
 * - `size == -1` uses [FullIntPermutation] over the full signed 32-bit range.
 * - `size < 0` (excluding `-1`) uses [BoundedIntPermutation] with [size] read as unsigned.
 * - `0 <= size <= 4096` uses [SmallIntPermutation].
 * - `size > 4096` uses [BoundedIntPermutation] with cycle-walking over a 2^k block.
 *
 * When [rounds] is `0`, a size-dependent default is chosen to balance speed and
 * dispersion. The permutation is reproducible for a given [rng] state.
 *
 * @param size domain size. For non-negative values the domain is `[0, size)`.
 *             Special values: `-1` selects a full 32-bit permutation, other
 *             negative values select an unsigned-style domain.
 * @param rng random source used to derive internal keys and parameters.
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return an [IntPermutation] with domain `[0, size)` or the full 32-bit space
 *         when `size == -1`.
 */
@JvmOverloads
fun intPermutation(size: Int = Int.MAX_VALUE, rng: Random = Random.Default, rounds: Int = 0): IntPermutation {
    require(rounds >= 0) { "rounds must be >= 0" }

    return when {
        size == -1 -> FullIntPermutation(
            rng,
            if (rounds == 0) 2 else rounds, // FullInt needs few rounds
        )

        size < 0 -> BoundedIntPermutation(
            size,
            rng,
            if (rounds == 0) defaultRounds(size.toUInt().toULong()) else rounds,
        )

        size <= SMALL_MAX -> SmallIntPermutation(size, rng)

        else -> BoundedIntPermutation(
            size,
            rng,
            if (rounds == 0) defaultRounds(size.toULong()) else rounds,
        )
    }
}

/**
 * Creates an [IntPermutation] using a seed-based [Random] instance.
 *
 * This overload behaves like [intPermutation] with an explicit rng, but
 * derives the random source from [seed]. For a fixed combination of
 * `[size, seed, rounds]` the resulting permutation is deterministic.
 *
 * @param size domain size; see [intPermutation] for semantics and special values.
 * @param seed seed used to construct the underlying [Random].
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return an [IntPermutation] with domain `[0, size)` or the full 32-bit space
 *         when `size == -1`.
 * @see intPermutation
 */
// No @JvmOverloads: a seed-only Java overload would read like longPermutation(size).
fun intPermutation(size: Int = Int.MAX_VALUE, seed: Long, rounds: Int = 0): IntPermutation =
    intPermutation(size, Random(seed), rounds)

/**
 * Creates an [IntPermutation] for values within the given inclusive [range].
 *
 * Internally this constructs a permutation over a domain of length
 * `range.last - range.first + 1`, then wraps it so that [IntPermutation.encode]
 * and [IntPermutation.decode] operate directly on values in [range].
 *
 * The same implementation selection rules and default [rounds] logic as
 * [intPermutation] are used based on the range length. Any non-empty range is
 * accepted; the full `Int` range uses [FullIntPermutation].
 *
 * @param range inclusive range of values to permute.
 * @param rng random source used to derive internal keys and parameters.
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return an [IntPermutation] whose domain is exactly [range].
 * @throws IllegalArgumentException if [range] is empty.
 */
@JvmOverloads
fun intPermutation(range: IntRange, rng: Random = Random.Default, rounds: Int = 0): IntPermutation {
    require(!range.isEmpty()) { "range must be non-empty and increasing: $range" }
    require(rounds >= 0) { "rounds must be >= 0" }
    val lastIndex = range.last.toUInt() - range.first.toUInt()
    val base = when (lastIndex) {
        UInt.MAX_VALUE -> intPermutation(-1, rng, rounds)

        // 2^32 - 1 values: size -1 would select the full domain instead.
        UInt.MAX_VALUE - 1u -> BoundedIntPermutation(
            -1,
            rng,
            if (rounds == 0) defaultRounds(UInt.MAX_VALUE.toULong()) else rounds,
        )

        else -> intPermutation((lastIndex + 1u).toInt(), rng, rounds)
    }
    return base.range(range)
}

/**
 * Creates an [IntPermutation] for the given inclusive [range] using a
 * seed-based [Random] instance.
 *
 * This overload behaves like [intPermutation] with an explicit rng, but
 * derives the random source from [seed].
 *
 * @param range inclusive range of values to permute.
 * @param seed seed used to construct the underlying [Random].
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return an [IntPermutation] whose domain is exactly [range].
 * @see intPermutation
 */
@JvmOverloads
fun intPermutation(range: IntRange, seed: Long, rounds: Int = 0): IntPermutation =
    intPermutation(range, Random(seed), rounds)

/**
 * Creates a [LongPermutation] for a contiguous long domain.
 *
 * The concrete implementation depends on [size]:
 * - `size == -1L` uses [FullLongPermutation] over the full signed 64-bit range.
 * - `size < 0L` (excluding `-1L`) uses [BoundedLongPermutation] with [size] read as unsigned.
 * - `0 <= size <= 4096L` uses [SmallLongPermutation].
 * - `size > 4096L` uses [BoundedLongPermutation] with cycle-walking over a 2^k block.
 *
 * When [rounds] is `0`, a size-dependent default is chosen to balance speed and
 * dispersion. The permutation is reproducible for a given [rng] state.
 *
 * @param size domain size. For non-negative values the domain is `[0, size)`.
 *             Special values: `-1L` selects a full 64-bit permutation, other
 *             negative values select an unsigned-style domain.
 * @param rng random source used to derive internal keys and parameters.
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return a [LongPermutation] with domain `[0, size)` or the full 64-bit space
 *         when `size == -1L`.
 */
@JvmOverloads
fun longPermutation(size: Long = Long.MAX_VALUE, rng: Random = Random.Default, rounds: Int = 0): LongPermutation {
    require(rounds >= 0) { "rounds must be >= 0" }

    return when {
        size == -1L -> FullLongPermutation(
            rng,
            if (rounds == 0) 2 else rounds,
        )

        size < 0L -> BoundedLongPermutation(
            size,
            rng,
            if (rounds == 0) defaultRounds(size.toULong()) else rounds,
        )

        size <= SMALL_MAX -> SmallLongPermutation(size, rng)

        // Former HalfLongPermutation constant, kept so outputs stay stable.
        else -> BoundedLongPermutation(
            size,
            rng,
            if (rounds == 0) defaultRounds(size.toULong()) else rounds,
            0x52A531B54E4EC5CBuL,
        )
    }
}

/**
 * Creates a [LongPermutation] using a seed-based [Random] instance.
 *
 * This overload behaves like [longPermutation] with an explicit rng, but
 * derives the random source from [seed]. For a fixed combination of
 * `[size, seed, rounds]` the resulting permutation is deterministic.
 *
 * @param size domain size; see [longPermutation] for semantics and special values.
 * @param seed seed used to construct the underlying [Random].
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return a [LongPermutation] with domain `[0, size)` or the full 64-bit space
 *         when `size == -1L`.
 * @see longPermutation
 */
fun longPermutation(size: Long = Long.MAX_VALUE, seed: Long, rounds: Int = 0): LongPermutation =
    longPermutation(size, Random(seed), rounds)

/**
 * Creates a [LongPermutation] for values within the given inclusive [range].
 *
 * Internally this constructs a permutation over a domain of length
 * `range.last - range.first + 1`, then wraps it so that [LongPermutation.encode]
 * and [LongPermutation.decode] operate directly on values in [range].
 *
 * The same implementation selection rules and default [rounds] logic as
 * [longPermutation] are used based on the range length. Any non-empty range is
 * accepted; the full `Long` range uses [FullLongPermutation].
 *
 * @param range inclusive range of values to permute.
 * @param rng random source used to derive internal keys and parameters.
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return a [LongPermutation] whose domain is exactly [range].
 * @throws IllegalArgumentException if [range] is empty.
 */
@JvmOverloads
fun longPermutation(range: LongRange, rng: Random = Random.Default, rounds: Int = 0): LongPermutation {
    require(!range.isEmpty()) { "range must be non-empty and increasing: $range" }
    require(rounds >= 0) { "rounds must be >= 0" }
    val lastIndex = range.last.toULong() - range.first.toULong()
    val base = when (lastIndex) {
        ULong.MAX_VALUE -> longPermutation(-1L, rng, rounds)

        // 2^64 - 1 values: size -1 would select the full domain instead.
        ULong.MAX_VALUE - 1uL -> BoundedLongPermutation(
            -1L,
            rng,
            if (rounds == 0) defaultRounds(ULong.MAX_VALUE) else rounds,
        )

        else -> longPermutation((lastIndex + 1uL).toLong(), rng, rounds)
    }
    return base.range(range)
}

/**
 * Creates a [LongPermutation] for the given inclusive [range] using a
 * seed-based [Random] instance.
 *
 * This overload behaves like [longPermutation] with an explicit rng, but
 * derives the random source from [seed].
 *
 * @param range inclusive range of values to permute.
 * @param seed seed used to construct the underlying [Random].
 * @param rounds number of mixing rounds; `0` selects a reasonable default per
 *               implementation.
 * @return a [LongPermutation] whose domain is exactly [range].
 * @see longPermutation
 */
@JvmOverloads
fun longPermutation(range: LongRange, seed: Long, rounds: Int = 0): LongPermutation =
    longPermutation(range, Random(seed), rounds)
