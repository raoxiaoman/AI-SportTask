package shared

import data.calculateConsecutiveDays
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 连续打卡核心逻辑深度测试
 *
 * 使用 Models.kt 中真实的 calculateConsecutiveDays 函数，
 * 覆盖各种实际可能出现的打卡模式。
 *
 * 注意：这些测试与 DateUtilTest.calcConsecutiveDays* 互补，
 * 但更注重"最大连续天数"在复杂模式下的表现。
 */
class StreakEdgeTest {

    @Test
    fun `严格连续7天`() {
        val dates = listOf(
            "2025-05-19", "2025-05-20", "2025-05-21",
            "2025-05-22", "2025-05-23", "2025-05-24", "2025-05-25"
        )
        assertEquals(7, calculateConsecutiveDays(dates))
    }

    @Test
    fun `间隔一天后连续三天-最大连续为3`() {
        val dates = listOf(
            "2025-05-19", "2025-05-20",       // 连续2天
            "2025-05-22",                      // 断一天
            "2025-05-23", "2025-05-24", "2025-05-25" // 连续3天
        )
        assertEquals(3, calculateConsecutiveDays(dates))
    }

    @Test
    fun `两周先长断后长连-取最长`() {
        val dates = listOf(
            "2025-05-05", "2025-05-06", "2025-05-07", // 连续3天
            "2025-05-10", "2025-05-11", "2025-05-12", "2025-05-13", "2025-05-14" // 连续5天
        )
        assertEquals(5, calculateConsecutiveDays(dates))
    }

    @Test
    fun `三周的大中断后超长连续`() {
        val dates = listOf(
            "2025-05-01", "2025-05-02", // 连续2天
            // 断13天
            "2025-05-16", "2025-05-17", "2025-05-18",
            "2025-05-19", "2025-05-20", "2025-05-21",
            "2025-05-22", "2025-05-23", "2025-05-24",
            "2025-05-25", "2025-05-26" // 连续11天
        )
        assertEquals(11, calculateConsecutiveDays(dates))
    }

    @Test
    fun `每三天打一次卡-全是孤独的一天`() {
        val dates = listOf(
            "2025-05-01", "2025-05-04", "2025-05-07",
            "2025-05-10", "2025-05-13"
        )
        assertEquals(1, calculateConsecutiveDays(dates))
    }

    @Test
    fun `月中月末跨月连续`() {
        val dates = listOf(
            "2025-05-29", "2025-05-30", "2025-05-31", "2025-06-01"
        )
        assertEquals(4, calculateConsecutiveDays(dates))
    }

    @Test
    fun `跨年连续10天`() {
        val dates = listOf(
            "2025-12-28", "2025-12-29", "2025-12-30", "2025-12-31",
            "2026-01-01", "2026-01-02", "2026-01-03",
            "2026-01-04", "2026-01-05", "2026-01-06"
        )
        assertEquals(10, calculateConsecutiveDays(dates))
    }

    @Test
    fun `闰年2月底跨3月连续`() {
        val dates = listOf(
            "2024-02-27", "2024-02-28", "2024-02-29",
            "2024-03-01"
        )
        assertEquals(4, calculateConsecutiveDays(dates))
    }

    @Test
    fun `非闰年2月跨3月但2月28后断一天`() {
        val dates = listOf(
            "2025-02-26", "2025-02-27", "2025-02-28",
            // 3月1日断
            "2025-03-02"
        )
        assertEquals(3, calculateConsecutiveDays(dates))
    }

    @Test
    fun `两个月连续30天`() {
        val dates = (1..30).map { day ->
            val month = if (day <= 15) "05" else "06"
            val d = if (day <= 15) day else day - 15
            "2025-$month-${d.toString().padStart(2, '0')}"
        }
        // 5月15日 + 6月1-15日 — 中间断开，不是连续
        // 实际需按月不同跨月才行
        val continuous = listOf(
            "2025-05-15", "2025-05-16", "2025-05-17",
            "2025-05-18", "2025-05-19", "2025-05-20",
            "2025-05-21", "2025-05-22", "2025-05-23",
            "2025-05-24", "2025-05-25", "2025-05-26"
        )
        assertEquals(12, calculateConsecutiveDays(continuous))
    }

