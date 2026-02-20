import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.util.Properties

val localProps = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}

fun localProp(name: String): String? = localProps.getProperty(name)

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
        viewBinding = true
    }
}

dependencies {
    implementation(project(":simulator"))
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