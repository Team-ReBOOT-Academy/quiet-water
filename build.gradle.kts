plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin")
}

group = "ru.omsu.quietwater"
version = "1.0.0"

val ktorVersion: String by project
val serializationJsonVersion: String by project
val logbackVersion: String by project
val kafkaVersion: String by project
val jvmToolchainVersion: String by project

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(jvmToolchainVersion.toInt())
}

application {
    mainClass.set("ru.omsu.quietwater.QuietWaterApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationJsonVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
}

tasks.test {
    useJUnitPlatform()
}
