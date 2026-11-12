import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.reelfree"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.reelfree"
        minSdk = 29
        targetSdk = 35
        versionCode = 4
        versionName = "0.2.0-phase1"
    }

    // Release signing: CI reads REELFREE_* environment variables (from GitHub secrets);
    // locally it reads ~/.reelfree/keystore.properties. Without either, the debug key is used.
    // A stable key matters: Android refuses to update an app signed with a different key.
    val keystoreProps = Properties().apply {
        val f = File(System.getProperty("user.home"), ".reelfree/keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    fun signingValue(env: String, prop: String): String? =
        (System.getenv(env)?.takeIf { it.isNotBlank() } ?: keystoreProps.getProperty(prop))?.trim()
    val releaseStoreFile = signingValue("REELFREE_KEYSTORE_PATH", "storeFile")?.let { file(it) }

    signingConfigs {
        if (releaseStoreFile != null && releaseStoreFile.exists()) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = signingValue("REELFREE_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("REELFREE_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("REELFREE_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // ./gradlew testDebugUnitTest -Pscreenshots renders UI screenshots to app/build/screenshots
            it.systemProperty("reelfree.screenshots", project.findProperty("screenshots")?.toString() ?: "")
            it.systemProperty("reelfree.screenshotDir", layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // unused icons are removed by R8
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
