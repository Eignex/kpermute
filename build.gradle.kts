plugins {
    id("com.eignex.kmp") version "1.0.0"
}

eignexPublish {
    description.set(
        "Kotlin library for shuffling lists too big for memory or for ID obfuscation. " + "Using bijective integer permutations with fast cycle-walking hash mixing."
    )
    githubRepo.set("Eignex/kpermute")
}


kotlin {
    jvm()
    js(IR) { browser(); nodejs() }
    linuxX64(); macosX64(); macosArm64(); mingwX64()
}
