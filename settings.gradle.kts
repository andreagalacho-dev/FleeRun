pluginManagement {
    repositories {
        google()  // Repositorio de Google para dependencias de Android
        mavenCentral()  // Repositorio de Maven Central para dependencias generales
        gradlePluginPortal()  // Para plugins Gradle
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()  // Repositorio de Google
        mavenCentral()  // Repositorio de Maven Central
        maven("https://jitpack.io")  // Si utilizas dependencias desde JitPack
    }
}

rootProject.name = "FleeRun"
include(":app")
