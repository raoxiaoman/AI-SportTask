package pages

import java.time.LocalDate

actual fun todayDateString(): String = LocalDate.now().toString()

actual fun daysAgoDateString(days: Int): String = LocalDate.now().minusDays(days.toLong()).toString()
