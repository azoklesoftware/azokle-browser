plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

android {
    namespace = "com.azokle.search"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.azokle.search"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "1.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    splits {
        abi {
            // Enable ABI splits only for APK builds; disable for Android App Bundles (.aab)
            isEnable = !gradle.startParameter.taskNames.any { it.contains("bundle", ignoreCase = true) }
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
        checkReleaseBuilds = false
        abortOnError = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

tasks.register<Exec>("buildRustCore") {
    workingDir = file("../../../bindings/kotlin")
    environment("ANDROID_NDK_HOME", "/home/azokledeveloper/Android/Sdk/ndk/28.2.13676358")
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a",
        "-t", "armeabi-v7a",
        "-t", "x86_64",
        "-o", "${projectDir}/src/main/jniLibs",
        "build", "--release"
    )
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "androidx.lifecycle") {
            useVersion("2.8.7")
        }
        if (requested.group == "androidx.core" && (requested.name == "core" || requested.name == "core-ktx")) {
            useVersion("1.13.1")
        }
        if (requested.group == "androidx.activity") {
            useVersion("1.9.3")
        }
        if (requested.group == "androidx.appcompat") {
            useVersion("1.7.0")
        }
    }
}

tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("androidx.core:core-splashscreen:1.0.1")

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

    // Networking & Async
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")
    implementation("io.coil-kt:coil-svg:2.6.0")

    // Mozilla GeckoView (Quantum Web Rendering Engine with 16 KB Page-Size Support)
    implementation("org.mozilla.geckoview:geckoview-omni:154.0.20260824154132")

    // AndroidX Media Framework (MediaSessionCompat, MediaStyle Notification)
    implementation("androidx.media:media:1.7.0")

    // Standalone Embedded Tor Daemon & Onion Network Engine
    implementation("info.guardianproject:tor-android:0.4.9.6")

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}