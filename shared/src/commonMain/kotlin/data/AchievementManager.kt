package data

import data.remote.PlatformStorage
import data.remote.PlatformStorageProvider
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

    private const val UNLOCKED_KEY = "sporttask_unlocked_achievements"
    private const val SEP = ","

    /** 可注入存储，便于测试；默认走平台持久化（Android SharedPreferences / iOS NSUserDefaults） */
    private var storageProvider: () -> PlatformStorage = { PlatformStorageProvider.get() }

    internal fun setStorageProviderForTest(provider: (() -> PlatformStorage)?) {
        storageProvider = provider ?: { PlatformStorageProvider.get() }
    }

    private fun loadUnlocked(): Set<String> {
        val raw = runCatching { storageProvider().load(UNLOCKED_KEY) }.getOrNull()
        return raw?.takeIf { it.isNotBlank() }
            ?.split(SEP)
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }

    private fun saveUnlocked(ids: Set<String>) {
        runCatching { storageProvider().save(UNLOCKED_KEY, ids.joinToString(SEP)) }
    }

    /**
     * 检查训练结束后新解锁的成就（只返回此前未解锁过的成就）。
     *
     * 修复：此前仅按当前统计筛选达标成就、不记录已解锁状态，
     * 导致每次训练完成都把 first_checkin 等已达成就当成「新解锁」重复弹窗。
     * 现在读取平台持久化的已解锁 id 做差集，并立即写回，
     * 既避免重复弹出，也保证重启后不再弹出。
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
        return computeNewAchievements(totalCount, streakCount, totalDuration, maxDuration)
    }

    /**
     * 按统计数据计算新解锁成就并立即写回持久化。
     * 抽出为纯统计入参，便于测试「重复调用不重复返回」的回归场景。
     */
    internal fun computeNewAchievements(
        totalCount: Int,
        streakDays: Int,
        totalDuration: Long,
        maxDuration: Long
    ): List<Achievement> {
        val achieved = ALL_ACHIEVEMENTS.filter { ach ->
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

        val unlocked = loadUnlocked()
        val newOnes = achieved.filterNot { it.id in unlocked }
        if (newOnes.isNotEmpty()) {
            saveUnlocked(unlocked + newOnes.map { it.id })
        }
        return newOnes
    }
}
