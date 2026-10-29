import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("com.eignex.kmp")
    id("com.eignex.publish.github")
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

eignexBuild { jvmToolchain.set(17) }

// Routine checks cover JVM + Linux x64; release validation opts into every target.
val fullTargets = providers.gradleProperty("targets.full").isPresent

@OptIn(ExperimentalWasmDsl::class, org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
kotlin {
    abiValidation {
        referenceDumpDir.set(layout.projectDirectory.dir(if (fullTargets) "api-full" else "api"))
    }
    jvm()
    linuxX64()
    if (fullTargets) {
        js { browser(); nodejs() }
        wasmJs { browser(); nodejs() }
        wasmWasi { nodejs() }
        linuxArm64()
        macosArm64(); mingwX64()
        iosX64(); iosArm64(); iosSimulatorArm64()
    }
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
