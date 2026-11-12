package data.plan

import android.content.Context
import db.appContext
import db.isDbContextReady

/**
 * 把今日任务与计划元信息写入 NotificationHelper 使用的 prefs（sport_task_prefs）。
 * 通知触发时若当天未打开 App，NotificationHelper 会用 startDate 自行计算进度文案。
 */
actual fun publishReminderPreview(
    text: String,
    startDate: String,
    totalDays: Int,
    generatedDate: String
) {
    if (!isDbContextReady()) return
    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_PREVIEW, text)
        .putString(KEY_PREVIEW_DATE, generatedDate)
        .putString(KEY_PLAN_START, startDate)
        .putInt(KEY_PLAN_TOTAL_DAYS, totalDays)
        .apply()
}

private const val PREFS_NAME = "sport_task_prefs"
private const val KEY_PREVIEW = "reminder_preview"
private const val KEY_PREVIEW_DATE = "reminder_preview_date"
private const val KEY_PLAN_START = "plan_start_date"
private const val KEY_PLAN_TOTAL_DAYS = "plan_total_days"
