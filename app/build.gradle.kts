plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ===== 固定签名配置 =====
// CI 通过环境变量注入 keystore；本地没有则退回 debug 签名，保证都能构建。
val ksPath = System.getenv("KEYSTORE_PATH")
val ksPass = System.getenv("KEYSTORE_PASSWORD")
val keyAliasEnv = System.getenv("KEY_ALIAS")
val keyPass = System.getenv("KEY_PASSWORD")
val hasFixedKey = ksPath != null && File(ksPath).exists() && ksPass != null &&
        keyAliasEnv != null && keyPass != null

android {
    namespace = "com.kernelsu.mgr"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kernelsu.mgr"
        minSdk = 29
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
    }

    signingConfigs {
        if (hasFixedKey) {
            create("fixed") {
                storeFile = File(ksPath!!)
                storePassword = ksPass
                keyAlias = keyAliasEnv
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        debug {
            if (hasFixedKey) {
                signingConfig = signingConfigs.getByName("fixed")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasFixedKey) {
                signingConfig = signingConfigs.getByName("fixed")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        jniLibs {
            // proot 以 .so 形式打包，需要解压到 nativeLibraryDir 才能执行
            useLegacyPackaging = true
        }
    }
    androidResources {
        noCompress += "so"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
}