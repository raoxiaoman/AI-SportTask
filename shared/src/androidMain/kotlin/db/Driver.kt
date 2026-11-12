package db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import com.raohui.sporttask.db.SportTaskDatabase

lateinit var appContext: Context

/** lateinit 的 backing field 仅在声明处可访问，对外提供初始化状态判断 */
fun isDbContextReady(): Boolean = ::appContext.isInitialized

fun initDb(context: Context) {
    appContext = context.applicationContext
}

actual fun createDriver(): SqlDriver {
    return AndroidSqliteDriver(SportTaskDatabase.Schema, appContext, "sporttask.db")
}