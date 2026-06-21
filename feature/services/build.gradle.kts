plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.cjlu.feature.services"
    compileSdk = 36
    defaultConfig { minSdk = 30 }
    buildFeatures { compose = true }
}

dependencies {
    api(project(":core:model"))
    api(project(":core:network"))
    implementation(project(":core:resources"))
    implementation(project(":core:data"))
    implementation(project(":feature:academic"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.documentfile:documentfile:1.0.1")
}
