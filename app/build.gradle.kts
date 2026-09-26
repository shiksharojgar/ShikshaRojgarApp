plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.shiksharojgar.app"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach { kotlinOptions.jvmTarget = "17" }

    defaultConfig {
        applicationId = "com.shiksharojgar.app"
        minSdk = 23
        targetSdk = 35
        // IMPORTANT: every future release must use the same applicationId and signing key.
        versionCode = 25
        versionName = "1.7.0"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("SHIKSHA_KEYSTORE_FILE")
            val storePasswordEnv = System.getenv("SHIKSHA_KEYSTORE_PASSWORD")
            val keyAliasEnv = System.getenv("SHIKSHA_KEY_ALIAS")
            val keyPasswordEnv = System.getenv("SHIKSHA_KEY_PASSWORD")
            if (!storeFilePath.isNullOrBlank() && !storePasswordEnv.isNullOrBlank() && !keyAliasEnv.isNullOrBlank() && !keyPasswordEnv.isNullOrBlank()) {
                storeFile = file(storeFilePath)
                storePassword = storePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (System.getenv("SHIKSHA_KEYSTORE_FILE") != null) signingConfig = signingConfigs.getByName("release")
        }
    }
}


dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
