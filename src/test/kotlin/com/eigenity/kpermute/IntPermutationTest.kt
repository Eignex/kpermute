package com.eigenity.kpermute

class IntPermutationTest {
    @Test
    fun emptyPermutation() {
        assertFalse(IntPermutation(0, Random).iterator().hasNext())
    }

    @Test
    fun onePermutation() {
        val p = CyclingHashIntPermutation(1, Random)
        assertTrue(p.iterator().hasNext())
        assertContentEquals(listOf(0), p.toList())
        assertEquals(0, p.encode(0))
    }

    @Test
    fun sameTwice() {
        val p = CyclingHashIntPermutation(100, Random)
        for (i in 0 until 100) {
            assertEquals(p.encode(i), p.encode(i))
        }
    }

    @Test
    fun exhaustiveEvenPermutation() {
        val p = CyclingHashIntPermutation(2.0.pow(4).toInt(), Random)
        val set = HashSet<Int>()
        for (i in 0 until 2.0.pow(4).toInt()) {
            set.add(p.encode(i))
        }
        assertEquals(2.0.pow(4).toInt(), set.size)
        for (i in 0 until 2.0.pow(4).toInt()) {
            assertTrue(set.contains(i))
        }
    }

    @Test
    fun exhaustiveOddPermutation() {
        val p = CyclingHashIntPermutation(1001, Random)
        val set = HashSet<Int>()
        for (i in 0 until 1001) {
            set.add(p.encode(i))
        }
        assertEquals(1001, set.size)
    }

    @Test
    fun iterator() {
        val l = CyclingHashIntPermutation(4, Random).iterator().asSequence().toList()
        assertEquals(4, l.size)
        assertEquals(4, l.toSet().size)
    }

    @Test
    fun permutationSize() {
        for (i in 0..500)
            assertEquals(i, CyclingHashIntPermutation(i, Random).asSequence().count())
    }

    @Test
    fun allValuesFirst() {
        for (i in 1..100) {
            while (CyclingHashIntPermutation(i, Random).first() != i - 1) {
            }
        }
    }
}
