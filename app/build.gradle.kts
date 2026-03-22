plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
    // alias(libs.plugins.sentry)
}

android {
    namespace = "com.amakaflow.companion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.amakaflow.companion"
        minSdk = 28
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Build config fields for API endpoints
        buildConfigField("String", "MAPPER_API_URL_PROD", "\"https://mapper-api.staging.amakaflow.com\"")
        buildConfigField("String", "MAPPER_API_URL_STAGING", "\"https://mapper-api.staging.amakaflow.com\"")
        buildConfigField("String", "MAPPER_API_URL_DEV", "\"http://10.0.2.2:8001\"")
        buildConfigField("String", "INGESTOR_API_URL_PROD", "\"https://workout-ingestor-api.staging.amakaflow.com\"")
        buildConfigField("String", "INGESTOR_API_URL_STAGING", "\"https://workout-ingestor-api.staging.amakaflow.com\"")
        buildConfigField("String", "INGESTOR_API_URL_DEV", "\"http://10.0.2.2:8002\"")
        buildConfigField("String", "MCP_API_URL_PROD", "\"https://amakaflow-mcp.onrender.com\"")
        buildConfigField("String", "MCP_API_URL_STAGING", "\"https://amakaflow-mcp.onrender.com\"")
        buildConfigField("String", "MCP_API_URL_DEV", "\"http://10.0.2.2:8000\"")
        
        // Sentry DSN - configured per build variant via environment or build config
        val sentryDsn = System.getenv("SENTRY_DSN")?.takeIf { it.isNotBlank() }
            ?: "https://placeholder@o1.ingest.sentry.io/0"
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "DEFAULT_ENVIRONMENT", "\"PRODUCTION\"")
        }
        debug {
            isMinifyEnabled = false
            // Use STAGING to match device behavior (same backend as release)
            buildConfigField("String", "DEFAULT_ENVIRONMENT", "\"STAGING\"")
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
    }
}

kover {
    reports {
        filters {
            excludes {
                // Exclude generated code
                classes(
                    "*_Factory",
                    "*_Factory\$*",
                    "*_HiltModules*",
                    "*Hilt_*",
                    "*_Impl",
                    "*_Impl\$*",
                    "*.BuildConfig",
                    "*_MembersInjector",
                    "*Module_*",
                    "*Dagger*",
                    "*_GeneratedInjector"
                )
                // Exclude DI module
                packages(
                    "*.di",
                    "dagger.hilt.*",
                    "hilt_aggregated_deps"
                )
            }
        }

        // Note: Coverage verification is disabled for now
        // The reports show ~2% coverage which is expected for the current test suite
        // Verification can be enabled once coverage reaches a meaningful threshold
    }
}

dependencies {
    // Shared module (phone <-> watch data contracts)
    implementation(project(":shared"))

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)

    // Data Storage
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // Kotlin Extensions
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)

    // Image Loading
    implementation(libs.coil.compose)

    // Accompanist
    implementation(libs.accompanist.permissions)

    // Camera & QR Scanning
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.zxing.core)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // WorkManager
    implementation(libs.workmanager.runtime)
    implementation(libs.workmanager.hilt)
    ksp(libs.workmanager.hilt.compiler)

    // Testing - JVM
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.okhttp.mockwebserver)

    // Testing - Instrumented
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.workmanager.testing)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.mockk.android)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Sentry for error tracking
    implementation(libs.sentry.android)

    // Wear OS DataLayer communication
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
}

// Configure Sentry
// sentry {
    // Upload ProGuard mappings and source context for readable stack traces
//     includeSourceContext = true
//     org = "amakaflow"
//     projectName = "amakaflow-android-app"
    
    // Auth token should be set via SENTRY_AUTH_TOKEN environment variable
    // For CI/CD, use GitHub Actions secrets
// }
