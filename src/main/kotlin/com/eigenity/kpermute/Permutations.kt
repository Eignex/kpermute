@file:JvmName("Permutations")

package com.eigenity.kpermute

import kotlin.random.Random

/**
 * Provides an `[IntPermutation]` instance, a fast repeatable integer permutation
 * for shuffling lists and data masking using a cycle-walking hash algorithm.
 *
 * Input Variables:
 *
 * [size] The size of the integer domain to permute. This also decides which
 * implementation is used, normally [HalfIntPermutation] but for negative values
 * [UIntPermutation] is used and for -1 [FullIntPermutation] is used.
 *
 * [rng] A random number generator used to initialize keys and parameters.
 *
 * [rounds] The number of rounds in the permutation algorithm. Use 8 for high
 * dispersion requirements and minimum 3 for low requirements.
 */
@JvmOverloads
fun intPermutation(
    size: Int = Int.MAX_VALUE,
    rng: Random = Random.Default,
    rounds: Int = 0
): IntPermutation {
    require(rounds >= 0) { "rounds must be >= 0" }

    // Determine default rounds when not provided.
    fun defaultRoundsForHalf(n: Int): Int = when {
        n <= 1 shl 10 -> 3          // up to 1 K
        n <= 1 shl 20 -> 4          // up to 1 M
        else -> 6                   // larger domains
    }

    fun defaultRoundsForUInt(n: Int): Int = when {
        n <= 1 shl 16 -> 3
        n <= 1 shl 24 -> 4
        else -> 5
    }

    return when {
        size == -1 ->
            FullIntPermutation(
                rng,
                if (rounds == 0) 2 else rounds // FullInt needs few rounds
            )

        size < 0 ->
            UIntPermutation(
                size,
                rng,
                if (rounds == 0) defaultRoundsForUInt(size) else rounds
            )

        size <= 16 -> ArrayIntPermutation(size, rng)

        else ->
            HalfIntPermutation(
                size,
                rng,
                if (rounds == 0) defaultRoundsForHalf(size) else rounds
            )
    }
}

/**
 * Provides an `[IntPermutation]` instance, a fast repeatable integer permutation
 * for shuffling lists and data masking using a cycle-walking hash algorithm.
 *
 * Input Variables:
 *
 * [size] The size of the integer domain to permute. This also decides which
 * implementation is used, normally [HalfIntPermutation] but for negative values
 * [UIntPermutation] is used and for -1 [FullIntPermutation] is used.
 *
 * [seed] Used as seed to [Random] to initialize keys and parameters.
 *
 * [rounds] The number of rounds in the permutation algorithm. Use 8 for high
 * dispersion requirements and minimum 3 for low requirements.
 */
@JvmOverloads
fun intPermutation(
    size: Int = Int.MAX_VALUE,
    seed: Long,
    rounds: Int = 0
): IntPermutation = intPermutation(size, Random(seed), rounds)

/**
 * Provides an `[IntPermutation]` instance for values within the given [range].
 * The implementation and parameters follow the same rules as the size-based
 * factories, normally using [HalfIntPermutation] for most domains.
 *
 * Input Variables:
 *
 * [range] The inclusive integer range to permute. Its length determines the
 * domain size used internally.
 *
 * [rng]  A random number generator used to initialize keys and parameters.
 *
 * [rounds] The number of permutation rounds. Use 8 for high dispersion and at
 * least 3 for low requirements.
 *
 * The resulting permutation encodes and decodes values directly in [range].
 */
@JvmOverloads
fun intPermutation(
    range: IntRange,
    rng: Random = Random.Default,
    rounds: Int = 0
): IntPermutation {
    val nLong = range.last.toLong() - range.first.toLong() + 1L
    require(nLong > 0L) {
        "range must be non-empty and increasing: $range"
    }
    require(nLong <= Int.MAX_VALUE.toLong()) {
        "range size $nLong exceeds Int.MAX_VALUE"
    }
    return intPermutation(nLong.toInt(), rng, rounds).range(range)
}

/**
 * Provides an `[IntPermutation]` instance for values within the given [range].
 * The implementation and parameters follow the same rules as the size-based
 * factories, normally using [HalfIntPermutation] for most domains.
 *
 * Input Variables:
 *
 * [range] The inclusive integer range to permute. Its length determines the
 * domain size used internally.
 *
 * [seed] Used as seed to [Random] to initialize keys and parameters.
 *
 * [rounds] The number of permutation rounds. Use 8 for high dispersion and at
 * least 3 for low requirements.
 *
 * The resulting permutation encodes and decodes values directly in [range].
 */
