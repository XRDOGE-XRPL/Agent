pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.google.com") }
        google()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.google.com") }
        google()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}

rootProject.name = "AutonomerEntwicklungsagent"
include(":app")
