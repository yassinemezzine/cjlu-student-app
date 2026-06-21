import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

private fun escapeForBuildConfigString(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

private val cjluLocalProperties: Properties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}

private fun readCjluLocalProp(key: String): String? =
    cjluLocalProperties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }

private fun readCjluSecret(key: String): String? {
    val fromGradleProp = providers.gradleProperty(key).orNull?.trim().orEmpty()
    if (fromGradleProp.isNotEmpty()) return fromGradleProp

    val fromLocal = readCjluLocalProp(key).orEmpty()
    if (fromLocal.isNotEmpty()) return fromLocal

    val envKey = key.uppercase().replace('.', '_')
    val fromEnv = System.getenv(envKey)?.trim().orEmpty()
    if (fromEnv.isNotEmpty()) return fromEnv

    return null
}

/** Matches insecure fallback string in backend Database.kt when CJLU_ALLOW_INSECURE_DEV_DEFAULTS=true. */
private val insecureFallbackStudentApiKey: String =
    "cjlu-insecure-local-student-api-key-do-not-use-in-production"

private val cjluApiHost: String = readCjluLocalProp("cjlu.api.host") ?: "10.0.2.2"
private val cjluApiPort: Int = readCjluLocalProp("cjlu.api.port")?.toIntOrNull() ?: 8080
private val cjluStudentApiKey: String = readCjluSecret("cjlu.student.api.key").orEmpty()
private val isReleaseBuild = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }

if (cjluStudentApiKey.isEmpty() && !isReleaseBuild) {
    logger.warn(
        "cjlu.student.api.key is not set in local.properties. Debug builds will use the insecure dev fallback key " +
            "(same as backend when CJLU_ALLOW_INSECURE_DEV_DEFAULTS=true); release builds require the property. See local.properties.example.",
    )
}

android {
    namespace = "com.cjlu.studentapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cjlu.studentapp"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Host and port: set `cjlu.api.host`, `cjlu.api.port` in root `local.properties` (see `local.properties.example`).
        buildConfigField("String", "API_HOST", "\"${escapeForBuildConfigString(cjluApiHost)}\"")
        buildConfigField("int", "API_PORT", cjluApiPort.toString())
    }

    buildTypes {
        debug {
            val key = cjluStudentApiKey.ifEmpty { insecureFallbackStudentApiKey }
            buildConfigField("String", "STUDENT_API_KEY", "\"${escapeForBuildConfigString(key)}\"")
        }
        release {
            isMinifyEnabled = false
            val releaseStudentApiKey = readCjluSecret("cjlu.student.api.key")
                ?: insecureFallbackStudentApiKey
            val releaseApiHost = readCjluLocalProp("cjlu.api.host")?.takeIf { it.isNotBlank() } ?: "10.0.2.2"
            val releaseApiPort = readCjluLocalProp("cjlu.api.port")?.toIntOrNull() ?: 8080
            buildConfigField("String", "STUDENT_API_KEY", "\"${escapeForBuildConfigString(releaseStudentApiKey)}\"")
            buildConfigField("String", "API_HOST", "\"${escapeForBuildConfigString(releaseApiHost)}\"")
            buildConfigField("int", "API_PORT", releaseApiPort.toString())
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared-contract"))
    implementation(project(":core:resources"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:data"))
    implementation(project(":core:preferences"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:academic"))
    implementation(project(":feature:services"))
    implementation(project(":feature:messages"))
    implementation(project(":feature:home"))
    implementation(project(":feature:profile"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    implementation("io.ktor:ktor-client-core:3.0.0")
    implementation("io.ktor:ktor-client-cio:3.0.0")
    implementation("io.ktor:ktor-client-websockets:3.0.0")

    compileOnly("com.google.firebase:firebase-common-ktx:21.0.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
