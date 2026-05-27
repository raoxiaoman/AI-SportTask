plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("app.cash.sqldelight")
}

// ===== 服务器配置自动生成 =====
val serverConfigDir = layout.buildDirectory.dir("generated/serverConfig")
val localPropFile = rootProject.file("local.properties")
val apiBaseUrl = if (localPropFile.exists()) {
    localPropFile.readLines()
        .firstOrNull { it.startsWith("api.baseUrl=") }
        ?.substringAfter("=")
        ?.trim()
        ?: "http://localhost:3456"
} else {
    "http://localhost:3456"
}

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(serverConfigDir)
        }
    }

    androidTarget("android")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
                implementation("app.cash.sqldelight:runtime:2.0.2")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
                // Ktor HTTP client
                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                // Kotlinx Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:1.9.23")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.9.0")
                api("androidx.appcompat:appcompat:1.7.0")
                api("androidx.core:core-ktx:1.13.1")
                implementation("app.cash.sqldelight:android-driver:2.0.2")
                implementation("io.ktor:ktor-client-okhttp:2.3.12")
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("org.jetbrains.kotlin:kotlin-test:1.9.23")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.12")
            }
        }
    }
}

// 生成 ServerConfig.kt（编译前执行）
tasks.register("generateServerConfig") {
    doLast {
        val file = serverConfigDir.get().file("cloud/ServerConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
package cloud

/**
 * 服务器配置 — 由 Gradle 自动生成
 * 设置: local.properties → api.baseUrl
 * 默认: http://localhost:3456
 */
object ServerConfig {
    const val API_BASE_URL = "$apiBaseUrl"
}
""".trimStart()
        )
    }
}

// 所有编译任务依赖生成器
tasks.matching { it.name.startsWith("compileKotlin") }.configureEach {
    dependsOn("generateServerConfig")
}

tasks.named("generateServerConfig") {
    if (localPropFile.exists()) {
        inputs.file(localPropFile)
    }
    outputs.dir(serverConfigDir)
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.raohui.sporttask.common"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

sqldelight {
    databases {
        create("SportTaskDatabase") {
            packageName.set("com.raohui.sporttask.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight"))
        }
    }
}
