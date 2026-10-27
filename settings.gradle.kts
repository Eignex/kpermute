rootProject.name = "kpermute"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Auto-provisions the required JDK.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
