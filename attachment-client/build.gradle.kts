import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("maven-publish")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs = listOf(
            "-opt-in=kotlin.uuid.ExperimentalUuidApi"
        )
    }
}

dependencies {
    api(project(":attachment-model"))

    implementation(libs.arrow.core)
    implementation(libs.arrow.functions)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.auth)
    implementation(libs.kotlin.logging)

    testImplementation(testLibs.bundles.kotest)
    testImplementation(testLibs.ktor.client.mock)
    testImplementation(testLibs.kotest.assertions.arrow)
    testImplementation(kotlin("test"))
}

tasks {
    test {
        useJUnitPlatform()
    }

    ktlintFormat {
        enabled = true
    }

    ktlintCheck {
        dependsOn("ktlintFormat")
    }

    build {
        dependsOn("ktlintCheck")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "no.nav.helsemelding"
            artifactId = "attachment-client"
            version = "0.0.1"
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/navikt/${rootProject.name}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
