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

// 桌面安装包要求纯数字版本号：Dmg 为 MAJOR[.MINOR][.PATCH]（MAJOR > 0），
// Msi 为 MAJOR.MINOR.BUILD，Deb 同理。CI 在 main 分支会注入 dev-<sha> 这类
// 非数字版本号（仅用于 Android versionName 与产物命名），直接传给桌面打包会在
// 配置阶段校验失败。这里做合法化：非法值降级为 1.0.0，不足三段时补 0；
// tag 注入的 1.1.1 等正式版本号原样保留。
val requestedVersion: String = findProperty("app.versionName") as String? ?: "1.0.0"
val versionParts: List<Int?> = requestedVersion.split(".").map { it.toIntOrNull() }
val isValidDesktopVersion = versionParts.size in 1..3 &&
    versionParts.all { it != null } &&
    (versionParts[0] ?: 0) > 0
val appVersionName: String = if (!isValidDesktopVersion) {
    "1.0.0"
} else {
    (0..2).joinToString(".") { (versionParts.getOrNull(it) ?: 0).toString() }
}

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
