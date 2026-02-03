import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"
    id("org.jetbrains.dokka") version "2.1.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.5"
    `maven-publish`
    signing

    id("io.github.sgtsilvio.gradle.maven-central-publishing") version "0.4.1"
}

group = "com.eignex"
version = "1.0.1"

repositories { mavenCentral() }

kotlin {
    jvmToolchain(21)
    compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

tasks.named<Jar>("javadocJar") {
    dependsOn(tasks.named("dokkaGenerate"))
    from(layout.buildDirectory.dir("dokka/html"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("kpermute")
                description.set("Pure Kotlin JVM library.")
                url.set("https://github.com/Eignex/kpermute")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                scm {
                    url.set("https://github.com/Eignex/kpermute")
                    connection.set("scm:git:https://github.com/Eignex/kpermute.git")
                    developerConnection.set("scm:git:ssh://git@github.com/Eignex/kpermute.git")
                }
                developers {
                    developer {
                        id.set("rasros")
                        name.set("Rasmus Ros")
                        url.set("https://github.com/rasros")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "localStaging"
            url = uri(layout.buildDirectory.dir("staging-repo"))
        }
    }
}

signing {
    val key = findProperty("signingKey") as String?
    val pass = findProperty("signingPassword") as String?

    if (key != null && pass != null) {
        useInMemoryPgpKeys(key, pass)
        sign(publishing.publications["mavenJava"])
    } else {
        logger.lifecycle("Signing disabled: signingKey or signingPassword not defined.")
    }
}
