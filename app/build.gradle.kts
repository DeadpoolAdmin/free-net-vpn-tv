import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.freevpn.androidtv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.freevpn.androidtv"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // FreeNet backend root (production HTTPS endpoint).
        buildConfigField("String", "BASE_URL", "\"https://api.freenetvpn.com/\"")

        // When true the app uses SimulatedTunnel (no real WireGuard server needed). Set to false to
        // run the real WireGuard tunnel via GoBackendTunnel.
        buildConfigField("boolean", "USE_SIMULATED_TUNNEL", "false")
    }

    buildTypes {
        debug {
            // Keep the simulated tunnel + a noisy log level for development.
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            // Strip unused resources alongside R8's code shrinking — smaller APK and less to load on
            // low-end Android 8 devices.
            isShrinkResources = true
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Target JVM 17 bytecode while building on whatever JDK runs Gradle (the JBR 21 pinned via
// org.gradle.java.home). We intentionally do NOT use jvmToolchain(17) because that would make Gradle
// hunt for a separate JDK 17 install, which this machine doesn't have.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }
}

dependencies {
    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose (BOM-governed)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Compose for TV
    implementation(libs.androidx.tv.material)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Storage
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // WireGuard production tunnel path — used by GoBackendTunnel. Bundles the wireguard-go native
    // libs and the config parser (com.wireguard.config.*).
    implementation(libs.wireguard.tunnel)
}
