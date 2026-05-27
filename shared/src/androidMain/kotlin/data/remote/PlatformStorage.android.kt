package data.remote

import android.content.Context
import android.content.SharedPreferences

/**
 * Android 平台存储 — 使用 SharedPreferences
 * 需要在应用入口调用 PlatformStorageAndroid.init(context)
 */
actual class PlatformStorage {
    private val prefs: SharedPreferences?
        get() = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    actual fun save(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    actual fun load(key: String): String? {
        return prefs?.getString(key, null)
    }

    actual fun remove(key: String) {
        prefs?.edit()?.remove(key)?.apply()
    }

    companion object {
        private const val PREFS_NAME = "sporttask_prefs"
        private var context: Context? = null

        fun init(ctx: Context) {
            context = ctx
        }
    }
}
