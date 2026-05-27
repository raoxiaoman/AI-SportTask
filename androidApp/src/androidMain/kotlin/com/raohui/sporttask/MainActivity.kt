package com.raohui.sporttask

import MainView
import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import data.remote.PlatformStorage
import data.remote.ServerConfig
import db.initDb

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化平台存储（SharedPreferences）
        PlatformStorage.init(this)

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

        setContent {
            MainView()
        }
    }
}
