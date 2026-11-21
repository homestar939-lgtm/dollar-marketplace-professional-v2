plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

val apiBaseUrl = providers.gradleProperty("API_BASE_URL").orElse("")
val releaseStoreFileProperty = providers.gradleProperty("RELEASE_STORE_FILE")
val releaseStorePasswordProperty = providers.gradleProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAliasProperty = providers.gradleProperty("RELEASE_KEY_ALIAS")
val releaseKeyPasswordProperty = providers.gradleProperty("RELEASE_KEY_PASSWORD")
val releaseStoreFile = releaseStoreFileProperty.orNull?.let { file(it) }
val hasReleaseSigning = releaseStoreFile?.isFile == true &&
    !releaseStorePasswordProperty.orNull.isNullOrBlank() &&
    !releaseKeyAliasProperty.orNull.isNullOrBlank() &&
    !releaseKeyPasswordProperty.orNull.isNullOrBlank()

android {
    namespace = "com.dollar.marketplace"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.dollar.marketplace"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }
    if (hasReleaseSigning) {
        signingConfigs {
            create("lanRelease") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePasswordProperty.orNull
                keyAlias = releaseKeyAliasProperty.orNull
                keyPassword = releaseKeyPasswordProperty.orNull
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("lanRelease")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes.configureEach {
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.get()}\"")
        buildConfigField("Boolean", "DEMO_MODE", "false")
    }
}

val verifyProductionReleaseConfig = tasks.register("verifyProductionReleaseConfig") {
    group = "verification"
    description = "Requires a non-loopback HTTP(S) API URL and private release signing credentials."
    doLast {
        val address = apiBaseUrl.get().trim()
        val isHttpLan = address.startsWith("http://") &&
            !address.contains("localhost", ignoreCase = true) &&
            !address.contains("127.0.0.1") && !address.contains("10.0.2.2")
        val isHttps = address.startsWith("https://")
        val problems = mutableListOf<String>()
        if (!isHttpLan && !isHttps) problems += "Set -PAPI_BASE_URL to a LAN HTTP or HTTPS API URL (not localhost/emulator loopback)"
        if (!hasReleaseSigning) problems += "Provide RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, and RELEASE_KEY_PASSWORD"
        if (problems.isNotEmpty()) throw GradleException("LAN release blocked: ${problems.joinToString("; ")}")
    }
}

tasks.configureEach {
    if (name.contains("Release") &&
        (name.startsWith("assemble") || name.startsWith("bundle") || name.startsWith("package"))) {
        dependsOn(verifyProductionReleaseConfig)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.9.4")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("io.coil-kt.coil3:coil-compose:3.6.2")
    implementation("io.coil-kt.coil3:coil-network-ktor3:3.6.2")
    implementation("io.ktor:ktor-client-android:3.5.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.5.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}
