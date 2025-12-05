package db

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import com.raohui.sporttask.db.SportTaskDatabase

actual fun createDriver(): SqlDriver {
    return NativeSqliteDriver(SportTaskDatabase.Schema, "sporttask.db")
}