package com.eignex.kpermute

import kotlin.random.Random
import kotlin.test.*

class ArrayLongPermutationTest {
    @Test
    fun rejectsSizesThatCannotBeRepresentedAsArrayLengths() {
        for (size in listOf(-1L, Int.MAX_VALUE.toLong() + 1L, 1L shl 32, (1L shl 32) + 1L, Long.MAX_VALUE)) {
            assertFailsWith<IllegalArgumentException> { ArrayLongPermutation(size, Random(1)) }
        }
    }

    @Test
    fun supportedSizesRemainBijective() {
        for (size in listOf(0L, 1L, 17L)) {
            CommonAssertsLong.assertBijectionOverDomain(ArrayLongPermutation(size, Random(1)), size)
        }
    }
}
