# KPermute

[![Maven Central](https://img.shields.io/maven-central/v/com.eigenity/kpermute.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.eigenity/kpermute/1.0.0)

> **Fast, deterministic integer permutation library for Kotlin.**  
> Shuffle or obfuscate large integer domains efficiently using bijective,
> reversible hash mixing.
>
> ⚠️ **Not intended for cryptographic use.**  
> Suitable for data masking, sampling, and reproducible pseudo-randomization
> where reversibility is required.

---

## Overview

`kpermute` generates stable, deterministic **pseudo-random permutations** over
integer ranges.  
Each seed defines a unique bijection between `[0, size)`.  
The result acts like a **keyed shuffle**, repeatable, memory-efficient, and
invertible.

Typical use cases:

- Repeatable pseudo-random shuffles
- Obfuscating integer IDs (e.g., user IDs, session numbers)
- Collision-free sampling or load balancing
- Data masking for non-sensitive identifiers

---

## Installation

Add the dependency from Maven Central:

```kotlin
implementation("com.eigenity:kpermute:1.0.0")
```

## Example Usage

```kotlin
fun main() {
// Example 1: Obfuscate numeric IDs reproducibly
// The range is 0-Int.MAX_VALUE so no negative values as in/out
    val intIdPerm = intPermutation(seed = 1L)
    val intId = 49102490
    val intIdEncoded = intIdPerm.encode(intId)
    println("encoded: $intIdEncoded (always prints 1394484051)")


    // Example 2: Obfuscate UUID-v7 IDs
    // hiding timestamp and UUID-version
    val uuidPerm = longPermutation(-1, seed = 1L)
    val uuid = Uuid.parse("019a67e6-02a0-7646-a5cd-ddcb69d3b71c")
    val encoded = uuid.toLongs { l1, l2 ->
        Uuid.fromLongs(
            uuidPerm.encode(l1),
            uuidPerm.encode(l2 xor 5955220737039975883L)
        )
    }
    println("encoded: $encoded")


    // Example 3: Shuffle a large list
    val largeList = object : AbstractList<Int>() {
        override val size: Int get() = Int.MAX_VALUE
        override fun get(index: Int) = index
    }
    val perm = intPermutation(largeList.size)
    val shuffled = largeList.permuted(perm) // does not load anything
    println("shuffled: ${shuffled.take(20)}}")
    val unshuffled = shuffled.unpermuted(perm)
    println("unshuffled: ${unshuffled.take(20)}")


    // Example 4: Custom range permutation and negative values
    val rangePerm = intPermutation(-100..199)
    println("encode(-50): ${rangePerm.encode(-50)}")
    println("decode(...): ${rangePerm.decode(rangePerm.encode(-50))}")

    // Example 5: Full 2^32 bit range permutation
    // Half the values will be negative
    val fullPerm = intPermutation(-1, seed = 1L)
    println(fullPerm.encode(0)) // 1339315335
    println(fullPerm.encode(1)) // -897806455

}
```

### How it works

KPermute creates a **deterministic shuffle** of all numbers in a chosen range,
for example, from `0` to `9999`.
Given the same seed, it always produces the same unique rearrangement of values,
but every number appears exactly once.
This means you can "scramble" IDs or keys in a repeatable way, without storing
lookup tables.

Under the hood, each number is passed through a small **mixing function**
several times.
That function multiplies by a constant, adds a secret key, and blends bits
together using fast XOR and shift operations.
Because these operations are designed to be reversible, the process can also run
backwards:
calling `decode()` will recover the original number from its scrambled form.

The algorithm behaves a bit like a simplified **Feistel cipher** (used in many
encryption systems), but instead of encrypting text, it permutes integers.
It applies several invertible "rounds" of hashing to mix the bits thoroughly,
then repeats until the result fits in your target range.
The result is a fast and lightweight way to map integers to unique pseudo-random
counterparts.

### 🔗 Sources

The original source of this implementation is unknown but is presumed to be in
the public domain.  
This version includes modifications and refinements by me.  
If you recognize or can identify the original source, please contact me.

- [Ciphers with Arbitrary Finite Domains (2002)](https://web.cs.ucdavis.edu/~rogaway/papers/subset.pdf):  
  Introduces the cycle-walking method for mapping a permutation over a
  power-of-two space into a smaller domain.
- [Format-Preserving Encryption (FFX) (2009)](https://csrc.nist.gov/csrc/media/projects/block-cipher-techniques/documents/bcm/proposed-modes/ffx/ffx-spec.pdf):  
  Defines standard format-preserving encryption constructions that also rely on
  cycle-walking.
- [Feistel Ciphers](https://en.wikipedia.org/wiki/Feistel_cipher):  
  Classical invertible round-based structure used in many block ciphers;
  `KPermute` is simpler but more efficient.
- [Integer Hash Functions (1997)](http://burtleburtle.net/bob/hash/integer.html):  
  Overview of multiply–xor–shift integer mixers used in `KPermute`’.
- [An Experimental Exploration of Marsaglia’s Xorshift Generators (2016)](https://arxiv.org/pdf/1402.6246.pdf):  
  Analyzes xor/shift mixers and their statistical properties.
- [xxHash (2014)](https://github.com/Cyan4973/xxHash):  
  Fast non-cryptographic hash function using constant multipliers and
  bit-scrambling, closely related in design to `KPermute`’s mixing logic. The
  default primes are chosen from here.
