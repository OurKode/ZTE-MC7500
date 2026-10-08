import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { load(it) }
    }
}

val versionMajor = (versionProps.getProperty("VERSION_MAJOR") ?: "1").toInt()
val versionMinor = (versionProps.getProperty("VERSION_MINOR") ?: "2").toInt()
val versionPatch = (versionProps.getProperty("VERSION_PATCH") ?: "0").toInt()

fun getGitCommitCount(): Int {
    return try {
        providers.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
        }.standardOutput.asText.get().trim().toIntOrNull() ?: 1
    } catch (e: Exception) {
        1
    }
}

fun getGitShortCommitHash(): String {
    return try {
        providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()
    } catch (e: Exception) {
        "dev"
    }
}

val autoVersionCode = getGitCommitCount()
val autoVersionName = "$versionMajor.$versionMinor.$versionPatch"

android {
    namespace = "com.example.odumonitor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.odumonitor"
        minSdk = 26
        targetSdk = 34
        versionCode = autoVersionCode
        versionName = autoVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GIT_HASH", "\"${getGitShortCommitHash()}\"")
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = project.findProperty("KEYSTORE_PASSWORD") as? String ?: "android"
                keyAlias = project.findProperty("KEY_ALIAS") as? String ?: "release"
                keyPassword = project.findProperty("KEY_PASSWORD") as? String ?: "android"
            } else {
                // Fallback otomatis ke debug keystore agar release build langsung dapat diinstall dan diuji
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.register("printVersion") {
    doLast {
        println("Aplikasi: com.example.odumonitor")
        println("Version Name: $autoVersionName")
        println("Version Code: $autoVersionCode")
        println("Git Commit Hash: ${getGitShortCommitHash()}")
    }
}

tasks.register("bumpPatch") {
    doLast {
        val newPatch = versionPatch + 1
        versionProps.setProperty("VERSION_PATCH", newPatch.toString())
        FileOutputStream(versionPropsFile).use { versionProps.store(it, "Updated by gradle bumpPatch") }
        println("Bumped version to $versionMajor.$versionMinor.$newPatch")
    }
}

tasks.register("bumpMinor") {
    doLast {
        val newMinor = versionMinor + 1
        versionProps.setProperty("VERSION_MINOR", newMinor.toString())
        versionProps.setProperty("VERSION_PATCH", "0")
        FileOutputStream(versionPropsFile).use { versionProps.store(it, "Updated by gradle bumpMinor") }
        println("Bumped version to $versionMajor.$newMinor.0")
    }
}

tasks.register("bumpMajor") {
    doLast {
        val newMajor = versionMajor + 1
        versionProps.setProperty("VERSION_MAJOR", newMajor.toString())
        versionProps.setProperty("VERSION_MINOR", "0")
        versionProps.setProperty("VERSION_PATCH", "0")
        FileOutputStream(versionPropsFile).use { versionProps.store(it, "Updated by gradle bumpMajor") }
        println("Bumped version to $newMajor.0.0")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose UI & Material 3
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Glance (Homescreen Widget)
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.glance:glance-material3:1.0.0")

    // Networking & JSON Parsing
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Background Processing
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
