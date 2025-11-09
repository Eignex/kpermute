package com.eigenity.kpermute

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.*
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.random.nextULong
import kotlin.test.assertEquals

class FuzzyingTest {

    private val outerIterations = 1000
    private val innerIterations = 1_000
    private val rng = Random(42)

    private fun choseIntPerm(): Triple<IntPermutation, Long, Int> {
        var size: Int
        do {
            size = when (rng.nextInt(4)) {
                0 -> rng.nextInt(17)        // ArrayIntPermutation
                1 -> rng.nextInt()          // HalfIntPermutation (size > 0)
                2 -> rng.nextUInt().toInt() // UIntPermutation (size < 0, except 0)
                else -> -1                  // FullIntPermutation sentinel
            }
        } while (size == 0)
        val rounds = rng.nextInt(2, 8)
        val seed = rng.nextLong()
        return Triple(intPermutation(size, Random(seed), rounds), seed, rounds)
    }

    private fun choseLongPerm(): Triple<LongPermutation, Long, Int> {
        var size: Long
        do {
            size = when (rng.nextInt(4)) {
                0 -> rng.nextLong(17L)        // ArrayLongPermutation
                1 -> rng.nextLong()           // HalfLongPermutation (size > 0)
                2 -> rng.nextULong().toLong() // ULongPermutation (size < 0, except 0)
                else -> -1L                   // FullLongPermutation sentinel
            }
        } while (size == 0L)
        val rounds = rng.nextInt(2, 8)
        val seed = rng.nextLong()
        return Triple(longPermutation(size, Random(seed), rounds), seed, rounds)
    }

    // Normal CDF approximation (Abramowitz–Stegun 7.1.26)
    private fun normalCdf(x: Double): Double {
        val t = 1.0 / (1.0 + 0.2316419 * abs(x))
        val d = 0.3989423 * exp(-x * x / 2.0)
        val prob = d * t * (
                0.3193815 +
                        t * (-0.3565638 +
                        t * (1.781478 +
                        t * (-1.821256 +
                        t * 1.330274)))
                )
        return if (x > 0.0) 1.0 - prob else prob
    }

    private fun meanTPValue(
        mean: Double,
        expectedMean: Double,
        variance: Double,
        n: Int
    ): Double {
        if (variance <= 0.0 || n <= 1) return 1.0
        val s = sqrt(variance)
        val t = (mean - expectedMean) / (s / sqrt(n.toDouble()))
        val z = abs(t)
        val p = 2.0 * (1.0 - normalCdf(z))
        return p.coerceIn(0.0, 1.0)
    }

    private data class Stats(
        var mean: Double = 0.0,
        var m2: Double = 0.0,
        var count: Int = 0
    )

    private fun Stats.update(yf: Double) {
        count++
        val delta = yf - mean
        mean += delta / count
        m2 += delta * (yf - mean)
    }

    private fun Stats.pValue(expectedMean: Double): Double {
        val variance = if (count > 1) m2 / (count - 1) else 0.0
        return meanTPValue(mean, expectedMean, variance, count)
    }

    private fun runIntFuzzIterationUsingEncode(
        perm: IntPermutation,
        seed: Long,
        rounds: Int
    ): Double {
        val stats = Stats()
        val size = perm.size
        val usize = size.toUInt()

        repeat(innerIterations) {
            val x = if (size > 0) rng.nextInt(size)
            else rng.nextUInt(0u, usize).toInt()

            val y = perm.encode(x)
            val z = perm.decode(y)
            assertEquals(
                x, z, "round-trip failed: " +
                        "size=$size rounds=$rounds seed=$seed x=$x y=$y z=$z"
            )

            val yu = y.toUInt()
            val yf = yu.toDouble()
            stats.update(yf)

            assertTrue(yu in 0u..<usize) {
                "support anomaly: perm=${perm::class.simpleName} size=$size " +
                        "rounds=$rounds seed=$seed"
            }
        }

        val expectedMean = (usize.toDouble() - 1.0) / 2.0
        return stats.pValue(expectedMean)
    }

    private fun runIntFuzzIterationUsingIterator(
        perm: IntPermutation,
        seed: Long,
        rounds: Int
    ): Double {
        val stats = Stats()
        val size = perm.size
        val usize = size.toUInt()

        val offset: Int =
            if (size > 0) rng.nextInt(size)
            else rng.nextInt()

        var i = offset
        val it = perm.iterator(offset)

        var taken = 0
        while (taken < innerIterations && it.hasNext()) {
            val y = it.nextInt()
            val x = perm.decode(y)

            assertEquals(
                y, perm.encode(i),
                "iterator mismatch: " +
                        "size=$size rounds=$rounds seed=$seed offset=$offset i=$i y=$y"
            )
            assertEquals(
                i, x,
                "decode(encode(i)) mismatch via iterator: " +
                        "size=$size rounds=$rounds seed=$seed offset=$offset i=$i x=$x"
            )

            val yu = y.toUInt()
            val yf = yu.toDouble()
            stats.update(yf)

            assertTrue(yu in 0u..<usize) {
                "support anomaly via iterator: perm=${perm::class.simpleName} size=$size " +
                        "rounds=$rounds seed=$seed"
            }

            i++
            taken++
        }

        val expectedMean = (usize.toDouble() - 1.0) / 2.0
        return stats.pValue(expectedMean)
    }

