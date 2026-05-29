// :app — Android UI (Compose). Native sloj, ne dijeli se s iOS-om.

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "hr.zet.transit"
    compileSdk = 34

    defaultConfig {
        applicationId = "hr.zet.transit"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            // Emulator → backend na host stroju (services/transit-api lokalno).
            buildConfigField("String", "BACKEND_URL", "\"http://10.0.2.2:8080\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Produkcijski backend URL — override iz Gradle propertyja
            // (-PbackendUrl=...) ili CI secreta; placeholder kao zadani.
            val backendUrl = (project.findProperty("backendUrl") as String?)
                ?: "https://api.zet-transit.example"
            buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")
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
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.maplibre.android)

    testImplementation(libs.junit)
}
