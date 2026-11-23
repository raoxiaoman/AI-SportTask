package pages

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

actual fun todayDateString(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

actual fun daysAgoDateString(days: Int): String =
    LocalDate.now().minusDays(days.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)

actual fun currentHourOfDay(): Int = LocalTime.now().hour
