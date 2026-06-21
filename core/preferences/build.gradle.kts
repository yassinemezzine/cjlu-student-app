plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.cjlu.core.preferences"
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
    implementation(project(":core:resources"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
}
