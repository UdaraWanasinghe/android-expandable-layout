pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
buildscript {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
    dependencies {
        classpath("com.aureusapps.gradle:plugin-utils:1.0.0")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    versionCatalogs {
        create("libs") {
            from("com.aureusapps:version-catalog:1.0.0")
        }
    }
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}
rootProject.name = "expandable-layout"
include("expandable-layout", "example")