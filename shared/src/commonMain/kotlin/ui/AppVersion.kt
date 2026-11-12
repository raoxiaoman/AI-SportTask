package ui

/**
 * 当前应用版本号。Android 读 BuildConfig，iOS 读 Info.plist；
 * 版本号由 CI 从 tag 注入，本地开发默认 1.0.0-dev。
 */
expect fun appVersionName(): String
