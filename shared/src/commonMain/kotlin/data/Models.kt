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
    return "%d:%02d".format(mins, secs)
}

/**
 * 计算连续打卡天数
 */
fun calculateConsecutiveDays(dates: List<String>): Int {
    if (dates.isEmpty()) return 0

    val sortedDates = dates.sorted()
    var maxConsecutive = 1
    var currentConsecutive = 1

    for (i in 1 until sortedDates.size) {
        val prev = java.time.LocalDate.parse(sortedDates[i - 1])
        val curr = java.time.LocalDate.parse(sortedDates[i])

        if (curr == prev.plusDays(1)) {
            currentConsecutive++
            maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
        } else {
            currentConsecutive = 1
        }
    }
    return maxConsecutive
}
