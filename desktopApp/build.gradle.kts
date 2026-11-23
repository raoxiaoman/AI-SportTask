plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

// 打包插件的 jlink 流程需递归删除构建临时目录。某些受限开发环境（如带沙箱的 AI 工具）
// 会拦截项目目录内的递归删除，此时可用 -PdesktopBuildDir=/tmp/sporttask-desktop-build
// 把构建输出挪到系统临时区规避；正常环境与 CI 用默认 build/ 即可。
val desktopBuildDir: String? = findProperty("desktopBuildDir") as String?
if (desktopBuildDir != null) {
    layout.buildDirectory.set(file(desktopBuildDir))
}

// 桌面安装包要求纯数字 MAJOR.MINOR.PATCH；CI 由 tag 注入，本地默认 1.0.0
val appVersionName: String = findProperty("app.versionName") as String? ?: "1.0.0"

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "SportTask"
            packageVersion = appVersionName
            description = "康复训练与运动打卡助手"
            vendor = "RaoHui"
            includeAllModules = true

            macOS {
                bundleID = "com.raohui.sporttask"
            }
        }
    }
}
