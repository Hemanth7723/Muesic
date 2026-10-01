```kotlin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val gitTag = providers.environmentVariable("GIT_TAG")
    .orElse(providers.environmentVariable("GITHUB_REF_NAME"))
    .orElse(providers.gradleProperty("gitTag"))
    .getOrElse("v1.2.0")

val gitCommitHash = providers.environmentVariable("GIT_COMMIT_HASH")
    .orElse(providers.environmentVariable("GITHUB_SHA").map { it.take(7) })
    .orElse(providers.gradleProperty("gitCommitHash"))
    .getOrElse("main")

val gitCommitDate = providers.environmentVariable("GIT_COMMIT_DATE")
    .orElse(providers.gradleProperty("gitCommitDate"))
    .getOrElse(
        SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())
    )

val gitRepoUrl = providers.environmentVariable("GITHUB_REPOSITORY")
    .map { "https://github.com/$it" }
    .orElse(providers.gradleProperty("gitRepoUrl"))
    .getOrElse("https://github.com/Hemu1104/Muesic")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.example"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.aistudio.muesic.vzkxqy"
        minSdk = 24
        targetSdk = 36

        versionCode = 1
        versionName = gitTag.removePrefix("v")

        buildConfigField(
            "String",
            "GIT_TAG_OR_VERSION",
            "\"$gitTag\""
        )

        buildConfigField(
            "String",
            "GIT_COMMIT_DATE",
            "\"$gitCommitDate\""
        )

        buildConfigField(
            "String",
            "GIT_COMMIT_HASH",
            "\"$gitCommitHash\""
        )

        buildConfigField(
            "String",
            "GIT_REPO_URL",
            "\"$gitRepoUrl\""
        )

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    // ---------------------------------------------------------
    // Signing
    // ---------------------------------------------------------
    signingConfigs {

        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")

            if (keystorePath.isNullOrBlank()) {
                throw GradleException(
                    "KEYSTORE_PATH is not set. " +
                    "Release builds require a release keystore."
                )
            }

            val keystoreFile = file(keystorePath)

            if (!keystoreFile.exists()) {
                throw GradleException(
                    "Release keystore not found: ${keystoreFile.absolutePath}"
                )
            }

            val storePassword = System.getenv("STORE_PASSWORD")
            val keyAlias = "Muesic"
            val keyPassword = System.getenv("KEY_PASSWORD")

            if (storePassword.isNullOrBlank()) {
                throw GradleException(
                    "STORE_PASSWORD is not set."
                )
            }

            if (keyAlias.isNullOrBlank()) {
                throw GradleException(
                    "KEY_ALIAS is not set."
                )
            }

            if (keyPassword.isNullOrBlank()) {
                throw GradleException(
                    "KEY_PASSWORD is not set."
                )
            }

            storeFile = keystoreFile
            this.storePassword = storePassword
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
        }

        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {

        release {
            isCrunchPngs = false
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }
}

// ---------------------------------------------------------
// Secrets Gradle Plugin
// ---------------------------------------------------------
secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
    ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

// ---------------------------------------------------------
// Dependencies
// ---------------------------------------------------------
dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.coil.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    ksp(libs.androidx.room.compiler)
}
```
