import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

private fun escapeForBuildConfigString(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

private val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

private fun localProperty(key: String): String? =
    localProperties.getProperty(key)?.trim()?.takeIf(String::isNotEmpty)

private fun secret(key: String): String? =
    providers.gradleProperty(key).orNull?.trim()?.takeIf(String::isNotEmpty)
        ?: localProperty(key)
        ?: System.getenv(key.uppercase().replace('.', '_'))?.trim()?.takeIf(String::isNotEmpty)

private val insecureFallbackStudentApiKey =
    "cjlu-insecure-local-student-api-key-do-not-use-in-production"
private val apiHost = localProperty("cjlu.api.host") ?: "10.0.2.2"
private val apiPort = localProperty("cjlu.api.port")?.toIntOrNull() ?: 8080
private val studentApiKey = secret("cjlu.student.api.key").orEmpty()

android {
    namespace = "com.cjlu.core.network"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        buildConfigField("String", "API_HOST", "\"${escapeForBuildConfigString(apiHost)}\"")
        buildConfigField("int", "API_PORT", apiPort.toString())
    }

    buildTypes {
        debug {
            val key = studentApiKey.ifEmpty { insecureFallbackStudentApiKey }
            buildConfigField("String", "STUDENT_API_KEY", "\"${escapeForBuildConfigString(key)}\"")
        }
        release {
            val key = studentApiKey.ifEmpty { insecureFallbackStudentApiKey }
            buildConfigField("String", "STUDENT_API_KEY", "\"${escapeForBuildConfigString(key)}\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":shared-contract"))
    api(project(":core:model"))
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
}
