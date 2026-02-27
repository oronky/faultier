plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}