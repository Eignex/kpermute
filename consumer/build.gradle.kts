plugins { application }

val pomOnly = providers.gradleProperty("consumer.pomOnly").getOrElse("false").toBoolean()
val module = if (pomOnly) "kpermute-jvm" else "kpermute"
val version = providers.gradleProperty("consumer.version").get()

dependencies {
    // No KBuild plugin, platform, or Kotlin dependency declaration: published metadata must suffice.
    implementation("com.eignex:$module:$version") {
        // CI reuses a private staging version; always inspect its current local metadata/artifact.
        isChanging = true
    }
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }
application { mainClass.set("Consumer") }

configurations.configureEach {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}
