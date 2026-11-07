plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

val apiBaseUrlProperty = providers.gradleProperty("API_BASE_URL")
val apiBaseUrl = apiBaseUrlProperty.orElse("http://10.0.2.2:4000/api")
val googleWebClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID").orElse("")
val demoMode = providers.gradleProperty("DEMO_MODE").orElse("false")

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
            create("production") {
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
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("production")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes.configureEach {
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.get()}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${googleWebClientId.get()}\"")
        buildConfigField("Boolean", "DEMO_MODE", demoMode.get())
    }
}

val verifyProductionReleaseConfig = tasks.register("verifyProductionReleaseConfig") {
    group = "verification"
    description = "Rejects release artifacts that lack production endpoints, Google Sign-In, or signing credentials."
    doLast {
        val problems = mutableListOf<String>()
        val productionApi = apiBaseUrlProperty.orNull?.trim()
        if (productionApi.isNullOrBlank() || !productionApi.startsWith("https://")) {
            problems += "Set -PAPI_BASE_URL to the production HTTPS API base URL"
        }
        if (googleWebClientId.get().isBlank()) {
            problems += "Set -PGOOGLE_WEB_CLIENT_ID to the production Google OAuth Web client ID"
        }
        if (demoMode.get() != "false") problems += "Set -PDEMO_MODE=false for release builds"
        if (!hasReleaseSigning) {
            problems += "Provide RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, and RELEASE_KEY_PASSWORD"
        }
        if (problems.isNotEmpty()) throw GradleException("Production release blocked: ${problems.joinToString("; ")}")
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
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("io.coil-kt.coil3:coil-compose:3.6.2")
    implementation("io.coil-kt.coil3:coil-network-ktor3:3.6.2")
    implementation("io.ktor:ktor-client-android:3.5.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.5.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}
