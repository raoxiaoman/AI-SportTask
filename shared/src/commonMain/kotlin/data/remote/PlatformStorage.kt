package data.remote

/**
 * 平台存储接口
 * Android: SharedPreferences
 * iOS: NSUserDefaults
 */
interface PlatformStorage {
    fun save(key: String, value: String)
    fun load(key: String): String?
    fun remove(key: String)
}

/**
 * 平台存储提供者 — 各平台在入口处初始化
 */
object PlatformStorageProvider {
    var instance: PlatformStorage? = null

    fun init(storage: PlatformStorage) {
        instance = storage
    }

    fun get(): PlatformStorage = instance
        ?: throw IllegalStateException("PlatformStorage not initialized. Call PlatformStorageProvider.init() from platform entry point.")
}
