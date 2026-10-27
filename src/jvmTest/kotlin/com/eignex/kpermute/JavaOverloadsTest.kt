package com.eignex.kpermute

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JavaOverloadsTest {
    private val methods = Class.forName("com.eignex.kpermute.Permutations").methods
        .map { m -> m.name to m.parameterTypes.toList() }
        .toSet()

    @Test
    fun seedOverloadsDoNotShadowSizeOverloads() {
        // From Java, intPermutation(5L) must not compile as "seed 5".
        assertFalse("intPermutation" to listOf(Long::class.javaPrimitiveType) in methods)
        assertTrue("longPermutation" to listOf(Long::class.javaPrimitiveType) in methods)
    }

    @Test
    fun rangeSeedOverloadsMatchBetweenIntAndLong() {
        val long = Long::class.javaPrimitiveType
        assertTrue("intPermutation" to listOf(IntRange::class.java, long) in methods)
        assertTrue("longPermutation" to listOf(LongRange::class.java, long) in methods)
    }
}
