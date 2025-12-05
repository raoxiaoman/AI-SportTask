package db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import com.raohui.sporttask.db.SportTaskDatabase

lateinit var appContext: Context

fun initDb(context: Context) {
    appContext = context.applicationContext
}

actual fun createDriver(): SqlDriver {
    return AndroidSqliteDriver(SportTaskDatabase.Schema, appContext, "sporttask.db")
}