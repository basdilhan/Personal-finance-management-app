plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.team.financeapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.team.financeapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "V2026"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.auth)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.work.runtime)
    implementation(libs.concurrent.futures)
    implementation(libs.guava)
    implementation(libs.biometric)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}