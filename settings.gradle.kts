pluginManagement {
    val kotlinVersion: String by settings
    val ktorVersion: String by settings
    val foojayResolverVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("io.ktor.plugin") version ktorVersion
        id("org.gradle.toolchains.foojay-resolver-convention") version foojayResolverVersion
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

rootProject.name = "quiet-water"
