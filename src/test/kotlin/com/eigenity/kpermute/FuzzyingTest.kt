package com.eigenity.kpermute

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertEquals

class FuzzyingTest {

    private val outerIterations = 100
    private val innerIterations = 100
    private val rng = Random(42)

    @Test
    fun roundTripWithStats() {
        repeat(outerIterations) {
            val usize = rng.nextUInt().coerceAtLeast(1u)
            val rounds = rng.nextInt(2, 8)
            val seed = rng.nextLong()
            val perm = intPermutation(usize, Random(seed), rounds)

            var sum = 0.0
            var sumSq = 0.0
            var min = UInt.MAX_VALUE
            var max = UInt.MIN_VALUE

            repeat(innerIterations) {
                val x = rng.nextUInt(usize).toInt()
                val y = perm.encode(x).toUInt()
                val z = perm.decode(y.toInt()).toUInt()
                assertEquals(
                    x.toUInt(), z,
                    "round-trip failed: usize=$usize " +
                            "rounds=$rounds seed=$seed " +
                            "x=$x y=$y z=$z"
                )

                val yf = y.toDouble()
                sum += yf
                sumSq += yf * yf
                if (y < min) min = y
                if (y > max) max = y
            }

            // compute mean and stddev safely
            val mean = sum / innerIterations
            val variance = (sumSq / innerIterations) - mean * mean
            val stddev = if (variance > 0.0) sqrt(variance) else 0.0

            val expectedMean = (usize.toDouble() - 1.0) / 2.0
            val expectedStd = usize.toDouble() / sqrt(12.0) // uniform [0,n)
            val meanDev = abs(mean - expectedMean) / expectedMean
            val stdDev = abs(stddev - expectedStd) / expectedStd

            if (meanDev > 0.1 || stdDev > 0.1 ||
                min.toLong() < 0 || max >= usize
            ) {
                println(
                    "⚠️ Stats anomaly: perm=${perm::class.simpleName} usize=$usize " +
                            "rounds=$rounds seed=$seed meanDev=${"%.3f".format(meanDev)} " +
                            "stdDev=${"%.3f".format(stdDev)} min=$min max=$max"
                )
            }
        }
    }
}
