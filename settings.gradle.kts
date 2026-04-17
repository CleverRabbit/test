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
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Messenger"
include(":app")
include(":core:network")
include(":core:datastore")
include(":core:security")
include(":core:ui")
include(":core:common")
include(":domain:auth")
include(":domain:chat")
include(":domain:contacts")
include(":domain:media")
include(":data")
include(":feature:auth")
include(":feature:chat")
include(":feature:contacts")
include(":feature:settings")
include(":feature:media")
