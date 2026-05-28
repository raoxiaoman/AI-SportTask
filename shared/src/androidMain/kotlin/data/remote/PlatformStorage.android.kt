package data.remote

import android.content.Context
import android.content.SharedPreferences

/**
 * Android 平台存储 — SharedPreferences
 */
class AndroidPlatformStorage(context: Context) : PlatformStorage {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun save(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun load(key: String): String? {
        return prefs.getString(key, null)
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    companion object {
        private const val PREFS_NAME = "sporttask_prefs"
    }
}
