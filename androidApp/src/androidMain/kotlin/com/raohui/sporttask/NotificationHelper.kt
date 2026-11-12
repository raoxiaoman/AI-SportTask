package com.raohui.sporttask

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "sport_task_reminder"
        const val CHANNEL_NAME = "训练提醒"
        const val NOTIFICATION_ID = 1001
        const val REQUEST_CODE = 1001
        const val PREFS_NAME = "sport_task_prefs"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_REMINDER_PREVIEW = "reminder_preview"
        const val KEY_REMINDER_PREVIEW_DATE = "reminder_preview_date"
        const val KEY_PLAN_START_DATE = "plan_start_date"
        const val KEY_PLAN_TOTAL_DAYS = "plan_total_days"
        private const val DEFAULT_TEXT = "今天还没训练呢？来动一动吧！"
        private const val HALF_DAY_MS = 12L * 60 * 60 * 1000
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "每天提醒您完成训练"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val savedDate = prefs.getString(KEY_REMINDER_PREVIEW_DATE, null)
        // 文案只在打开 App 时生成；隔天未打开则丢弃，避免展示昨天的完成状态
        val preview = prefs.getString(KEY_REMINDER_PREVIEW, null)
            ?.takeIf { it.isNotBlank() && savedDate == today }
        val body = preview ?: fallbackText(today)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (preview != null) "今日训练" else "训练提醒")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /** 未打开 App 时，按计划起始日算出进度文案，不依赖数据库与协程 */
    private fun fallbackText(today: String): String {
        val start = prefs.getString(KEY_PLAN_START_DATE, null)
        if (start.isNullOrBlank()) return DEFAULT_TEXT
        val diff = daysBetween(start, today) ?: return DEFAULT_TEXT
        val day = diff + 1
        val total = prefs.getInt(KEY_PLAN_TOTAL_DAYS, 56).coerceAtLeast(1)
        return when {
            day < 1 -> "康复计划 $start 开始"
            day > total -> "8 周计划已完成，保持每周 2 次训练"
            else -> "康复计划第 $day/$total 天 · 打开 App 查看今日动作"
        }
    }

    private fun daysBetween(from: String, to: String): Int? {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val a = fmt.parse(from) ?: return null
            val b = fmt.parse(to) ?: return null
            ((b.time - a.time + HALF_DAY_MS) / DAY_MS).toInt()
        } catch (_: Exception) {
            null
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
        if (enabled) {
            val hour = prefs.getInt(KEY_REMINDER_HOUR, 21)
            val minute = prefs.getInt(KEY_REMINDER_MINUTE, 0)
            scheduleDailyReminder(hour, minute)
        } else {
            cancelDailyReminder()
        }
    }

    fun isReminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMINDER_ENABLED, false)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()

        if (isReminderEnabled()) {
            scheduleDailyReminder(hour, minute)
        }
    }

    fun getReminderHour(): Int {
        return prefs.getInt(KEY_REMINDER_HOUR, 21)
    }

    fun getReminderMinute(): Int {
        return prefs.getInt(KEY_REMINDER_MINUTE, 0)
    }

    fun scheduleDailyReminder(hour: Int = 21, minute: Int = 0) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // 如果时间已过，设置为明天
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 使用 AlarmManager 设置重复提醒
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelDailyReminder() {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 检查是否是开机启动广播
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 重新设置提醒
            val prefs = context.getSharedPreferences(
                NotificationHelper.PREFS_NAME,
                Context.MODE_PRIVATE
            )
            val enabled = prefs.getBoolean(NotificationHelper.KEY_REMINDER_ENABLED, false)
            if (enabled) {
                val hour = prefs.getInt(NotificationHelper.KEY_REMINDER_HOUR, 21)
                val minute = prefs.getInt(NotificationHelper.KEY_REMINDER_MINUTE, 0)
                val helper = NotificationHelper(context)
                helper.scheduleDailyReminder(hour, minute)
            }
        } else {
            // 显示提醒通知
            val notificationHelper = NotificationHelper(context)
            notificationHelper.showNotification()
        }
    }
}
