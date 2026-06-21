plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.cjlu.core.navigation"
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
    implementation("androidx.navigation:navigation-compose:2.7.7")
}
