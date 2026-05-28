package data.remote

import platform.Foundation.NSUserDefaults

/**
 * iOS 平台存储 — NSUserDefaults
 */
class IosPlatformStorage : PlatformStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun save(key: String, value: String) {
        defaults.setObject(value, forKey = key)
        defaults.synchronize()
    }

    override fun load(key: String): String? {
        return defaults.stringForKey(key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
        defaults.synchronize()
    }
}
