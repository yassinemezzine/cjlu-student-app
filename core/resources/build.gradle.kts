plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.cjlu.core.resources"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
}
