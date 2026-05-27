package data.remote

import platform.Foundation.NSUserDefaults

/**
 * iOS 平台存储 — 使用 NSUserDefaults
 */
actual class PlatformStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun save(key: String, value: String) {
        defaults.setObject(value, forKey = key)
        defaults.synchronize()
    }

    actual fun load(key: String): String? {
        return defaults.stringForKey(key)
    }

    actual fun remove(key: String) {
        defaults.removeObjectForKey(key)
        defaults.synchronize()
    }
}
