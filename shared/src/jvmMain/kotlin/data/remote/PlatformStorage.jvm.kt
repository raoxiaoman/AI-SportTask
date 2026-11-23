package data.remote

import java.io.File
import java.util.Properties

/**
 * 桌面端平台存储 —— 用用户目录下的 properties 文件持久化。
 * 位置：~/.sporttask/sporttask_prefs.properties
 * 与 Android SharedPreferences / iOS NSUserDefaults 语义一致（key-value 字符串）。
 */
class JvmPlatformStorage : PlatformStorage {
    private val dir = File(System.getProperty("user.home"), ".sporttask")
    private val file = File(dir, "sporttask_prefs.properties")
    private val props = Properties()

    init {
        runCatching {
            dir.mkdirs()
            if (file.exists()) file.inputStream().use { props.load(it) }
        }
    }

    @Synchronized
    override fun save(key: String, value: String) {
        props.setProperty(key, value)
        flush()
    }

    @Synchronized
    override fun load(key: String): String? = props.getProperty(key)

    @Synchronized
    override fun remove(key: String) {
        props.remove(key)
        flush()
    }

    private fun flush() {
        runCatching { file.outputStream().use { props.store(it, "SportTask preferences") } }
    }
}
