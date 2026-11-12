package com.raohui.sporttask

import MainView
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import data.plan.ReminderConfig
import data.plan.ReminderController
import data.remote.AndroidPlatformStorage
import data.remote.PlatformStorageProvider
import data.remote.ServerConfig
import db.initDb

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化平台存储（SharedPreferences）
        PlatformStorageProvider.init(AndroidPlatformStorage(this))

        // 从本地存储恢复服务器地址
        val savedUrl = getPreferences(Context.MODE_PRIVATE)
            .getString("server_url", null)
        if (savedUrl != null) {
            ServerConfig.setBaseUrl(savedUrl)
        } else {
            // 首次启动：尝试从 assets/server_url.txt 读取
            val urlFromAssets = try {
                assets.open("server_url.txt").bufferedReader().use { it.readLine() }
            } catch (_: Exception) { null }
            if (!urlFromAssets.isNullOrBlank()) {
                ServerConfig.setBaseUrl(urlFromAssets.trim())
            }
        }

        // 初始化数据库
        initDb(this)

        setupReminderBridge()
        requestNotificationPermissionIfNeeded()

        setContent {
            MainView()
        }
    }

    /** 把 commonMain 的提醒控制点接到 Android 的 AlarmManager 封装上 */
    private fun setupReminderBridge() {
        val helper = NotificationHelper(this)
        ReminderController.apply = { enabled, hour, minute ->
            helper.setReminderTime(hour, minute)
            helper.setReminderEnabled(enabled)
        }
        ReminderController.snapshot = {
            ReminderConfig(
                enabled = helper.isReminderEnabled(),
                hour = helper.getReminderHour(),
                minute = helper.getReminderMinute()
            )
        }
    }

    /** Android 13+ 需要运行时申请通知权限，否则提醒会被系统静默丢弃 */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 1002
    }
}
