package data

/**
 * 分组数据类
 */
data class GroupItem(
    val id: Long,
    val name: String,
    val actionCount: Int
)

/**
 * 动作数据类
 */
data class ActionItem(
    val id: Long = 0,
    val name: String,
    val stepsText: String,
    val defaultTime: Int,
    val restTime: Int,
    val orderIndex: Int
)

/**
 * 打卡记录数据类
 */
data class CheckinItem(
    val id: Long,
    val date: String,
    val duration: Int,
    val isCompleted: Boolean
)

/**
 * 训练结果数据类
 */
data class TrainingResult(
    val groupId: Long,
    val duration: Int,
    val completed: Boolean
)

/**
 * 导入结果数据类
 */
data class ImportResult(
    val success: Boolean,
    val groupsImported: Int,
    val actionsImported: Int,
    val checkinsImported: Int,
    val errorMessage: String? = null
)

/**
 * 格式化时间
 */
fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (secs < 10) "$mins:0$secs" else "$mins:$secs"
}

/**
 * 计算最大连续打卡天数（基于已排序的日期列表）
 */
fun calculateConsecutiveDays(dates: List<String>): Int {
    if (dates.isEmpty()) return 0

    val sortedDates = dates.sorted()
    var maxConsecutive = 1
    var currentConsecutive = 1

    for (i in 1 until sortedDates.size) {
        if (areConsecutiveDays(sortedDates[i - 1], sortedDates[i])) {
            currentConsecutive++
            maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
        } else {
            currentConsecutive = 1
        }
    }
    return maxConsecutive
}

/**
 * 判断两个 YYYY-MM-DD 日期是否相邻
 */
fun areConsecutiveDays(date1: String, date2: String): Boolean {
    return daysBetweenStrings(date1, date2) == 1
}

internal fun daysBetweenStrings(from: String, to: String): Int {
    val f = from.split("-").map { it.toIntOrNull() ?: 0 }
    val t = to.split("-").map { it.toIntOrNull() ?: 0 }
    if (f.size < 3 || t.size < 3) return Int.MAX_VALUE
    return absoluteDayCount(t[0], t[1], t[2]) - absoluteDayCount(f[0], f[1], f[2])
}

internal fun absoluteDayCount(y: Int, m: Int, d: Int): Int {
    val monthDays = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val y0 = y - 1
    val leapDays = y0 / 4 - y0 / 100 + y0 / 400
    var days = y0 * 365 + leapDays
    for (i in 0 until m - 1) days += monthDays[i]
    if (m > 2 && ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0)) days++
    days += d
    return days
}
