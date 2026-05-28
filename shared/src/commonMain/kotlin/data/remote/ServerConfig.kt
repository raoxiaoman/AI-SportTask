package data.remote

/**
 * 后端服务器配置。
 *
 * 地址不硬编码在源码里，从本地配置文件读取。
 * 配置文件不会提交到 git。
 *
 * 首次使用时用户手动配置，之后自动从本地存储恢复。
 */
object ServerConfig {
    var baseUrl: String = "http://localhost:3456"
        private set

    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
        // 持久化到本地存储
        try {
            val storage = PlatformStorageProvider.get()
            storage.save("sporttask_server_url", baseUrl)
        } catch (_: Exception) {
            // 存储未初始化时忽略
        }
        println("[ServerConfig] baseUrl = $baseUrl")
    }

    /**
     * 从本地存储恢复服务器地址
     */
    fun restoreFromStorage(): Boolean {
        return try {
            val storage = PlatformStorageProvider.get()
            val saved = storage.load("sporttask_server_url")
            if (!saved.isNullOrBlank()) {
                baseUrl = saved.trimEnd('/')
                println("[ServerConfig] restored: $baseUrl")
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
