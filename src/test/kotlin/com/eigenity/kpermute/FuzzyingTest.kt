package com.eigenity.kpermute

import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertEquals

class FuzzyingTest {

    val outerIterations: Int = 100
    val innerIterations: Int = 100
    val rng: Random = Random(42)

    @Test
    fun roundTrip() {

        repeat(outerIterations) {
            val usize = rng.nextUInt(1u, UInt.MAX_VALUE)
            val rounds = rng.nextInt(2, 8)
            val seed = rng.nextLong()
            val perm = intPermutation(usize, Random(seed), rounds)

            repeat(innerIterations) {
                val x = rng.nextUInt(usize).toInt()
                val y = perm.encode(x)
                val z = perm.decode(y)
                assertEquals(
                    x, z,
                    "Failed round-trip: usize=$usize rounds=$rounds seed=$seed x=$x y=$y z=$z"
                )
            }
        }
    }
}
