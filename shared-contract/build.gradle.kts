plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.cjlu"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
