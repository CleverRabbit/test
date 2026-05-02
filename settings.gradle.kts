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

rootProject.name = "VibeMessenger"

include(":app")
include(":core:common")
include(":core:network")
include(":core:ui")
include(":core:security")
include(":core:datastore")
include(":domain:model")
include(":domain:repository")
include(":domain:usecase")
include(":data")
include(":feature:auth")
include(":feature:chat")
include(":feature:settings")
include(":feature:media")
