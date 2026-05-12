import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
}

val versionPropsFile = file("version.properties")

fun getAndIncrementVersion(): Pair<Int, String> {
    val props = Properties()
    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { props.load(it) }
    }

    var currentCode = (props.getProperty("VERSION_CODE", "1").toIntOrNull() ?: 1)
    var currentName = props.getProperty("VERSION_NAME", "1.0")

    val isBuilding = gradle.startParameter.taskNames.any {
        it.contains("assemble", ignoreCase = true) ||
                it.contains("bundle", ignoreCase = true) ||
                it.contains("install", ignoreCase = true)
    }

    if (isBuilding) {
        currentCode += 1
        val nextName = (currentName.toDoubleOrNull() ?: 1.0) + 0.1
        currentName = String.format("%.1f", nextName)

        props.setProperty("VERSION_CODE", currentCode.toString())
        props.setProperty("VERSION_NAME", currentName)
        FileOutputStream(versionPropsFile).use { props.store(it, null) }
    }

    return Pair(currentCode, currentName)
}

val (nextCode, nextName) = getAndIncrementVersion()

android {
    namespace = "com.rksaykot.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rksaykot.myapplication"
        minSdk = 24
        targetSdk = 35
        versionCode = nextCode
        versionName = nextName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            firebaseAppDistribution {
                artifactType = "APK"
                groups = "testers"
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            firebaseAppDistribution {
                artifactType = "APK"
                groups = "testers"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // ==================== FIREBASE ====================
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.appdistribution)
    implementation(libs.play.services.auth)

    // ==================== COMPOSE ====================
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // ==================== ANDROIDX ====================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ==================== IMAGE LOADING ====================
    implementation(libs.coil.compose)

    // ==================== BACKGROUND WORK ====================
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // ==================== COROUTINES ====================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")

    // ==================== TESTING ====================
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // ==================== DEBUG ====================
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}