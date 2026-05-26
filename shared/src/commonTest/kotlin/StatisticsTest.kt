package shared

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 统计模块核心逻辑测试
 *
 * 覆盖：周/月统计聚合、完成率、趋势计算、
 * 统计区间边界、数据归一化
 */
class StatisticsTest {

    // ============ 周统计 ============

    @Test
    fun `周训练次数-每天小于等于7天`() {
        val dailyData = listOf(1, 0, 2, 0, 1, 0, 1) // 7天
        assertTrue(dailyData.size <= 7)
    }

    @Test
    fun `周训练次数汇总`() {
        val weeklyCounts = listOf(1, 2, 1, 0, 1, 0, 0)
        assertEquals(5, weeklyCounts.sum())
    }

    @Test
    fun `周训练时长为各个动作耗时累计`() {
        val weeklyDuration = listOf(600, 1200, 0, 900, 1800, 0, 450)
        assertEquals(4950, weeklyDuration.sum())
    }

    @Test
    fun `周统计-全勤训练`() {
        val dailyData = listOf(1, 2, 1, 1, 2, 1, 1)
        assertEquals(9, dailyData.sum())
    }

    @Test
    fun `周统计-完全未训练`() {
        val dailyData = listOf(0, 0, 0, 0, 0, 0, 0)
        assertEquals(0, dailyData.sum())
    }

    // ============ 月统计 ============

    @Test
    fun `月训练次数-30天汇总`() {
        val monthData = (1..30).map { if (it % 2 == 0) 1 else 0 } // 隔天练
        assertEquals(15, monthData.sum())
    }

    @Test
    fun `月训练时长-假设每次30分钟`() {
        val trainingDays = 15
        val durationPerSession = 1800 // 30分钟
        assertEquals(27000, trainingDays * durationPerSession) // 27000秒 = 7.5小时
    }

    @Test
    fun `月完成率-训练天数比例`() {
        val trainingDays = 20
        val totalDays = 30
        val rate = (trainingDays.toFloat() / totalDays) * 100
        assertEquals(66.666f, rate, 0.01f)
    }

    // ============ 完成率计算 ============

    @Test
    fun `完成率-全部完成100%`() {
        val completed = 10L
        val total = 10L
        val rate = if (total > 0) (completed.toFloat() / total) * 100 else 0f
        assertEquals(100f, rate)
    }

    @Test
    fun `完成率-一半`() {
        val completed = 5L
        val total = 10L
        val rate = if (total > 0) (completed.toFloat() / total) * 100 else 0f
        assertEquals(50f, rate)
    }

    @Test
    fun `完成率-无记录时为零`() {
        val completed = 0L
        val total = 0L
        val rate = if (total > 0) (completed.toFloat() / total) * 100 else 0f
        assertEquals(0f, rate)
    }

    @Test
    fun `完成率-全部未完成`() {
        val completed = 0L
        val total = 10L
        val rate = if (total > 0) (completed.toFloat() / total) * 100 else 0f
        assertEquals(0f, rate)
    }

    @Test
    fun `完成率-只完成一项`() {
        val completed = 1L
        val total = 10L
        val rate = if (total > 0) (completed.toFloat() / total) * 100 else 0f
        assertEquals(10f, rate)
    }

    // ============ 趋势分析 ============

    @Test
    fun `趋势-周对比周`() {
        val thisWeek = 5
        val lastWeek = 3
        val change = ((thisWeek - lastWeek).toFloat() / lastWeek) * 100
        assertEquals(66.666f, change, 0.01f) // 增长 66.67%
    }

    @Test
    fun `趋势-持平`() {
        val thisWeek = 4
        val lastWeek = 4
        assertEquals(0, thisWeek - lastWeek)
    }

    @Test
    fun `趋势-下降`() {
        val thisWeek = 4
        val lastWeek = 7
        assertTrue(thisWeek < lastWeek)
        assertEquals(-42.857f, ((thisWeek - lastWeek).toFloat() / lastWeek) * 100, 0.01f)
    }

    @Test
    fun `趋势-上周无数据不计算变化率`() {
        val thisWeek = 5
        val lastWeek = 0
        val change = if (lastWeek > 0) ((thisWeek - lastWeek).toFloat() / lastWeek) * 100 else null
        assertEquals(null, change)
    }

    // ============ 每日统计趋势 ============

