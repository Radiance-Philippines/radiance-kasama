import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.radianceph.pos"
    compileSdk = 37

    defaultConfig {
        // the package the shops' POS checks in the phone's key attestation (radiance-pos core/Phones::PACKAGE)
        applicationId = "com.radianceph.pos"
        minSdk = 29 // Android 10: key attestation with an app id, CameraX, ML Kit
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        // the phones the shops use: 64-bit and 32-bit Arm (ML Kit's reader is native code)
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    // The release key lives outside the repository (on the build server); see README.
    val signing = Properties().apply {
        val f = file(System.getenv("RPOS_SIGNING") ?: "/srv/build/android-pos/keys/signing.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    signingConfigs {
        create("release") {
            if (signing.isNotEmpty()) {
                storeFile = file(signing.getProperty("storeFile"))
                storePassword = signing.getProperty("storePassword")
                keyAlias = signing.getProperty("keyAlias")
                keyPassword = signing.getProperty("keyPassword")
            }
        }
    }
    // Where the app comes from (owner, 2026-10-31): the download page (github: updates itself from GitHub Releases,
    // radiance-kasama#1) or Google Play (play: Play updates it; Play forbids an app updating itself and the
    // install-packages permission), both the same package and the same signer
    flavorDimensions += "store"
    productFlavors {
        create("github") { dimension = "store" }
        create("play") { dimension = "store" }
    }
    buildTypes {
        // a test build beside the real app (its own name): it can only link to the demo and the test shop
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (signing.isNotEmpty()) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    packaging {
        resources {
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/AL2.0", "META-INF/LGPL2.1", "META-INF/*.kotlin_module")
        }
    }
}

dependencies {
    val compose = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(compose)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    implementation("androidx.camera:camera-camera2:1.6.2")
    implementation("androidx.camera:camera-lifecycle:1.6.2")
    implementation("androidx.camera:camera-view:1.6.2")
    // barcodes and QR codes read on the phone, offline: ML Kit's bundled model (no Google Play download)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    // the shop's alerts on the phone (radiance-pos#175): Firebase Cloud Messaging only, no analytics
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-messaging")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17") // the screens drawn on the build server, to look at
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
