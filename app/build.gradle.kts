plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.calisthenicsapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.calisthenicsapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        // Robolectric tests read schema.sql and seed.sql from the assets.
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Robolectric reaches into JDK internals for file descriptors and loads native
            // SQLite; newer JDKs only allow that when asked.
            it.jvmArgs(
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                "--enable-native-access=ALL-UNNAMED"
            )
        }
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation("com.github.LottieFiles:dotlottie-android:0.5.0")
    implementation("com.github.penfeizhou.android.animation:awebp:3.0.5")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
