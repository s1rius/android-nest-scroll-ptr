plugins {
    id(Plugins.androidLib)
    kotlin("android")
    kotlin("android.extensions")
}

android {
    compileSdkVersion(Versions.compileSdkVersion)

    defaultConfig {
        minSdkVersion(Versions.minSdkVersion)
        targetSdkVersion(Versions.targetSdkVersion)
    }

    lintOptions {
        isAbortOnError = false
    }
}

dependencies {
    implementation(Libs.AndroidX.Core.utils)
}


//apply from: './gradle-mvn-push.gradle'