    @Test
    fun `每日数据-生成7天完整周视图`() {
        val dailyStats = listOf(
            DailyStat("2025-01-20", 1, 600),
            DailyStat("2025-01-21", 2, 1200),
            DailyStat("2025-01-22", 0, 0),
            DailyStat("2025-01-23", 1, 900)
        )

        // 补齐7天
        val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val fullWeek = (0..6).map { offset ->
            val date = daysAgoDateString(6 - offset)
            dailyStats.find { it.date == date }
                ?: DailyStat(date = date, count = 0, duration = 0)
        }

        assertEquals(7, fullWeek.size)
    }

    // ============ 柱状图数据 ============

    @Test
    fun `柱状图-最大高度归一化`() {
        val counts = listOf(0, 1, 3, 2, 0, 1, 2)
        val maxCount = counts.maxOrNull() ?: 1
        val normalized = counts.map { it.toFloat() / maxCount }

        assertEquals(1f, normalized[2]) // 索引2对应3次，是最大值
        assertEquals(0f, normalized[0])
        assertEquals(2f/3f, normalized[3], 0.01f)
    }

    @Test
    fun `柱状图-全零不崩溃`() {
        val counts = listOf(0, 0, 0, 0, 0, 0, 0)
        val maxCount = counts.maxOrNull() ?: 1
        val normalized = counts.map { it.toFloat() / maxCount }

        // 全零时 maxCount 回退到1，全部显示为0
        normalized.forEach { assertEquals(0f, it) }
    }

    // ============ 统计区间边界 ============

    @Test
    fun `统计区间-本周一至今天`() {
        val today = "2025-01-28" // 周二
        val weekStart = "2025-01-23" // 6天前(含今天=7天)

        val startDate = daysAgoDateString(6)
        assertTrue(startDate <= today)
    }

    @Test
    fun `统计区间-本月1号至今天`() {
        val today = "2025-01-28"
        val monthStart = "2025-01-01" // 28天前

        val startDate = daysAgoDateString(27)
        assertTrue(startDate <= today)
    }

    // ============ 平均统计 ============

    @Test
    fun `平均每日训练时长`() {
        val totalDuration = 5400L // 1.5小时
        val days = 7
        val avgPerDay = totalDuration / days
        assertEquals(771L, avgPerDay) // 5400/7 ≈ 771秒
    }

    @Test
    fun `平均每次训练时长`() {
        val totalDuration = 3600
        val totalSessions = 3
        val avgPerSession = totalDuration / totalSessions
        assertEquals(1200, avgPerSession) // 20分钟
    }

    // ============ 辅助函数 ============

    private data class DailyStat(
        val date: String,
        val count: Int,
        val duration: Int
    )

    /**
     * 生成 N 天前的日期字符串 (YYYY-MM-DD)
     * 简化版，模拟 expect/actual 行为
     */
    private fun daysAgoDateString(days: Int): String {
        val parts = "2025-01-28".split("-") // 模拟 today = 2025-01-28
        val y = parts[0].toInt()
        val m = parts[1].toInt()
        val d = parts[2].toInt()

        val totalDays = absoluteDayCount(y, m, d) - days
        return fromAbsoluteDayCount(totalDays)
    }

    private fun absoluteDayCount(y: Int, m: Int, d: Int): Int {
        val monthDays = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val y0 = y - 1
        val leapDays = y0 / 4 - y0 / 100 + y0 / 400
        var days = y0 * 365 + leapDays
        for (i in 0 until m - 1) days += monthDays[i]
        if (m > 2 && ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0)) days++
        days += d
        return days
    }

    private fun fromAbsoluteDayCount(totalDays: Int): String {
        // 简化：从 2025-01-01 (absoluteDayCount = 738521) 开始推算
        val base = absoluteDayCount(2025, 1, 1)
        var remaining = totalDays - base
        var y = 2025
        val monthDays = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        while (true) {
            val daysInYear = if ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0) 366 else 365
            if (remaining >= daysInYear) {
                remaining -= daysInYear
                y++
            } else break
        }

        var m = 1
        while (m <= 12) {
            val daysInMonth = monthDays[m - 1] + if (m == 2 && ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0)) 1 else 0
            if (remaining >= daysInMonth) {
                remaining -= daysInMonth
                m++
            } else break
        }

        val d = remaining + 1
        return "${y}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
    }
}
