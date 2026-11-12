package data.remote

/**
 * 「先不登录 · 离线使用」偏好。
 *
 * 云同步服务端尚未部署时，用户可选择不登录直接使用本地功能
 * （分组 / 训练 / 打卡 / 统计 / 动作图解），避免被登录页挡住。
 *
 * 该标记只影响启动时的登录拦截，**不伪造登录态**：
 * `AuthService.isLoggedIn` 仍为 false，设置页显示「未登录」并提供返回登录页的入口。
 * 登录成功后标记会被清除，之后按正常登录态运行。
 */
object OfflineMode {
    private const val KEY = "sporttask_offline_mode"

    fun isEnabled(): Boolean = try {
        PlatformStorageProvider.get().load(KEY) == "true"
    } catch (_: Exception) {
        // 平台存储尚未初始化（如单元测试）时按未启用处理
        false
    }

    fun setEnabled(enabled: Boolean) {
        try {
            val storage = PlatformStorageProvider.get()
            if (enabled) storage.save(KEY, "true") else storage.remove(KEY)
        } catch (_: Exception) {
            // 存储不可用时忽略：本次会话仍可离线使用，仅不持久化
        }
    }
}
