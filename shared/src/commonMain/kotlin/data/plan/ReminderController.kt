package data.plan

/** 每日提醒配置快照（默认 21:00，与康复计划提醒一致） */
data class ReminderConfig(
    val enabled: Boolean = false,
    val hour: Int = 21,
    val minute: Int = 0
)

/**
 * 每日训练提醒的跨平台控制点。
 * 平台入口（Android MainActivity）注入 apply/snapshot，commonMain 的 UI 不直接依赖平台通知类。
 */
object ReminderController {
    var apply: ((enabled: Boolean, hour: Int, minute: Int) -> Unit)? = null
    var snapshot: (() -> ReminderConfig)? = null

    fun setEnabled(enabled: Boolean, hour: Int, minute: Int) {
        apply?.invoke(enabled, hour, minute)
    }

    fun current(): ReminderConfig =
        runCatching { snapshot?.invoke() }.getOrNull() ?: ReminderConfig()
}
