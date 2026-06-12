import java.util.Properties

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

group = "com.cjlu"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.cjlu.backend.ApplicationKt")
}

// Gradle `run` does not inherit IDE env vars; enable local dev fallbacks for :backend-ktor:run only.
tasks.named<JavaExec>("run") {
    environment("CJLU_ALLOW_INSECURE_DEV_DEFAULTS" to "true")

    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val properties = Properties()
        localPropertiesFile.inputStream().use { properties.load(it) }
        properties.forEach { key, value ->
            val keyStr = key.toString()
            val valStr = value.toString()
            when {
                keyStr.startsWith("db.") || keyStr.startsWith("admin.") -> {
                    val envName = keyStr.replace(".", "_").uppercase()
                    environment(envName, valStr)
                }
                keyStr == "seed.db" -> environment("SEED_DB", valStr)
                keyStr == "jwt.secret" -> environment("JWT_SECRET", valStr)
                keyStr == "student.api.key" -> environment("STUDENT_API_KEY", valStr)
            }
        }
    }
}

dependencies {
    implementation(project(":shared-contract"))
    implementation("io.ktor:ktor-server-core-jvm:3.0.0")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.0")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.0")
    implementation("io.ktor:ktor-server-websockets-jvm:3.0.0")
    implementation("io.ktor:ktor-server-sessions-jvm:3.0.0")
    implementation("io.ktor:ktor-server-auth-jvm:3.0.0")
    implementation("io.ktor:ktor-server-freemarker-jvm:3.0.0")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.0.0")
    implementation("org.jetbrains.exposed:exposed-core:0.59.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.59.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.59.0")
    implementation("org.jetbrains.exposed:exposed-migration:0.59.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.30.1")
    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.0.0")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm:3.0.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.2.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

tasks.test {
    environment("CJLU_ALLOW_INSECURE_DEV_DEFAULTS", "true")
    systemProperty("cjlu.test.db", "mem")
}

// Android Studio / configuration cache tooling may request `compileKotlinJvm` (KMP-style).
// This module uses `kotlin("jvm")` only, which registers `compileKotlin` — provide an alias after the plugin creates it.
afterEvaluate {
    tasks.register("compileKotlinJvm") {
        group = "build"
        description = "Compatibility alias; runs compileKotlin (JVM-only module, not KMP)"
        dependsOn(tasks.named("compileKotlin"))
    }
}
