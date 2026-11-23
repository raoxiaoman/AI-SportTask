package db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.raohui.sporttask.db.SportTaskDatabase
import java.io.File

/** 桌面端数据库驱动 —— SQLite JDBC，文件落在用户目录 ~/.sporttask/sporttask.db */
actual fun createDriver(): SqlDriver {
    val dir = File(System.getProperty("user.home"), ".sporttask").apply { mkdirs() }
    val dbFile = File(dir, "sporttask.db")
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
    // 首次创建时建表；已存在则跳过，避免重复建表报错
    if (!dbFile.exists() || dbFile.length() == 0L) {
        SportTaskDatabase.Schema.create(driver)
    }
    return driver
}
