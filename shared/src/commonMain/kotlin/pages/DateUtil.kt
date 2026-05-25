package pages

// 跨平台日期工具（避免依赖 java.time）

expect fun todayDateString(): String

/**
 * 返回 N 天前的日期字符串 "YYYY-MM-DD"
 */
expect fun daysAgoDateString(days: Int): String

/**
 * 解析 "YYYY-MM-DD" 格式日期获取星期几
 * 返回 1=Monday ... 7=Sunday
 */
fun dayOfWeekFromDate(dateStr: String): Int {
    val parts = dateStr.split("-")
    if (parts.size != 3) return 1
    val y = parts[0].toIntOrNull() ?: return 1
    val m = parts[1].toIntOrNull() ?: return 1
    val d = parts[2].toIntOrNull() ?: return 1

    val month = if (m <= 2) m + 12 else m
    val year = if (m <= 2) y - 1 else y
    val k = year % 100
    val j = year / 100
    return ((d + (13 * (month + 1)) / 5 + k + k / 4 + j / 4 + 5 * j) % 7).let { q ->
        ((q + 5) % 7) + 1 // 1=Monday ... 7=Sunday
    }
}
