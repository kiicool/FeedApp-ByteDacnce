plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.feedapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.feedapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
    implementation(libs.androidx.swiperefreshlayout)
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    annotationProcessor("com.github.bumptech.glide:compiler:5.0.5")
}