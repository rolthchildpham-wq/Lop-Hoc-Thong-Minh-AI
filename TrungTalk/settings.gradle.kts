pluginManagement {
    repositories {
        google()
        maven { url = uri("https://maven-central-eu.storage-download.googleapis.com/maven2/") }
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven { url = uri("https://maven-central-eu.storage-download.googleapis.com/maven2/") }
        mavenCentral()
    }
}
rootProject.name = "TrungTalk"
include(":app")
