plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("maven-publish")
}

dependencies {
    implementation(libs.ktor.serialization.kotlinx.json)
}

tasks {
    register<Wrapper>("wrapper") {
        gradleVersion = "8.1.1"
    }
    ktlintFormat {
        this.enabled = true
    }
    ktlintCheck {
        dependsOn("ktlintFormat")
    }
    build {
        dependsOn("ktlintCheck")
    }
}
