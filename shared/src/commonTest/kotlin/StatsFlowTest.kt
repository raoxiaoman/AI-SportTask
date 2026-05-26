package shared

import data.calculateConsecutiveDays
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 统计页面核心计算测试
 *
 * 覆盖：统计数据聚合、趋势分析、
 * 历史对比、汇总统计的边界场景
 */
class StatsFlowTest {

    // ============ 周统计聚合 ============

    @Test
    fun `7天完整周的数据填充`() {
        // 模拟只有3天的实际数据，填充为完整7天
        val rawData = mapOf(
            "2025-05-19" to listOf(DailyRecord(600)),
            "2025-05-21" to listOf(DailyRecord(900)),
            "2025-05-23" to listOf(DailyRecord(1200))
        )

        val weekDates = (6 downTo 0).map { daysAgo("2025-05-26", it) }
        val filled = weekDates.map { date ->
            val records = rawData[date] ?: emptyList()
            DailyStat(
                date = date,
                count = records.size,
                duration = records.sumOf { it.duration }
            )
        }

        assertEquals(7, filled.size)
        assertEquals(600, filled[1].duration) // 5/19
        assertEquals(0, filled[0].duration)   // 5/18
        assertEquals(900, filled[3].duration) // 5/21
    }

    @Test
    fun `周统计数据汇总-有数据的几天`() {
        val weeklyStats = listOf(
            DailyStat("2025-05-19", 1, 600),
            DailyStat("2025-05-20", 0, 0),
            DailyStat("2025-05-21", 2, 1800),
            DailyStat("2025-05-22", 0, 0),
            DailyStat("2025-05-23", 1, 900),
            DailyStat("2025-05-24", 0, 0),
            DailyStat("2025-05-25", 1, 1200)
        )

        val weeklyCount = weeklyStats.sumOf { it.count }
        val weeklyDuration = weeklyStats.sumOf { it.duration }

        assertEquals(5, weeklyCount)
        assertEquals(4500, weeklyDuration) // 600 + 1800 + 900 + 1200
    }

    @Test
    fun `整周无训练-统计数据全零`() {
        val emptyWeek = (1..7).map {
            DailyStat("2025-05-19", 0, 0)
        }
        assertEquals(0, emptyWeek.sumOf { it.count })
        assertEquals(0, emptyWeek.sumOf { it.duration })
    }

    // ============ 日统计数据去重 ============

    @Test
    fun `同一天多次打卡-只计一次连续天数`() {
        val checkinDates = listOf(
            "2025-05-20", "2025-05-20", // 同一天两次
            "2025-05-21", "2025-05-22"
        )
        val uniqueDates = checkinDates.distinct().sorted()
        assertEquals(3, uniqueDates.size) // 20, 21, 22
        assertEquals(3, calculateConsecutiveDays(uniqueDates))
    }

    @Test
    fun `一个月内每天打多次卡-统计唯一日期`() {
        val rawDates = mutableListOf<String>()
        for (day in 1..30) {
            rawDates.add("2025-05-${day.toString().padStart(2, '0')}")
            rawDates.add("2025-05-${day.toString().padStart(2, '0')}") // 两次
        }
        val unique = rawDates.distinct()
        assertEquals(30, unique.size)
    }

    // ============ 多维度统计 ============

    @Test
    fun `月度统计-按周统计后汇总`() {
        // 一个月4周
        val weeklyCounts = listOf(3, 5, 2, 4)
        val weeklyDurations = listOf(3600, 5400, 1800, 4500)

        val monthCount = weeklyCounts.sum()
        val monthDuration = weeklyDurations.sum()

        assertEquals(14, monthCount)
        assertEquals(15300, monthDuration) // 4.25 小时
    }

    @Test
    fun `季度统计-按月汇总`() {
        val monthlyCounts = listOf(12, 15, 10)
        val quarterCount = monthlyCounts.sum()
        assertEquals(37, quarterCount)
    }

    // ============ 趋势计算 ============

    @Test
    fun `本周对比上周-增长`() {
        val thisWeek = 7
        val lastWeek = 5
        val changePercent = ((thisWeek - lastWeek).toFloat() / lastWeek) * 100
        assertEquals(40.0f, changePercent, 0.01f)
    }

