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

val releaseKeystorePath = providers.environmentVariable("KEYSTORE_PATH").orNull
val releaseStorePassword = providers.environmentVariable("STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("KEY_PASSWORD").orNull

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

        ndk {
            abiFilters.addAll(
                listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            )
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    // Release signing values are optional during project sync and debug builds.
    signingConfigs {
        create("release") {
            releaseKeystorePath?.let { storeFile = file(it) }
            releaseStorePassword?.let { storePassword = it }
            releaseKeyAlias?.let { keyAlias = it }
            releaseKeyPassword?.let { keyPassword = it }
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

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

gradle.taskGraph.whenReady {
    val releasePackagingTasks = allTasks.filter { task ->
        val taskName = task.name.lowercase(Locale.ROOT)
        taskName.contains("release") &&
            listOf("assemble", "install", "bundle", "package")
                .any(taskName::startsWith)
    }

    if (releasePackagingTasks.isNotEmpty()) {
        val missingValues = mutableListOf<String>().apply {
            if (releaseKeystorePath.isNullOrBlank()) add("KEYSTORE_PATH")
            if (releaseStorePassword.isNullOrBlank()) add("STORE_PASSWORD")
            if (releaseKeyAlias.isNullOrBlank()) add("KEY_ALIAS")
            if (releaseKeyPassword.isNullOrBlank()) add("KEY_PASSWORD")
        }
        val keystoreFile = releaseKeystorePath?.let { project.file(it) }
        if (keystoreFile == null || !keystoreFile.isFile) {
            if ("KEYSTORE_PATH" !in missingValues) {
                missingValues.add("KEYSTORE_PATH (file does not exist)")
            }
        }

        if (missingValues.isNotEmpty()) {
            throw GradleException(
                "Release APK/AAB builds require a valid release keystore and signing credentials. " +
                    "Configure: ${missingValues.joinToString()}."
            )
        }
    }
}

/*
 * Secrets Gradle Plugin
 *
 * Firebase is NOT used by Muesic.
 * This configuration is harmless and allows .env / .env.example
 * to continue working if the project uses them.
 */
secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
    ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

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
    implementation(libs.yt.dlp.android)
    implementation(libs.yt.dlp.ffmpeg)

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

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )
    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )
    androidTestImplementation(
        libs.androidx.espresso.core
    )
    androidTestImplementation(
        libs.androidx.junit
    )
    androidTestImplementation(
        libs.androidx.runner
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )
    debugImplementation(
        libs.androidx.compose.ui.tooling
    )

    ksp(libs.androidx.room.compiler)
}
