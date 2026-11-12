plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":shared"))
            }
        }
    }
}

// 版本号由 CI 从 tag 注入（-Papp.versionName / -Papp.versionCode），本地默认开发版。
val appVersionName: String = findProperty("app.versionName") as String? ?: "1.0.0-dev"
val appVersionCode: Int = (findProperty("app.versionCode") as String?)?.toIntOrNull() ?: 1

// release 签名只在提供密钥参数时启用；CI 通过 ORG_GRADLE_PROJECT_* 环境变量注入，不落到命令行。
val keystorePath = findProperty("keystorePath") as String?
val keystoreStorePassword = findProperty("keystoreStorePassword") as String?
val keystoreKeyAlias = findProperty("keystoreKeyAlias") as String?
val keystoreKeyPassword = findProperty("keystoreKeyPassword") as String?
val hasReleaseSigning = listOf(
    keystorePath, keystoreStorePassword, keystoreKeyAlias, keystoreKeyPassword
).all { !it.isNullOrBlank() }

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.raohui.sporttask"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        applicationId = "com.raohui.sporttask"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }
    buildFeatures {
        buildConfig = true
    }
    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(keystorePath!!)
                storePassword = keystoreStorePassword
                keyAlias = keystoreKeyAlias
                keyPassword = keystoreKeyPassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            // 未配置 R8 规则前保持不混淆，避免 Compose/SQLDelight 反射被裁掉。
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}
