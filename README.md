<p align="center">
  <a href="https://eignex.com/">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/Eignex/.github/refs/heads/main/profile/banner-white.svg">
      <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/Eignex/.github/refs/heads/main/profile/banner.svg">
      <img alt="Eignex" src="https://raw.githubusercontent.com/Eignex/.github/refs/heads/main/profile/banner.svg" style="max-width: 100%; width: 22em;">
    </picture>
  </a>
</p>

# KPermute

[![Maven Central](https://img.shields.io/maven-central/v/com.eignex/kpermute.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.eignex/kpermute)
[![Build](https://github.com/eignex/kpermute/actions/workflows/build.yml/badge.svg)](https://github.com/eignex/kpermute/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/eignex/kpermute/branch/main/graph/badge.svg)](https://codecov.io/gh/eignex/kpermute)
[![License](https://img.shields.io/github/license/eignex/kpermute)](https://github.com/eignex/kpermute/blob/main/LICENSE)

KPermute generates fast, deterministic, reversible integer permutations over
arbitrary integer domains using bijective hash mixing.

## Overview

Each seed defines a unique bijection over `[0, size)`. The result behaves like
a keyed shuffle: repeatable, memory-efficient, and invertible.

Typical uses are obfuscating numeric IDs (such as user or session numbers),
generating reproducible pseudo-random shuffles, collision-free sampling and
load balancing, and masking non-sensitive identifiers. It is not a cryptographic
primitive; if your use case is cryptographic, choose a real cipher.

### Installation

```kotlin
implementation("com.eignex:kpermute:1.1.2")
```

## Usage

Obfuscate a numeric ID reproducibly:

```kotlin
val idPerm = longPermutation(seed = 1L)
val longId = 49102490812045L
val encoded = idPerm.encode(longId)
println("encoded: $encoded (always prints 3631103739497407856)")
```

Shuffle a large list lazily:

```kotlin
val largeList = object : AbstractList<Int>() {
    override val size: Int get() = Int.MAX_VALUE
    override fun get(index: Int) = index
}
val perm = intPermutation(largeList.size)
val shuffled = largeList.permuted(perm)
println("shuffled: ${shuffled.take(20)}")
val unshuffled = shuffled.unpermuted(perm)
println("unshuffled: ${unshuffled.take(20)}")
```

Permute a custom range, including negatives:

```kotlin
val rangePerm = intPermutation(-100..199)
println("encode(-50): ${rangePerm.encode(-50)}")
println("decode(...): ${rangePerm.decode(rangePerm.encode(-50))}")
```

Permute the full 32-bit domain:

```kotlin
val fullPerm = intPermutation(-1, seed = 1L)
println(fullPerm.encode(0)) // 1339315335
println(fullPerm.encode(1)) // -897806455
```

---

## How It Works

KPermute builds keyed, reversible permutations over integer domains using
xor-shift-multiply mixers and cycle-walking. It never stores lookup tables, and
every output decodes back to its original value.

A permutation is selected by its `size`. A positive size means a finite domain
`[0, size)`. A size of `-1` or `-1L` means the full 32- or 64-bit signed
domain. Other negative sizes select the unsigned variants via `UIntPermutation`
or `ULongPermutation`.

| Domain Type       | Implementation               | Description                     |
|-------------------|------------------------------|---------------------------------|
| Tiny (`≤16`)      | `Array[Int/Long]Permutation` | Uses shuffled array and inverse |
| Finite            | `Half[Int/Long]Permutation`  | Uses cycle-walking              |
| Full bit-width    | `Full[Int/Long]Permutation`  | No cycle-walking                |
| Unsigned variants | `U[Int/Long]Permutation`     | Modulo `2^32` or `2^64`         |

Range factories like `intPermutation(range)` and `longPermutation(range)` wrap
these with a `range(...)` view, so you can permute directly on intervals such
as `-100..199`.

Each round multiplies by an odd constant, adds or xors a secret per-round key,
and applies xor-shift steps (`x ^= x >>> s`) to diffuse bits. Every step is
invertible via modular inverses and xor-shift inversion [1] [3] [4] [5]. For
non-power-of-two domains KPermute uses cycle-walking [1] [2]: permute in the
next power-of-two space and retry until the output falls in `[0, size)`.

## References

[1]: https://web.cs.ucdavis.edu/~rogaway/papers/subset.pdf

[2]: https://csrc.nist.gov/csrc/media/projects/block-cipher-techniques/documents/bcm/proposed-modes/ffx/ffx-spec.pdf

[3]: https://www-cs-faculty.stanford.edu/~knuth/taocp.html

[4]: http://burtleburtle.net/bob/hash/integer.html

[5]: https://arxiv.org/pdf/1402.6246.pdf

[6]: https://github.com/Cyan4973/xxHash

1. P. Rogaway and T. Shrimpton,
   “Ciphers with Arbitrary Finite Domains,” *CT-RSA 2002*. [PDF][1]
2. M. Bellare, P. Rogaway, and T. Spies,
   “The FFX Mode of Operation for Format-Preserving Encryption,” *NIST
   submission, 2010.* [Spec][2]
3. D. E. Knuth,
   *The Art of Computer Programming, Vol. 2: Seminumerical Algorithms,* 3rd ed.,
    1997. [Info][3]
4. B. Jenkins,
   “Integer Hash Functions,” 1997. [Web][4]
5. S. Vigna,
   “An Experimental Exploration of Marsaglia’s Xorshift Generators, Scrambled,”
   *TOMS 42(4), 2016.* [Preprint][5]
6. Y. Collet,
   “xxHash – Extremely fast hash algorithm,” 2014. [GitHub][6]
