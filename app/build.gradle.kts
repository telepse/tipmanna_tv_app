import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    kotlin("kapt")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.example.telepse"
    compileSdk = 36

    defaultConfig {
        applicationId = "tv.tipmanna.androidtv"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions += "environment"
    productFlavors {
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            // Documented staging API (STAGING-HANDOFF / Phase 1 audit)
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://tipmanna-staging.onrender.com\"",
            )
            buildConfigField(
                "String",
                "SHARE_CONTENT_ORIGIN",
                "\"https://tipmanna-fj3ffgvfq-test01.vercel.app\"",
            )
            // Public Pusher client key/cluster only (matches staging Render env / tv-app bootstrap)
            buildConfigField(
                "String",
                "PUSHER_PUBLIC_KEY",
                "\"14924529808c59fdb418\"",
            )
            buildConfigField(
                "String",
                "PUSHER_CLUSTER",
                "\"eu\"",
            )
            buildConfigField("boolean", "ENABLE_VIDEO_SHARE_QR", "true")
        }
        create("prod") {
            dimension = "environment"
            // Documented production API (BACKEND_URL / prior ApiConfig)
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://tipmanna-live.onrender.com\"",
            )
            buildConfigField(
                "String",
                "SHARE_CONTENT_ORIGIN",
                "\"https://tipmanna.tv\"",
            )
            buildConfigField(
                "String",
                "PUSHER_PUBLIC_KEY",
                "\"09e014941983d1503d77\"",
            )
            buildConfigField(
                "String",
                "PUSHER_CLUSTER",
                "\"us3\"",
            )
            // Same-video mobile QR not E2E-verified on prod; hide until PO signs off (see P4-QR-SHARE-BLOCKED.md).
            buildConfigField("boolean", "ENABLE_VIDEO_SHARE_QR", "false")
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = keystoreProperties.getProperty("storeFile")?.let { file(it) }
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Hilt-generated release components are not on disk during unit-test lint (AGP lint bug).
        checkTestSources = false
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Material3 (for components not available in TV Material3)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Hilt Navigation Compose
    implementation(libs.hilt.navigation.compose)

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.logging)

    implementation("com.google.zxing:core:3.5.3")

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Coil for GIF loading
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // ExoPlayer for video playback
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.ui)

    // ZXing for QR code generation
    implementation(libs.zxing.core)

    // Pusher for real-time events
    implementation(libs.pusher.java.client)

    // Gson for JSON parsing
    implementation(libs.gson)
    implementation(libs.androidx.security.crypto)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.ktor:ktor-client-mock:3.0.3")
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}