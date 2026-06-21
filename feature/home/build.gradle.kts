plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.cjlu.feature.home"
    compileSdk = 36
    defaultConfig { minSdk = 30 }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:resources"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:services"))
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
}
