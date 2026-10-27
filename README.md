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

The size selects the domain 0 until size. A size of -1 means every 32- or
64-bit value, and other negative sizes are read as unsigned.

Sizes up to 4096 use a shuffled lookup table. Larger sizes mix within the
enclosing power-of-two block and use
[cycle-walking](https://web.cs.ucdavis.edu/~rogaway/papers/subset.pdf),
repeating until the output is in range. The full 32- or 64-bit domain mixes directly. Range
factories wrap any of these in a view over a non-empty range.

Each mixing round multiplies by an odd constant, adds or xors a per-round key,
and xor-shifts the value right, in the style of
[Jenkins' integer hashes](http://burtleburtle.net/bob/hash/integer.html).
Every step is invertible.
