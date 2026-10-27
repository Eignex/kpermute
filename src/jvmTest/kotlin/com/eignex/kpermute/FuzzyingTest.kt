package com.eignex.kpermute

import java.util.Locale
import kotlin.math.*
import kotlin.random.*
import kotlin.test.*

class FuzzyingTest {

    private val outerIterations = 1000
    private val innerIterations = 1_000
    private val rng = Random(42)

    // Int and Long permutations seen through unsigned 64-bit values.
    private class FuzzCase(
        val name: String,
        val size: ULong,
        val seed: Long,
        val rounds: Int,
        val encode: (ULong) -> ULong,
        val decode: (ULong) -> ULong,
        val iterator: (ULong) -> Iterator<ULong>,
        val nextValue: () -> ULong,
        val nextOffset: () -> ULong,
    ) {
        override fun toString() = "perm=$name size=$size rounds=$rounds seed=$seed"
    }

    private fun choseIntPerm(): FuzzCase {
        var size: Int
        do {
            size = when (rng.nextInt(4)) {
                0 -> rng.nextInt(17)
                1 -> rng.nextInt()
                2 -> rng.nextUInt().toInt()
                else -> -1
            }
        } while (size == 0)
        val rounds = rng.nextInt(2, 8)
        val seed = rng.nextLong()
        val perm = intPermutation(size, Random(seed), rounds)
        val usize = size.toUInt()
        return FuzzCase(
            name = perm::class.simpleName!!,
            size = usize.toULong(),
            seed = seed,
            rounds = rounds,
            encode = { perm.encode(it.toInt()).toUInt().toULong() },
            decode = { perm.decode(it.toInt()).toUInt().toULong() },
            iterator = { offset ->
                perm.iterator(offset.toInt()).asSequence().map { it.toUInt().toULong() }.iterator()
            },
            nextValue = {
                val x = if (size > 0) rng.nextInt(size) else rng.nextUInt(0u, usize).toInt()
                x.toUInt().toULong()
            },
            nextOffset = {
                val offset = when {
                    size > 0 -> rng.nextInt(size)
                    size == -1 -> rng.nextInt()
                    else -> rng.nextUInt(usize).toInt()
                }
                offset.toUInt().toULong()
            },
        )
    }

    private fun choseLongPerm(): FuzzCase {
        var size: Long
        do {
            size = when (rng.nextInt(4)) {
                0 -> rng.nextLong(17L)
                1 -> rng.nextLong()
                2 -> rng.nextULong().toLong()
                else -> -1L
            }
        } while (size == 0L)
        val rounds = rng.nextInt(2, 8)
        val seed = rng.nextLong()
        val perm = longPermutation(size, Random(seed), rounds)
        val usize = size.toULong()
        return FuzzCase(
            name = perm::class.simpleName!!,
            size = usize,
            seed = seed,
            rounds = rounds,
            encode = { perm.encode(it.toLong()).toULong() },
            decode = { perm.decode(it.toLong()).toULong() },
            iterator = { offset ->
                perm.iterator(offset.toLong()).asSequence().map { it.toULong() }.iterator()
            },
            nextValue = { if (size > 0L) rng.nextLong(size).toULong() else rng.nextULong(0uL, usize) },
            nextOffset = {
                when {
                    size > 0L -> rng.nextLong(size).toULong()
                    size == -1L -> rng.nextLong().toULong()
                    else -> rng.nextULong(usize)
                }
            },
        )
    }

    // Abramowitz–Stegun 7.1.26
    private fun normalCdf(x: Double): Double {
        val t = 1.0 / (1.0 + 0.2316419 * abs(x))
        val d = 0.3989423 * exp(-x * x / 2.0)
        val prob = d * t * (
            0.3193815 +
                t * (
                    -0.3565638 +
                        t * (
                            1.781478 +
                                t * (
                                    -1.821256 +
                                        t * 1.330274
                                    )
                            )
                    )
            )
        return if (x > 0.0) 1.0 - prob else prob
    }

    private fun meanTPValue(mean: Double, expectedMean: Double, variance: Double, n: Int): Double {
        if (variance <= 0.0 || n <= 1) return 1.0
        val s = sqrt(variance)
        val t = (mean - expectedMean) / (s / sqrt(n.toDouble()))
        val z = abs(t)
        val p = 2.0 * (1.0 - normalCdf(z))
        return p.coerceIn(0.0, 1.0)
    }

    private data class Stats(var mean: Double = 0.0, var m2: Double = 0.0, var count: Int = 0)

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

    private fun runFuzzIterationUsingEncode(c: FuzzCase): Double {
        val stats = Stats()

        repeat(innerIterations) {
            val x = c.nextValue()
            val y = c.encode(x)
            val z = c.decode(y)
            assertEquals(x, z, "round-trip failed: $c x=$x y=$y z=$z")

            stats.update(y.toDouble())
            assertTrue(y < c.size, "support anomaly: $c")
        }

        return stats.pValue((c.size.toDouble() - 1.0) / 2.0)
    }

    private fun runFuzzIterationUsingIterator(c: FuzzCase): Double {
        val stats = Stats()

        val offset = c.nextOffset()
        var i = offset
        val it = c.iterator(offset)

        var taken = 0
        while (taken < innerIterations && it.hasNext()) {
            val y = it.next()
            val x = c.decode(y)

            assertEquals(y, c.encode(i), "iterator mismatch: $c offset=$offset i=$i y=$y")
            assertEquals(i, x, "decode(encode(i)) mismatch via iterator: $c offset=$offset i=$i x=$x")

            stats.update(y.toDouble())
            assertTrue(y < c.size, "support anomaly via iterator: $c")

            i++
            taken++
        }

        return stats.pValue((c.size.toDouble() - 1.0) / 2.0)
    }

    private fun runFuzz(label: String, classNames: List<String>, chose: () -> FuzzCase, useIterator: Boolean) {
        val pValuesByClass = classNames.associateWith { ArrayList<Double>() }

        repeat(outerIterations) { outerIdx ->
            val c = chose()
            val pValue = if (useIterator) runFuzzIterationUsingIterator(c) else runFuzzIterationUsingEncode(c)
            pValuesByClass.getValue(c.name).add(pValue)

            if (outerIdx % 1_000 == 0 && outerIdx > 0) {
                val s = pValuesByClass.map {
                    "${it.key} = ${String.format(Locale.ENGLISH, "%.3f", it.value.average())}"
                }.joinToString(", ")
                println("[$label/${if (useIterator) "ITER" else "ENC"}] $s")
            }
        }
    }

    private fun runIntFuzz(useIterator: Boolean) = runFuzz(
        "INT",
        listOf(
            SmallIntPermutation::class.simpleName!!,
            BoundedIntPermutation::class.simpleName!!,
            FullIntPermutation::class.simpleName!!,
        ),
        ::choseIntPerm,
        useIterator,
    )

    private fun runLongFuzz(useIterator: Boolean) = runFuzz(
        "LONG",
        listOf(
            SmallLongPermutation::class.simpleName!!,
            BoundedLongPermutation::class.simpleName!!,
            FullLongPermutation::class.simpleName!!,
        ),
        ::choseLongPerm,
        useIterator,
    )

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
