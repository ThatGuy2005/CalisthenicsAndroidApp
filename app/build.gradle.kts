import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

// Release signing: the upload key's file and passwords come from keystore.properties in the
// project root, which git ignores (README, "Releasing"). Without that file, release builds are
// left unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
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
    }

    signingConfigs {
        if (!keystoreProperties.isEmpty) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            // R8: shrinks and obfuscates the code and drops unused resources. Our own keep
            // rules are in src/main/keepRules; the libraries bring theirs.
            optimization {
                enable = true
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

// Javadoc: ./gradlew javadoc writes the API documentation of our Java code to
// app/build/docs/javadoc (open index.html). It is built against the debug variant's classpath,
// so the Android, AndroidX and Firebase types in our signatures resolve.
androidComponents {
    onVariants(selector().withName("debug")) { variant ->
        tasks.register<Javadoc>("javadoc") {
            description = "Generates the Javadoc of the app's Java sources."
            group = "documentation"
            source = fileTree("src/main/java") { include("**/*.java") }
            classpath = files(androidComponents.sdkComponents.bootClasspath) +
                    variant.compileClasspath
            setDestinationDir(layout.buildDirectory.dir("docs/javadoc").get().asFile)
            title = "CalisthenicsApp ${android.defaultConfig.versionName}"
            (options as StandardJavadocDocletOptions).apply {
                encoding = "UTF-8"
                docEncoding = "UTF-8"
                charSet = "UTF-8"
                // Package-private classes and members too: most of the back end is.
                memberLevel = JavadocMemberLevel.PACKAGE
                addStringOption("Xdoclint:all,-missing", "-quiet")
            }
        }
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.dotlottie.android)
    implementation(libs.awebp)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
