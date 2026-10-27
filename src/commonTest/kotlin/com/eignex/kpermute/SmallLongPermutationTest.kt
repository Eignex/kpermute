package com.eignex.kpermute

import kotlin.random.Random
import kotlin.test.*

class SmallLongPermutationTest {
    @Test
    fun rejectsSizesThatCannotBeRepresentedAsArrayLengths() {
        for (size in listOf(-1L, Int.MAX_VALUE.toLong() + 1L, 1L shl 32, (1L shl 32) + 1L, Long.MAX_VALUE)) {
            assertFailsWith<IllegalArgumentException> { SmallLongPermutation(size, Random(1)) }
        }
    }

    @Test
    fun supportedSizesRemainBijective() {
        for (size in listOf(0L, 1L, 17L)) {
            CommonAssertsLong.assertBijectionOverDomain(SmallLongPermutation(size, Random(1)), size)
        }
    }

    @Test
    fun rejectsInvalidSizeAndOffsets() {
        assertFailsWith<IllegalArgumentException> { SmallLongPermutation(-1L, Random(1)) }
        val p = SmallLongPermutation(10L, Random(1))
        for (offset in listOf(-1L, 11L, (1L shl 32) + 1)) {
            assertFailsWith<IllegalArgumentException>("offset=$offset") { p.iterator(offset) }
        }
        assertFalse(p.iterator(10L).hasNext())
    }
}
