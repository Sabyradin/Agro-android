pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "agroland-android"

include(":app")
include(":core:ui")
include(":core:common")
include(":core:l10n")
include(":core:network")
include(":feature:shell")
include(":feature:auth")
include(":feature:profile")
include(":feature:marketplace")
include(":feature:cart")
include(":feature:payment")
include(":feature:wallet")
include(":feature:push")
include(":feature:location")
include(":feature:chat")
include(":feature:notifications")