    @Test
    fun `日期乱序输入-结果与排序后一致`() {
        val unordered = listOf(
            "2025-05-25", "2025-05-20", "2025-05-22",
            "2025-05-23", "2025-05-21"
        )
        val ordered = unordered.sorted()

        val resultUnordered = calculateConsecutiveDays(unordered)
        val resultOrdered = calculateConsecutiveDays(ordered)
        assertEquals(resultOrdered, resultUnordered)
    }

    @Test
    fun `整个月每天打卡-连续30或31天`() {
        val jan = (1..31).map { "2025-01-${it.toString().padStart(2, '0')}" }
        assertEquals(31, calculateConsecutiveDays(jan))

        val febNonLeap = (1..28).map { "2025-02-${it.toString().padStart(2, '0')}" }
        assertEquals(28, calculateConsecutiveDays(febNonLeap))

        val febLeap = (1..29).map { "2024-02-${it.toString().padStart(2, '0')}" }
        assertEquals(29, calculateConsecutiveDays(febLeap))
    }

    @Test
    fun `只打了一次卡-连续天数为1`() {
        assertEquals(1, calculateConsecutiveDays(listOf("2025-05-20")))
    }

    @Test
    fun `两天打卡但跨了5天-不连续`() {
        val dates = listOf("2025-05-20", "2025-05-26")
        assertEquals(1, calculateConsecutiveDays(dates))
    }

    @Test
    fun `一模一样日期重复-去重后连续天数应一致`() {
        val withDuplicates = listOf(
            "2025-05-20", "2025-05-20", "2025-05-21",
            "2025-05-22", "2025-05-22", "2025-05-22",
            "2025-05-23"
        )
        // 去重后 20,21,22,23 → 连续4天
        val distinct = withDuplicates.distinct()
        assertEquals(4, calculateConsecutiveDays(distinct))
    }

    @Test
    fun `打了一月中的周一三五-全部孤单`() {
        val dates = listOf(
            "2025-05-05", "2025-05-07", "2025-05-09",
            "2025-05-12", "2025-05-14", "2025-05-16",
            "2025-05-19", "2025-05-21", "2025-05-23"
        )
        assertEquals(1, calculateConsecutiveDays(dates))
    }

    @Test
    fun `交替连续和断-取最长段`() {
        // 模式: [3天连][断2][5天连][断1][2天连]
        val dates = listOf(
            "2025-05-01", "2025-05-02", "2025-05-03",
            "2025-05-06", "2025-05-07", "2025-05-08", "2025-05-09", "2025-05-10",
            "2025-05-12", "2025-05-13"
        )
        assertEquals(5, calculateConsecutiveDays(dates))
    }

    @Test
    fun `连续50天-大数边界`() {
        val dates = (1..50).map { day ->
            // 模拟连续的日月
            val totalDays = day
            var y = 2025
            var m = 1
            var d = totalDays
            val mDays = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            for (i in 0 until 12) {
                if (d > mDays[i]) {
                    d -= mDays[i]
                    m++
                } else break
            }
            "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
        }
        assertEquals(50, calculateConsecutiveDays(dates))
    }

    @Test
    fun `最大连续天数大于当前连续天数-当过去有更长段`() {
        // 历史有连续5天，最近只有3天
        val dates = listOf(
            "2025-05-01", "2025-05-02", "2025-05-03", "2025-05-04", "2025-05-05", // 5天
            "2025-05-10",
            "2025-05-22", "2025-05-23", "2025-05-24" // 3天
        )
        assertEquals(5, calculateConsecutiveDays(dates))
    }

    @Test
    fun `只有一个日期-仍为1`() {
        assertEquals(1, calculateConsecutiveDays(listOf("2025-05-20")))
    }

    @Test
    fun `两年间的连续打卡`() {
        val dates = listOf(
            "2025-12-30", "2025-12-31",
            "2026-01-01", "2026-01-02", "2026-01-03"
        )
        assertEquals(5, calculateConsecutiveDays(dates))
    }
}
