pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CJLUStudentApp"
include(":app")
include(":backend-ktor")
include(":shared-contract")
include(":core:resources")
include(":core:designsystem")
include(":core:navigation")
include(":core:model")
include(":core:network")
include(":core:database")
include(":core:data")
include(":core:preferences")
include(":feature:auth")
include(":feature:academic")
include(":feature:services")
include(":feature:messages")
include(":feature:home")
include(":feature:profile")
