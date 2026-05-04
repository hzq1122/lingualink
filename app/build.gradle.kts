plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val gitTag = System.getenv("GIT_TAG") ?: run {
    try {
        val process = ProcessBuilder("git", "describe", "--tags", "--always")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().readLine()?.trim() ?: "1.0.0-dev"
    } catch (e: Exception) {
        "1.0.0-dev"
    }
}

val versionName = gitTag.removePrefix("v")
val versionParts = versionName.split(".", "-").take(3)
val versionCode = try {
    val major = versionParts.getOrElse(0) { "0" }.toIntOrNull() ?: 0
    val minor = versionParts.getOrElse(1) { "0" }.toIntOrNull() ?: 0
    val patch = versionParts.getOrElse(2) { "0" }.toIntOrNull() ?: 0
    major * 10000 + minor * 100 + patch
} catch (e: Exception) { 1 }

android {
    namespace = "com.lingualink"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lingualink"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCode
        versionName = versionName
        buildConfigField("String", "VERSION_NAME", "\"$versionName\"")
        buildConfigField("String", "GITHUB_REPO", "\"${findProperty("GITHUB_REPO") ?: ""}\"")
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // OkHttp only - no Ktor
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}
