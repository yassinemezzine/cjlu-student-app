plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.cjlu.core.model"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":shared-contract"))
    implementation(project(":core:resources"))
    implementation("androidx.annotation:annotation:1.9.1")
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