fun intPermutation(
    range: IntRange,
    seed: Long,
    rounds: Int = 0
): IntPermutation = intPermutation(range, Random(seed), rounds)

/**
 * Provides a `[LongPermutation]` instance, a fast repeatable integer permutation
 * for shuffling lists and data masking using a cycle-walking hash algorithm.
 *
 * Input Variables:
 *
 * [size] The size of the integer domain to permute. This also decides which
 * implementation is used, normally `HalfLongPermutation` but for negative values
 * `ULongPermutation` is used and for -1 `FullLongPermutation` is used.
 *
 * [rng] A random number generator used to initialize keys and parameters.
 *
 * [rounds] The number of rounds in the permutation algorithm. Use 8 for high
 * dispersion requirements and minimum 3 for low requirements.
 */
@JvmOverloads
fun longPermutation(
    size: Long = Long.MAX_VALUE,
    rng: Random = Random.Default,
    rounds: Int = 0
): LongPermutation {
    require(rounds >= 0) { "rounds must be >= 0" }

    // TODO: implement LongPermutation variants (HalfLongPermutation, ULongPermutation, FullLongPermutation)
    // and dispatch similarly to intPermutation. For now this is a stub.
    throw NotImplementedError("longPermutation(size, rng, rounds) is not implemented yet")
}

/**
 * Provides a `[LongPermutation]` instance, a fast repeatable integer permutation
 * for shuffling lists and data masking using a cycle-walking hash algorithm.
 *
 * Input Variables:
 *
 * [size] The size of the integer domain to permute. This also decides which
 * implementation is used, normally `HalfLongPermutation` but for negative values
 * `ULongPermutation` is used and for -1 `FullLongPermutation` is used.
 *
 * [seed] Used as seed to [Random] to initialize keys and parameters.
 *
 * [rounds] The number of rounds in the permutation algorithm. Use 8 for high
 * dispersion requirements and minimum 3 for low requirements.
 */
fun longPermutation(
    size: Long = Long.MAX_VALUE,
    seed: Long,
    rounds: Int = 0
): LongPermutation = longPermutation(size, Random(seed), rounds)

/**
 * Provides a `[LongPermutation]` instance for values within the given [range].
 * The implementation and parameters follow the same rules as the size-based
 * factories, normally using `HalfLongPermutation` for most domains.
 *
 * Input Variables:
 *
 * [range] The inclusive integer range to permute. Its length determines the
 * domain size used internally.
 *
 * [rng]  A random number generator used to initialize keys and parameters.
 *
 * [rounds] The number of permutation rounds. Use 8 for high dispersion and at
 * least 3 for low requirements.
 *
 * The resulting permutation encodes and decodes values directly in [range].
 */
@JvmOverloads
fun longPermutation(
    range: LongRange,
    rng: Random = Random.Default,
    rounds: Int = 0
): LongPermutation {
    val nULong = range.last.toULong() - range.first.toULong() + 1uL
    require(nULong > 0uL) {
        "range must be non-empty and increasing: $range"
    }
    require(nULong <= Long.MAX_VALUE.toULong()) {
        "range size $nULong exceeds Long.MAX_VALUE"
    }
    return longPermutation(nULong.toLong(), rng, rounds).range(range)
}

/**
 * Provides a `[LongPermutation]` instance for values within the given [range].
 * The implementation and parameters follow the same rules as the size-based
 * factories, normally using `HalfLongPermutation` for most domains.
 *
 * Input Variables:
 *
 * [range] The inclusive integer range to permute. Its length determines the
 * domain size used internally.
 *
 * [seed] Used as seed to [Random] to initialize keys and parameters.
 *
 * [rounds] The number of permutation rounds. Use 8 for high dispersion and at
 * least 3 for low requirements.
 *
 * The resulting permutation encodes and decodes values directly in [range].
 */
@JvmOverloads
fun longPermutation(
    range: LongRange,
    seed: Long,
    rounds: Int = 0
): LongPermutation = longPermutation(range, Random(seed), rounds)
