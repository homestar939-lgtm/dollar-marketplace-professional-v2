plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

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
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    val apiBaseUrl = providers.gradleProperty("API_BASE_URL").orElse("http://10.0.2.2:4000/api")
    val googleWebClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID").orElse("")
    // النسخة التجريبية افتراضية؛ لتعطيلها: -PDEMO_MODE=false
    val demoMode = providers.gradleProperty("DEMO_MODE").orElse("true")
    buildTypes.configureEach {
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.get()}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${googleWebClientId.get()}\"")
        buildConfigField("Boolean", "DEMO_MODE", demoMode.get())
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
