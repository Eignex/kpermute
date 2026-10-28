import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("com.eignex.kmp")
}

repositories {
    ivy {
        url = uri("https://github.com/Eignex/kbuild/releases/download")
        patternLayout { artifact("v[revision]/[artifact]-[revision](-[classifier]).[ext]") }
        metadataSources { gradleMetadata() }
        content { includeModule("com.eignex", "kbuild-platform") }
    }
}

eignexPublish {
    description.set(
        "Kotlin library for shuffling lists too big for memory or for ID obfuscation. " +
                "Using bijective integer permutations with fast cycle-walking hash mixing."
    )
    githubRepo.set("Eignex/kpermute")
}

// Run the real JVM artifact on both the supported floor and the current LTS.
eignexBuild {
    packagedTestJdks.set(listOf(17, 25))
}

tasks.withType<org.gradle.api.tasks.testing.AbstractTestTask>().configureEach {
    // Do not reuse results produced with the old aggregate-only failure policy.
    inputs.property("kotlin.tests.individualTaskReports", providers.gradleProperty("kotlin.tests.individualTaskReports"))
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    jvm()
    js { browser(); nodejs() }
    wasmJs { browser(); nodejs() }
    wasmWasi { nodejs() }
    linuxX64(); linuxArm64()
    macosArm64(); mingwX64()
    iosX64(); iosArm64(); iosSimulatorArm64()
}

dokka {
    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory.set(projectDir.resolve("src"))
            val sub = projectDir.relativeTo(rootDir).invariantSeparatorsPath
            val prefix = if (sub.isEmpty()) "src" else "$sub/src"
            remoteUrl("https://github.com/Eignex/${rootProject.name}/blob/main/$prefix")
            remoteLineSuffix.set("#L")
        }
    }
}

// Select an installed simulator in CI rather than relying on a particular iPhone model.
tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest>().configureEach {
    providers.gradleProperty("iosSimulatorDevice").orNull?.let { device.set(it) }
}
