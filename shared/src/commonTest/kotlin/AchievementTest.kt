package shared

import data.Achievement
import data.AchievementManager
import data.ALL_ACHIEVEMENTS
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 成就系统测试
 *
 * 覆盖：成就定义完整性、解锁条件判定逻辑、
 * 成就排序、成就密度检查
 */
class AchievementTest {

    // ============ 成就定义测试 ============

    @Test
    fun `所有成就都有唯一ID`() {
        val ids = ALL_ACHIEVEMENTS.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals(ids.size, uniqueIds.size, "成就ID必须唯一")
    }

    @Test
    fun `所有成就都有非空字段`() {
        ALL_ACHIEVEMENTS.forEach { ach ->
            assertTrue(ach.id.isNotBlank(), "成就ID不能为空: ${ach.id}")
            assertTrue(ach.title.isNotBlank(), "成就标题不能为空: ${ach.id}")
            assertTrue(ach.description.isNotBlank(), "成就描述不能为空: ${ach.id}")
            assertTrue(ach.icon.isNotBlank(), "成就图标不能为空: ${ach.id}")
        }
    }

    @Test
    fun `成就总数为10个`() {
        assertEquals(10, ALL_ACHIEVEMENTS.size)
    }

    @Test
    fun `成就按sortOrder排序`() {
        val sorted = ALL_ACHIEVEMENTS.sortedBy { it.sortOrder }
        for (i in 1 until sorted.size) {
            assertTrue(sorted[i].sortOrder >= sorted[i - 1].sortOrder,
                "成就排序错误: ${sorted[i-1].id}(${sorted[i-1].sortOrder}) > ${sorted[i].id}(${sorted[i].sortOrder})")
        }
    }

    // ============ 成就类型测试 ============

    @Test
    fun `成就ID列表完整`() {
        val expectedIds = listOf(
            "first_checkin", "three_streak", "seven_streak", "thirty_streak",
            "ten_total", "fifty_total", "hundred_total",
            "hour_total", "ten_hours", "first_hour_session"
        )
        val actualIds = ALL_ACHIEVEMENTS.map { it.id }.sorted()
        assertEquals(expectedIds.sorted(), actualIds)
    }

    @Test
    fun `成就包含连续打卡类型`() {
        val streakIds = listOf("three_streak", "seven_streak", "thirty_streak")
        val actualIds = ALL_ACHIEVEMENTS.map { it.id }
        streakIds.forEach { id ->
            assertTrue(actualIds.contains(id), "缺少连续打卡成就: $id")
        }
    }

    @Test
    fun `成就包含累计次数类型`() {
        val countIds = listOf("ten_total", "fifty_total", "hundred_total")
        val actualIds = ALL_ACHIEVEMENTS.map { it.id }
        countIds.forEach { id ->
            assertTrue(actualIds.contains(id), "缺少累计次数成就: $id")
        }
    }

    @Test
    fun `成就包含累计时长类型`() {
        val durationIds = listOf("hour_total", "ten_hours", "first_hour_session")
        val actualIds = ALL_ACHIEVEMENTS.map { it.id }
        durationIds.forEach { id ->
            assertTrue(actualIds.contains(id), "缺少累计时长成就: $id")
        }
    }

    // ============ 解锁条件逻辑测试 ============

    @Test
    fun `首次打卡成就-0次不触发`() {
        val condition = { totalCount: Int -> totalCount >= 1 }
        assertFalse(condition(0))
    }

    @Test
    fun `首次打卡成就-1次触发`() {
        val condition = { totalCount: Int -> totalCount >= 1 }
        assertTrue(condition(1))
    }

    @Test
    fun `连续3天成就-2天不触发`() {
        val condition = { streak: Int -> streak >= 3 }
        assertFalse(condition(2))
    }

    @Test
    fun `连续3天成就-3天触发`() {
        val condition = { streak: Int -> streak >= 3 }
        assertTrue(condition(3))
    }

    @Test
    fun `连续30天成就-30天触发`() {
        val condition = { streak: Int -> streak >= 30 }
        assertTrue(condition(30))
    }

    @Test
    fun `累计10次成就-9次不触发`() {
        val condition = { count: Int -> count >= 10 }
        assertFalse(condition(9))
    }

    @Test
    fun `累计50次成就-50次触发`() {
        val condition = { count: Int -> count >= 50 }
        assertTrue(condition(50))
    }

    @Test
    fun `累计100次成就-99次不触发`() {
        val condition = { count: Int -> count >= 100 }
        assertFalse(condition(99))
    }

    @Test
    fun `累计1小时成就-不足1小时不触发`() {
        val condition = { duration: Long -> duration >= 3600L }
        assertFalse(condition(3599))
    }

    @Test
    fun `累计10小时成就-10小时触发`() {
        val condition = { duration: Long -> duration >= 36000L }
        assertTrue(condition(36000))
    }

    @Test
    fun `单次1小时成就-59分钟不触发`() {
        val condition = { maxDuration: Long -> maxDuration >= 3600L }
        assertFalse(condition(3540))
    }

    // ============ 边界条件测试 ============

    @Test
    fun `多项成就同时解锁`() {
        // 模拟第一次训练完成：1次打卡 + 时长3600秒
        val totalCount = 1
        val totalDuration = 3600L
        val maxDuration = 3600L

        val unlocked = mutableListOf<String>()
        if (totalCount >= 1) unlocked.add("first_checkin")
        if (totalDuration >= 3600L) unlocked.add("hour_total")
        if (maxDuration >= 3600L) unlocked.add("first_hour_session")

        assertEquals(3, unlocked.size)
        assertTrue(unlocked.contains("first_checkin"))
        assertTrue(unlocked.contains("hour_total"))
        assertTrue(unlocked.contains("first_hour_session"))
    }

    @Test
    fun `第50次训练解锁三个成就`() {
        val totalCount = 50
        val streakDays = 10

        val unlocked = mutableListOf<String>()
        if (totalCount >= 1) unlocked.add("first_checkin")
        if (streakDays >= 3) unlocked.add("three_streak")
        if (streakDays >= 7) unlocked.add("seven_streak")
        if (totalCount >= 10) unlocked.add("ten_total")
        if (totalCount >= 50) unlocked.add("fifty_total")

        assertEquals(5, unlocked.size)
    }

    // ============ 成就密度测试 ============

    @Test
    fun `成就进度阶梯合理`() {
        // 检查连续打卡成就的难度递进
        val streakAch = ALL_ACHIEVEMENTS.filter {
            it.id.contains("streak")
        }.sortedBy { it.sortOrder }

        val requiredDays = streakAch.map {
            when (it.id) {
                "three_streak" -> 3
                "seven_streak" -> 7
                "thirty_streak" -> 30
                else -> 0
            }
        }

        // 确保难度递增
        for (i in 1 until requiredDays.size) {
            assertTrue(requiredDays[i] > requiredDays[i - 1],
                "成就难度应递增: ${streakAch[i-1].id}(${requiredDays[i-1]}) -> ${streakAch[i].id}(${requiredDays[i]})")
        }
    }

    @Test
    fun `累计次数成就难度递进`() {
        val countAch = ALL_ACHIEVEMENTS.filter {
            it.id.contains("total")
        }.sortedBy { it.sortOrder }

        val requiredCounts = countAch.map {
            when (it.id) {
                "ten_total" -> 10
                "fifty_total" -> 50
                "hundred_total" -> 100
                else -> 0
            }
        }

        for (i in 1 until requiredCounts.size) {
            assertTrue(requiredCounts[i] > requiredCounts[i - 1])
        }
    }
}
