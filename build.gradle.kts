buildscript {
    repositories {
        google()
        jcenter()

    }
    dependencies {
        classpath(ClassPaths.gradlePlugin)
        classpath(ClassPaths.kotlinPlugin)
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}