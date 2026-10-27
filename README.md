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

Fast, seeded, reversible permutations over integer domains of any size. Use
it to obfuscate IDs, shuffle lazily, or sample without collisions. Not
cryptographic.

### Installation

```kotlin
implementation("com.eignex:kpermute:1.2.0")
```

## Usage

Obfuscate an ID:

```kotlin
val idPerm = longPermutation(seed = 1L)
val longId = 49102490812045L
val encoded = idPerm.encode(longId)
println("encoded: $encoded (always prints 3631103739497407856)")
```

Shuffle a list lazily:

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

Permute a range:

```kotlin
val rangePerm = intPermutation(-100..199)
println("encode(-50): ${rangePerm.encode(-50)}")
println("decode(...): ${rangePerm.decode(rangePerm.encode(-50))}")
```

Permute the full 32-bit domain:

```kotlin
val fullPerm = intPermutation(-1, seed = 1L)
println(fullPerm.encode(0)) // 1789640036
println(fullPerm.encode(1)) // -299069089
```

## How It Works

`size` selects the domain `[0, size)`. `-1` means every 32- or 64-bit value;
other negative sizes are read as unsigned.

| Domain        | Implementation                 | Method                    |
|---------------|--------------------------------|---------------------------|
| ≤ 4096        | `Small[Int/Long]Permutation`   | Shuffled lookup table     |
| Finite        | `Bounded[Int/Long]Permutation` | Mixing with cycle-walking |
| Full width    | `Full[Int/Long]Permutation`    | Mixing                    |

Range factories such as `intPermutation(-100..199)` wrap these in a view over
any non-empty range.

Each mixing round multiplies by an odd constant, adds or xors a per-round key,
and applies `x ^= x >>> s`; every step is invertible [1] [3]. For
non-power-of-two sizes, cycle-walking [1] [2] permutes the enclosing
power-of-two block and repeats until the output is in range.

## References

[1]: https://web.cs.ucdavis.edu/~rogaway/papers/subset.pdf

[2]: https://csrc.nist.gov/csrc/media/projects/block-cipher-techniques/documents/bcm/proposed-modes/ffx/ffx-spec.pdf

[3]: http://burtleburtle.net/bob/hash/integer.html

1. P. Rogaway and T. Shrimpton,
   “Ciphers with Arbitrary Finite Domains,” *CT-RSA 2002*. [PDF][1]
2. M. Bellare, P. Rogaway, and T. Spies,
   “The FFX Mode of Operation for Format-Preserving Encryption,” *NIST
   submission, 2010.* [Spec][2]
3. B. Jenkins,
   “Integer Hash Functions,” 1997. [Web][3]
