package com.eigenity.kpermute

import kotlin.random.Random

interface IntPermutation : Iterable<Int> {

    /**
     * Domain of the permutation.
     */
    val size: Int


    /** Fast path. No range checks. Precondition: if size >= 0 then arg ∈ [0, size). */
    fun encodeUnchecked(value: Int): Int

    /** Fast path. No range checks. Precondition: if size >= 0 then arg ∈ [0, size). */
    fun decodeUnchecked(encoded: Int): Int

    /**
     * Encode an integer in [0, [size]) into its permuted value.
     */
    fun encode(value: Int): Int {
        if (size >= 0) require(value in 0 until size) {
            "value $value out of range [0, $size)"
        }
        return encodeUnchecked(value)
    }

    /**
     *  Decode a previously encoded integer back to its original value.
     */
    fun decode(encoded: Int): Int {
        if (size >= 0) require(encoded in 0 until size) {
            "encoded $encoded out of range [0, $size)"
        }
        return decodeUnchecked(encoded)
    }

    /**
     * Iterator that yields encoded values for [0, size).
     */
    override fun iterator(): IntIterator = iterator(0)

    /**
     * Iterator that yields encoded values for [offset, size).
     */
    fun iterator(offset: Int): IntIterator
}

/**
 * Returns a new list whose elements are permuted by [perm].
 * The original list is not modified.
 *
 * Example:
 * ```
 * val perm = intPermutation(5, seed = 42)
 * val shuffled = listOf("a","b","c","d","e").permutedBy(perm)
 * ```
 */
fun <T> List<T>.permutedBy(perm: IntPermutation): List<T> {
    val n = size
    require(perm.size >= 0 && perm.size == n) {
        "Permutation domain (${perm.size}) must equal list size ($n)"
    }
    return List(n) { index -> this[perm.decode(index)] }
}

/**
 * Applies the inverse of [perm] to reorder this list back to original order.
 */
fun <T> List<T>.unpermutedBy(perm: IntPermutation): List<T> {
    val n = size
    require(perm.size >= 0 && perm.size == n) {
        "Permutation domain (${perm.size}) must equal list size ($n)"
    }
    return List(n) { index -> this[perm.encode(index)] }
}

/**
 * Returns a new list whose elements are permuted by a permutation initialized
 * by [rng] and [rounds].
 * The original list is not modified.
 *
 * Example:
 * ```
 * val perm = intPermutation(5, seed = 42)
 * val shuffled = listOf("a","b","c","d","e").permutedBy(perm)
 * ```
 */
fun <T> List<T>.permute(rng: Random, rounds: Int = 0) =
    permutedBy(intPermutation(size, rng, rounds))
