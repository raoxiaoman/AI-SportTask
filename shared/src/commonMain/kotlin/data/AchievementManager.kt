package data

import pages.todayDateString

/**
 * 成就系统 - 里程碑定义
 */
data class Achievement(
    val id: String,
    val icon: String,
    val title: String,
    val description: String,
    val sortOrder: Int
)

/**
 * 所有可解锁成就
 */
val ALL_ACHIEVEMENTS = listOf(
    Achievement("first_checkin", "🏅", "第一次打卡", "完成你的第一次训练打卡", 1),
    Achievement("three_streak", "🔥", "小试牛刀", "连续打卡 3 天", 2),
    Achievement("seven_streak", "🔥🔥", "渐入佳境", "连续打卡 7 天", 3),
    Achievement("thirty_streak", "💎", "铁打不练", "连续打卡 30 天", 4),
    Achievement("ten_total", "🎯", "坚持初显", "累计完成 10 次训练", 5),
    Achievement("fifty_total", "⭐", "训练达人", "累计完成 50 次训练", 6),
    Achievement("hundred_total", "🌟", "百炼成钢", "累计完成 100 次训练", 7),
    Achievement("hour_total", "⏱️", "一小时起步", "累计训练 1 小时", 8),
    Achievement("ten_hours", "⏱️⏱️", "十小时达人", "累计训练 10 小时", 9),
    Achievement("first_hour_session", "💪", "全力以赴", "单次训练超过 1 小时", 10)
)

object AchievementManager {

    /**
     * 检查训练结束后新解锁的成就
     * @param repository 数据仓库
     * @return 新解锁的成就列表
     */
    suspend fun checkNewAchievements(repository: SportTaskRepository): List<Achievement> {
        val allCheckins = repository.getCheckinsByDateRange("1970-01-01", "2099-12-31")
        val uniqueDates = allCheckins.map { it.date }.distinct().sorted()
        val totalCount = allCheckins.size
        val totalDuration: Long = allCheckins.sumOf { it.duration ?: 0L }
        val maxDuration: Long = allCheckins.maxOfOrNull { it.duration ?: 0L } ?: 0L

        // 当前连续打卡天数
        val today = todayDateString()
        var streakCount = 0
        var expectedDate = today
        for (dateStr in uniqueDates.reversed()) {
            val diff = daysBetweenStrings(dateStr, expectedDate)
            if (diff == 0 || diff == 1) {
                streakCount++
                expectedDate = dateStr
            } else if (diff > 1) break
        }
        val streakDays = streakCount

        return ALL_ACHIEVEMENTS.filter { ach ->
            when (ach.id) {
                "first_checkin" -> totalCount >= 1
                "three_streak" -> streakDays >= 3
                "seven_streak" -> streakDays >= 7
                "thirty_streak" -> streakDays >= 30
                "ten_total" -> totalCount >= 10
                "fifty_total" -> totalCount >= 50
                "hundred_total" -> totalCount >= 100
                "hour_total" -> totalDuration >= 3600L
                "ten_hours" -> totalDuration >= 36000L
                "first_hour_session" -> maxDuration >= 3600L
                else -> false
            }
        }
    }
}

