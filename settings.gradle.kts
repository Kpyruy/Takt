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

rootProject.name = "Takt"
include(":app")
include(":core:model")
include(":core:database")
include(":core:data")
include(":core:ui")
include(":feature:home")
include(":feature:calendar")
include(":feature:subjects")
include(":feature:studyplan")
include(":feature:settings")
