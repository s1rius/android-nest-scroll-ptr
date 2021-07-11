package wtf.s1rius.ptr.buildsrc

object Versions {
    const val kotlin = "1.5.0"
    const val ktx = "1.0.0"
    const val kotlinCoroutines = "1.0.1"
    const val gradlePlugin = "4.1.0"
    const val compileSdkVersion = 28
    const val minSdkVersion = 14
    const val targetSdkVersion = 28
    const val versionCode = 1
    const val versionName = "1.0.0"
    const val bytex = "0.2.7"
}

object Plugins {
    const val androidLib = "com.android.library"
}

object Deps {
    object Kotlin {
        const val stdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
        const val coroutines =
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}"
        const val coroutinesAndroid =
            "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.kotlinCoroutines}"
        const val ktxCore = "androidx.core:core-ktx:${Versions.ktx}"
    }

    object AndroidX {
        const val appcompat = "androidx.appcompat:appcompat:1.3.0"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:1.1.3"
        const val cardView = "androidx.cardview:cardview:1.0.0"

        object Core {
            const val utils = "androidx.legacy:legacy-support-core-utils:1.0.0"
            const val ktx = "androidx.core:core-ktx:1.5.0"
        }

        const val viewpager2 = "androidx.viewpager2:viewpager2:1.0.0"
    }

    object Google {
        const val material = "com.google.android.material:material:1.0.0"
        const val glide = "com.github.bumptech.glide:glide:3.7.0"
    }

    const val circleimageview = "de.hdodenhof:circleimageview:1.3.0"

    const val hugo2 = "wtf.s1.pudge:hugo2-core:0.1.4"

    const val multitype = "com.drakeet.multitype:multitype:4.2.0"
}

object ClassPaths {
    const val gradlePlugin = "com.android.tools.build:gradle:${Versions.gradlePlugin}"
    const val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val bytex = "com.bytedance.android.byteX:base-plugin:${Versions.bytex}"
    const val hugoByteX = "wtf.s1.pudge:hugo2-bytex:0.1.4"
    const val mavenPlugin = "com.vanniktech:gradle-maven-publish-plugin:0.17.0"
    const val dokaa = "org.jetbrains.dokka:dokka-gradle-plugin:1.4.32"
}