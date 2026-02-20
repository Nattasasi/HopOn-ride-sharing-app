import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.util.Properties

val localProps = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}

fun localProp(name: String): String? = localProps.getProperty(name)
val useMockData = (localProp("mockMode") ?: "true").toBoolean()

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.tritech.hopon"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tritech.hopon"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "USE_MOCK_DATA", useMockData.toString())
            resValue(
                "string",
                "google_maps_key",
                localProp("apiKey") ?: ""
            )
            resValue(
                "string",
                "routes_api_key",
                localProp("routesApiKey") ?: ""
            )
        }
        release {
            buildConfigField("boolean", "USE_MOCK_DATA", useMockData.toString())
            resValue(
                "string",
                "google_maps_key",
                localProp("apiKey") ?: ""
            )
            resValue(
                "string",
                "routes_api_key",
                localProp("routesApiKey") ?: ""
            )
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }
}

dependencies {
    implementation(project(":simulator"))
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("com.google.maps.android:maps-compose:5.0.3") {
        exclude(group = "com.google.android.gms", module = "play-services-maps")
    }
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // Places Library (required for search places)
    implementation("com.google.android.libraries.places:places:3.3.0") {
        exclude(group = "com.google.android.gms", module = "play-services-maps")
    }
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.libraries.navigation:navigation:latest.release")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}