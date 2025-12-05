package db

import app.cash.sqldelight.db.SqlDriver
import com.raohui.sporttask.db.SportTaskDatabase

expect fun createDriver(): SqlDriver

object DatabaseProvider {
    val driver: SqlDriver by lazy { createDriver() }
    val db: SportTaskDatabase by lazy { SportTaskDatabase(driver) }
}