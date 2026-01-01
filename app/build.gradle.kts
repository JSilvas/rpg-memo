plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.memo.widget"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.memo.widget"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.2.0-phase1"
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")

    // DataStore (Phase 1)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Kotlin Serialization (Phase 1)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Lifecycle (Phase 1)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
