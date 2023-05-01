plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "me.beresnev"
version = "unused, run with -v or --version instead"

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("me.beresnev.downloader.periscope.MainKt")
}

tasks.jar {
    enabled = false // shadowJar instead
}

tasks.shadowJar {
    archiveBaseName.set("periscope-chat-downloader")

    // don't want any more noise
    archiveClassifier.set("")
    archiveVersion.set("")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    implementation("io.ktor:ktor-client-core:2.3.0")
    implementation("io.ktor:ktor-client-cio:2.3.0")

    // silence warning message, don't need logging
    implementation("org.slf4j:slf4j-nop:1.7.36")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

