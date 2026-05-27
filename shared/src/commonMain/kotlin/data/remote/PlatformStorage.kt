package data.remote

/**
 * 平台存储接口 — 各平台实现
 * Android: SharedPreferences
 * iOS: NSUserDefaults
 */
expect class PlatformStorage {
    fun save(key: String, value: String)
    fun load(key: String): String?
    fun remove(key: String)
}
