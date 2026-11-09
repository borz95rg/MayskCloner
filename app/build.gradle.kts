plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Tagged releases use the version from v1.0.N; branch builds use GitHub's run number.
val tagVersion = System.getenv("GITHUB_REF_NAME")?.takeIf { it.matches(Regex("v1\\.0\\.[1-9]\\d*")) }?.removePrefix("v")
val buildNumber: Int = tagVersion?.substringAfterLast('.')?.toIntOrNull()
    ?: (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toIntOrNull()
    ?: 1
val buildName = tagVersion ?: "1.0.$buildNumber"
// Репозиторий, из которого приложение получает обновления (owner/repo). В Actions заполняется автоматически.
val updateRepo: String = System.getenv("GITHUB_REPOSITORY") ?: ""

android {
    namespace = "com.maysk.cloner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.maysk.cloner"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = buildName
        buildConfigField("String", "UPDATE_REPO", "\"$updateRepo\"")
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("SIGNING_STORE_FILE")
            if (!storePath.isNullOrBlank() && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            // Leave the variant unsigned when no release keystore is configured.
            signingConfig = signingConfigs.getByName("release").takeIf {
                !System.getenv("SIGNING_STORE_FILE").isNullOrBlank() &&
                    file(System.getenv("SIGNING_STORE_FILE")).exists()
            }
        }
        debug {
            applicationIdSuffix = ".debug"
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
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/*.kotlin_module",
        )
    }
}

dependencies {
    implementation(project(":design"))

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.github.sceneview:sceneview:2.2.1")
}
