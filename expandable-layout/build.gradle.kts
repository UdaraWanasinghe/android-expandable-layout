@file:Suppress("UnstableApiUsage")

import com.aureusapps.gradle.PublishLibraryConstants.GROUP_ID
import com.aureusapps.gradle.PublishLibraryConstants.VERSION_NAME

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.aureusapps.gradle.update-version")
    id("com.aureusapps.gradle.publish-library")
}

class Props(project: Project) {
    val groupId = project.findProperty(GROUP_ID) as String
    val versionName = project.findProperty(VERSION_NAME) as String
}

val props = Props(project)

android {
    namespace = "${props.groupId}.expandablelayout"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishLibrary {
    groupId.set(props.groupId)
    artifactId.set("expandable-layout")
    versionName.set(props.versionName)
    libName.set("ExpandableLayout")
    libDescription.set("Adds horizontal or vertical expand and collapse animations to Android views.")
    libUrl.set("https://github.com/UdaraWanasinghe/android-expandable-layout")
    licenseName.set("MIT License")
    licenseUrl.set("https://github.com/UdaraWanasinghe/android-expandable-layout/blob/main/LICENSE")
    devId.set("UdaraWanasinghe")
    devName.set("Udara Wanasinghe")
    devEmail.set("udara.developer@gmail.com")
    scmConnection.set("scm:git:https://github.com/UdaraWanasinghe/android-expandable-layout.git")
    scmDevConnection.set("scm:git:ssh://git@github.com/UdaraWanasinghe/android-expandable-layout.git")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.aureusapps.extensions)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
