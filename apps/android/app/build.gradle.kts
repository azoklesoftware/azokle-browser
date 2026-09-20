import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.azokle.search"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.azokle.search"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "2.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystorePropertiesFile = rootProject.file("key.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("KEYSTORE_PATH")
                ?: keystoreProperties.getProperty("storeFile")
                ?: "/home/azokledeveloper/.android/keystore/azokle-search.jks"
            val storePass = System.getenv("KEYSTORE_PASSWORD")
                ?: keystoreProperties.getProperty("storePassword")
            val keyUserAlias = System.getenv("KEY_ALIAS")
                ?: keystoreProperties.getProperty("keyAlias")
            val keyPass = System.getenv("KEY_PASSWORD")
                ?: keystoreProperties.getProperty("keyPassword")

            if (file(storeFilePath).exists() && !storePass.isNullOrBlank()) {
                storeFile = file(storeFilePath)
                storePassword = storePass
                keyAlias = keyUserAlias ?: "azokle-search"
                keyPassword = keyPass ?: storePass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning != null && releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    splits {
        abi {
            // Enable ABI splits specifically for APK builds (assemble/install), disable for Bundles and full build tasks to prevent resource shrinking collisions
            val taskNames = gradle.startParameter.taskNames
            isEnable = taskNames.any { it.contains("assemble", ignoreCase = true) || it.contains("install", ignoreCase = true) } &&
                !taskNames.any { it.contains("bundle", ignoreCase = true) || it.contains("build", ignoreCase = true) }
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = false
    }

    sourceSets {
        getByName("main") {
            assets.directories.add("src/main/assets")
            assets.directories.add("${layout.buildDirectory.asFile.get()}/generated/shared_assets")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

val syncSharedAssets = tasks.register<Sync>("syncSharedAssets") {
    from(file("../../../data/filters")) {
        into("adblock")
    }
    from(file("../../../data/filters")) {
        into("data/filters")
    }
    from(file("../../../extensions")) {
        into("extensions")
    }
    into(layout.buildDirectory.dir("generated/shared_assets"))
}

tasks.matching { it.name.contains("Assets") && it.name != "syncSharedAssets" }.configureEach {
    dependsOn(syncSharedAssets)
}

tasks.named("preBuild").configure {
    dependsOn(syncSharedAssets)
}

// GeckoView 154 omni specifies min_compile_sdk=37.1 in its AAR metadata.
// While compiling against stable Android 16 (API 36), bypass AarMetadata check to permit native compilation.
tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.media)

    // Jetpack Compose & Material 3
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)

    // Networking, Async & Images
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)

    // Mozilla GeckoView (Quantum Web Rendering Engine with 16 KB Page-Size Support)
    implementation(libs.geckoview.omni)

    // Standalone Embedded Tor Daemon & Onion Network Engine
    implementation(libs.tor.android)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)

    // CameraX & Barcode Scanner Engine (Privacy-First Offline ZXing)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}