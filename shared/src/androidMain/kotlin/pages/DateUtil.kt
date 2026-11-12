@file:JvmName("DateUtilAndroid")
package pages

import java.time.LocalDate
import java.time.LocalTime

actual fun todayDateString(): String = LocalDate.now().toString()

actual fun daysAgoDateString(days: Int): String = LocalDate.now().minusDays(days.toLong()).toString()

actual fun currentHourOfDay(): Int = LocalTime.now().hour