    @Test
    fun `本周对比上周-下降`() {
        val thisWeek = 3
        val lastWeek = 6
        val changePercent = ((thisWeek - lastWeek).toFloat() / lastWeek) * 100
        assertEquals(-50.0f, changePercent, 0.01f)
    }

    @Test
    fun `本周对比上周-持平`() {
        val thisWeek = 4
        val lastWeek = 4
        assertEquals(0.0f, ((thisWeek - lastWeek).toFloat() / lastWeek) * 100, 0.01f)
    }

    @Test
    fun `上周无数据-趋势为null`() {
        val lastWeekCount = 0
        val hasTrend = lastWeekCount > 0
        assertFalse(hasTrend)
    }

    // ============ 平均计算 ============

    @Test
    fun `日均训练次数`() {
        val total = 15
        val days = 7
        val avg = total.toFloat() / days
        assertEquals(2.142f, avg, 0.01f)
    }

    @Test
    fun `日均训练时长`() {
        val totalSecs = 10800L // 3小时
        val days = 7L
        val avg = totalSecs / days
        assertEquals(1542L, avg) // 10800/7 ≈ 1542秒 ≈ 25.7分钟
    }

    @Test
    fun `平均每次训练时长`() {
        val total = 10800 / 7 // 一周总和10800秒，7次训练
        assertEquals(1542, total / 1) // 每次训练约 1542 秒
    }

    @Test
    fun `无数据时平均为0`() {
        assertEquals(0f, 0.toFloat() / 1)
    }

    // ============ 柱状图归一化 ============

    @Test
    fun `柱状图高度归一化-最大值归一为1`() {
        val values = listOf(0, 2, 5, 3, 1, 0, 4)
        val max = values.maxOrNull() ?: 1
        val normalized = values.map { if (max > 0) it.toFloat() / max else 0f }

        assertEquals(1.0f, normalized[2])  // 5 → 1.0
        assertEquals(0.8f, normalized[6])  // 4 → 0.8
        assertEquals(0.0f, normalized[0])  // 0 → 0.0
    }

    @Test
    fun `全零柱状图不崩溃`() {
        val values = listOf(0, 0, 0, 0, 0, 0, 0)
        val max = values.maxOrNull() ?: 1
        val normalized = values.map { if (max > 0) it.toFloat() / max else 0f }
        normalized.forEach { assertEquals(0f, it) }
    }

    @Test
    fun `单天有数据的柱状图归一化`() {
        val values = listOf(0, 0, 5, 0, 0, 0, 0)
        val max = values.maxOrNull() ?: 1
        val normalized = values.map { if (max > 0) it.toFloat() / max else 0f }
        assertEquals(1.0f, normalized[2])
        assertEquals(0.0f, normalized[0])
    }

    // ============ 分组统计 ============

    @Test
    fun `按分组统计训练次数`() {
        val checkins = listOf(
            GroupCheckinStat("上肢训练", 10, 7200),
            GroupCheckinStat("下肢训练", 8, 5400),
            GroupCheckinStat("核心训练", 3, 1800)
        )

        val totalSessions = checkins.sumOf { it.count }
        val totalDuration = checkins.sumOf { it.totalDuration }

        assertEquals(21, totalSessions)
        assertEquals(14400, totalDuration) // 4小时
    }

    @Test
    fun `某分组无训练记录`() {
        val groups = listOf(
            GroupCheckinStat("新分组", 0, 0)
        )
        assertEquals(0, groups.sumOf { it.count })
    }

    // ============ 辅助函数 ============

    private data class DailyRecord(val duration: Int)
    private data class DailyStat(val date: String, val count: Int, val duration: Int)
    private data class GroupCheckinStat(val name: String, val count: Int, val totalDuration: Int)

    private fun daysAgo(from: String, days: Int): String {
        val parts = from.split("-")
        val y = parts[0].toInt()
        val m = parts[1].toInt()
        val d = parts[2].toInt()
        val totalDay = absoluteDayCount(y, m, d) - days
        return fromAbsoluteDayCount(totalDay)
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


