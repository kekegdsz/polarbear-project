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

rootProject.name = "Android-im"
include(":app")
include(":core:common")
include(":business:user")
include(":im:core")
include(":shared:ui")
include(":notify")
include(":bootstrap")
include(":feature:splash")
include(":feature:auth")
include(":feature:home")
include(":feature:chat")