    private fun runIntFuzz(useIterator: Boolean) {
        val pValuesByClass = mapOf<String, MutableList<Double>>(
            ArrayIntPermutation::class.simpleName!! to ArrayList(),
            HalfIntPermutation::class.simpleName!! to ArrayList(),
            UIntPermutation::class.simpleName!! to ArrayList(),
            FullIntPermutation::class.simpleName!! to ArrayList(),
        )

        repeat(outerIterations) { outerIdx ->
            val (perm, seed, rounds) = choseIntPerm()
            val className = perm::class.simpleName!!

            val pValue =
                if (useIterator) runIntFuzzIterationUsingIterator(perm, seed, rounds)
                else runIntFuzzIterationUsingEncode(perm, seed, rounds)

            pValuesByClass[className]!!.add(pValue)

            if (outerIdx % 1_000 == 0 && outerIdx > 0) {
                val s = pValuesByClass.map {
                    "${it.key} = ${String.format("%.3f", it.value.average())}"
                }.joinToString(", ")
                println((if (useIterator) "[INT/ITER] " else "[INT/ENC] ") + s)
            }
        }
    }

    private fun runLongFuzzIterationUsingEncode(
        perm: LongPermutation,
        seed: Long,
        rounds: Int
    ): Double {
        val stats = Stats()
        val size = perm.size
        val usize = size.toULong()

        repeat(innerIterations) {
            val x: Long =
                if (size > 0L) {
                    rng.nextLong(size)
                } else {
                    rng.nextULong(0uL, usize).toLong()
                }

            val y = perm.encode(x)
            val z = perm.decode(y)

            assertEquals(
                x, z, "round-trip failed (Long): " +
                        "size=$size rounds=$rounds seed=$seed x=$x y=$y z=$z perm=${perm::class.simpleName}"
            )

            val yu = y.toULong()
            val yf = yu.toDouble()
            stats.update(yf)

            assertTrue(yu in 0uL..<usize) {
                "support anomaly (Long): perm=${perm::class.simpleName} size=$size " +
                        "rounds=$rounds seed=$seed"
            }
        }

        val expectedMean = (usize.toDouble() - 1.0) / 2.0
        return stats.pValue(expectedMean)
    }

    private fun runLongFuzzIterationUsingIterator(
        perm: LongPermutation,
        seed: Long,
        rounds: Int
    ): Double {
        val stats = Stats()
        val size = perm.size
        val usize = size.toULong()

        val offset: Long =
            if (size > 0L) rng.nextLong(size)
            else rng.nextLong()

        var i = offset
        val it = perm.iterator(offset)

        var taken = 0
        while (taken < innerIterations && it.hasNext()) {
            val y = it.nextLong()
            val x = perm.decode(y)

            assertEquals(
                y, perm.encode(i),
                "iterator mismatch (Long): " +
                        "size=$size rounds=$rounds seed=$seed offset=$offset i=$i y=$y perm=${perm::class.simpleName}"
            )
            assertEquals(
                i, x,
                "decode(encode(i)) mismatch via iterator (Long): " +
                        "size=$size rounds=$rounds seed=$seed offset=$offset i=$i x=$x perm=${perm::class.simpleName}"
            )

            val yu = y.toULong()
            val yf = yu.toDouble()
            stats.update(yf)

            assertTrue(yu in 0uL..<usize) {
                "support anomaly via iterator (Long): perm=${perm::class.simpleName} size=$size " +
                        "rounds=$rounds seed=$seed"
            }

            i++
            taken++
        }

        val expectedMean = (usize.toDouble() - 1.0) / 2.0
        return stats.pValue(expectedMean)
    }

    private fun runLongFuzz(useIterator: Boolean) {
        val pValuesByClass = mapOf<String, MutableList<Double>>(
            ArrayLongPermutation::class.simpleName!! to ArrayList(),
            HalfLongPermutation::class.simpleName!! to ArrayList(),
            ULongPermutation::class.simpleName!! to ArrayList(),
            FullLongPermutation::class.simpleName!! to ArrayList(),
        )

        repeat(outerIterations) { outerIdx ->
            val (perm, seed, rounds) = choseLongPerm()
            val className = perm::class.simpleName!!

            val pValue =
                if (useIterator) runLongFuzzIterationUsingIterator(perm, seed, rounds)
                else runLongFuzzIterationUsingEncode(perm, seed, rounds)

            pValuesByClass[className]!!.add(pValue)

            if (outerIdx % 1_000 == 0 && outerIdx > 0) {
                val s = pValuesByClass.map {
                    "${it.key} = ${String.format("%.3f", it.value.average())}"
                }.joinToString(", ")
                println((if (useIterator) "[LONG/ITER] " else "[LONG/ENC] ") + s)
            }
        }
    }

    @Test
    fun roundTripWithStatsInt() {
        runIntFuzz(useIterator = false)
    }

    @Test
    fun roundTripWithStatsIntIterator() {
        runIntFuzz(useIterator = true)
    }

    @Test
    fun roundTripWithStatsLong() {
        runLongFuzz(useIterator = false)
    }

    @Test
    fun roundTripWithStatsLongIterator() {
        runLongFuzz(useIterator = true)
    }
}
