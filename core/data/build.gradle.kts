plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.cjlu.core.data"
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
    api(project(":core:model"))
    api(project(":core:network"))
    api(project(":core:database"))
    implementation(libs.retrofit.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)
    implementation("androidx.documentfile:documentfile:1.0.1")
}
