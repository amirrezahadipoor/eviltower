import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val keystoreFile = rootProject.file("keystore.properties")
val keystore = Properties().apply { if (keystoreFile.exists()) keystoreFile.inputStream().use(::load) }
fun secret(property: String, env: String) = (keystore.getProperty(property) ?: System.getenv(env)).orEmpty()
val releaseStore = secret("storeFile", "EVILTOWER_STORE_FILE")
val releasePassword = secret("storePassword", "EVILTOWER_STORE_PASSWORD")
val releaseAlias = secret("keyAlias", "EVILTOWER_KEY_ALIAS")
val releaseKeyPassword = secret("keyPassword", "EVILTOWER_KEY_PASSWORD")
val hasReleaseKey = releaseStore.isNotBlank() && rootProject.file(releaseStore).exists()

android {
    namespace = "ir.hadipoor.eviltower"
    compileSdk = 35
    defaultConfig {
        applicationId = "ir.hadipoor.eviltower"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "BAZAAR_RSA_KEY", "\"${System.getenv("BAZAAR_RSA_KEY") ?: ""}\"")
    }
    signingConfigs {
        if (hasReleaseKey) create("release") {
            storeFile = rootProject.file(releaseStore)
            storePassword = releasePassword
            keyAlias = releaseAlias
            keyPassword = releaseKeyPassword
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }
    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    lint { abortOnError = false; checkReleaseBuilds = false }
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
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
