package ui

/** 桌面版本号：由打包时注入系统属性 app.versionName，本地运行默认开发版 */
actual fun appVersionName(): String = System.getProperty("app.versionName", "1.0.0-dev")
