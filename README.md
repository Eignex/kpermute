# KPermute

[![Maven Central](https://img.shields.io/maven-central/v/com.eigenity/kpermute.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.eigenity/kpermute/1.0.0)

> ⚙️ Kotlin library for shuffling lists too big for memory or for ID obfuscation. Using bijective integer permutations with fast cycle-walking hash mixing.
> 
> **Not cryptographic — but fast, lightweight, and repeatable.**

---

### ✨ Overview

`kpermute` provides stable, deterministic **pseudo-random permutations** over integer domains using a simple **cycle-walking hash** algorithm.  It behaves like a keyed shuffler: every RNG seed defines a new bijection between `[0, size)`.
Originally developed as part of [COMBO](https://github.com/Eigenity/combo) used for statistical sampling of decision variables in a search space but has been extracted here into it's own thing.

---

### 🧪 Use cases
 
- Generating repeatable pseudo-random shuffles
- Obfuscating integer IDs (user IDs, session numbers)
- Sampling or load-balancing without collisions
- Data masking for non-sensitive identifiers


### 🚀 Example

```kotlin
import com.eigenity.kpermute.*

fun main() {

    // This example shows how to encode a Long ID to e.g. obfuscate how many users you have in your app
    val longPerm = LongPermutation(size = 1_000_000_000L, Random(0))
    val encoded = longPerm.encode(42L)
    println("encoded: $encoded (always 85 for this fixed seed)")
    val decoded = longPerm.decode(encoded)
    println("decoded: $decoded (should be 42)")
    // perm.encode(2_000_000_000) // too big to encode since the size was set to 1B

    // This example shows how to shuffle a very large list.
    // Pretend that this list is read from a disk with random access.
    // Like a parquet file with billions of rows.
    val list = List(100) { it }
    val intPerm = IntPermutation(size = list.size)

    for (i in list.indices) {
        // this will print all elements shuffled without loading the whole list in memory
        print("" + list[intPerm.encode(i)] + ", ")
    }
    println()

    // verify that all elements are included once
    println(longPerm.toList().size) // 100
    println(longPerm.toSet().size) // 100

}
```

### 🧠 How It Works
KPermute creates a **deterministic shuffle** of all numbers in a chosen range — for example, from `0` to `9999`.  
Given the same seed, it always produces the same unique rearrangement of values, but every number appears exactly once.  
This means you can "scramble" IDs or keys in a repeatable way, without storing lookup tables or using heavy cryptography.

Under the hood, each number is passed through a small **mixing function** several times.  
That function multiplies by a constant, adds a secret key, and blends bits together using fast XOR and shift operations.  
Because these operations are carefully chosen to be reversible, the process can also run backwards:  
calling `decode()` will perfectly recover the original number from its scrambled form.

The algorithm behaves a bit like a simplified **Feistel cipher** (used in many encryption systems), but instead of encrypting text, it permutes integers.  
It applies several invertible "rounds" of arithmetic to mix the bits thoroughly, then repeats until the result fits in your target range.  
The result is a fast, lightweight way to map integers to unique pseudo-random counterparts — ideal for obfuscation, sampling, or repeatable randomization.

### 🔗 Sources
The original source of this implementation is unknown but is presumed to be in the public domain.  
This version includes modifications and refinements by me.  
If you recognize or can identify the original source, please contact me.

- [Ciphers with Arbitrary Finite Domains (2002)](https://web.cs.ucdavis.edu/~rogaway/papers/subset.pdf):  
  Introduces the cycle-walking method for mapping a permutation over a power-of-two space into a smaller domain.  
- [Format-Preserving Encryption (FFX) (2009)](https://csrc.nist.gov/csrc/media/projects/block-cipher-techniques/documents/bcm/proposed-modes/ffx/ffx-spec.pdf):  
  Defines standard format-preserving encryption constructions that also rely on cycle-walking.  
- [Feistel Ciphers](https://en.wikipedia.org/wiki/Feistel_cipher):  
  Classical invertible round-based structure used in many block ciphers; `KPermute` is conceptually similar but operates on a single modular integer instead of split halves for efficiency.  
- [Integer Hash Functions (1997)](http://burtleburtle.net/bob/hash/integer.html):  
  Overview of multiply–xor–shift integer mixers that influenced `KPermute`’s round function.  
- [An Experimental Exploration of Marsaglia’s Xorshift Generators (2016)](https://arxiv.org/pdf/1402.6246.pdf):  
  Analyzes xor/shift mixers and their statistical properties.  
- [xxHash (2014)](https://github.com/Cyan4973/xxHash):  
  Fast non-cryptographic hash function using constant multipliers and bit-scrambling, closely related in design to `KPermute`’s mixing logic. The default primes are chosen from here.
