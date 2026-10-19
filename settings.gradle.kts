
pluginManagement {
    repositories {
        maven {
            // RetroFuturaGradle
            name = "GTNH Maven"
            url = uri("https://nexus.gtnewhorizons.com/repository/public/")
            mavenContent {
                includeGroup("com.gtnewhorizons")
                includeGroupByRegex("com\\.gtnewhorizons\\..+")
            }
        }
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    // Override the Kotlin compiler bundled with GTNH's convention plugin to match Forgelin.
    kotlin("jvm") version("2.4.10") apply false
    id("com.gtnewhorizons.gtnhsettingsconvention") version("2.0.34")
}
