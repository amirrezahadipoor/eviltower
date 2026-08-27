import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// --------------------------------------------------------------------------------------
// Release signing.
// The real keystore is NEVER committed. Values are read from (in order of precedence):
//   1. keystore.properties in the project root (git-ignored, see keystore.properties.example)
//   2. environment variables (used by the GitHub Actions workflow)
// If neither exists, the release build falls back to the debug signing config so that the
// project always builds with zero manual fixes.
// --------------------------------------------------------------------------------------
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

fun secret(key: String, env: String): String? =
    (keystoreProps.getProperty(key) ?: System.getenv(env))?.takeIf { it.isNotBlank() }

val storeFilePath = secret("storeFile", "EVILTOWER_STORE_FILE")
val storePasswordValue = secret("storePassword", "EVILTOWER_STORE_PASSWORD")
val keyAliasValue = secret("keyAlias", "EVILTOWER_KEY_ALIAS")
val keyPasswordValue = secret("keyPassword", "EVILTOWER_KEY_PASSWORD")
val hasReleaseSigning = storeFilePath != null && rootProject.file(storeFilePath).exists()

android {
    namespace = "ir.hadipoor.eviltower"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.hadipoor.eviltower"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        resourceConfigurations += listOf("fa", "en")
        // RSA public key of the Cafe Bazaar developer console (empty -> local security check off).
        buildConfigField(
            "String",
            "BAZAAR_RSA_KEY",
            "\"${System.getenv("BAZAAR_RSA_KEY") ?: ""}\"",
        )
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(storeFilePath!!)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    // Cafe Bazaar in-app billing (NOT Google Play Billing).
    implementation(libs.poolakey)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
