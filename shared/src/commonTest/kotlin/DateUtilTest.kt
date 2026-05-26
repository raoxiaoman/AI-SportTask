package shared

import data.absoluteDayCount
import data.areConsecutiveDays
import data.calculateConsecutiveDays
import data.daysBetweenStrings
import data.formatTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 日期工具函数测试
 *
 * 覆盖：formatTime、absoluteDayCount、daysBetweenStrings、
 * areConsecutiveDays、calculateConsecutiveDays
 */
class DateUtilTest {

    // ============ formatTime 测试 ============

    @Test
    fun `formatTime-零秒`() {
        assertEquals("0:00", formatTime(0))
    }

    @Test
    fun `formatTime-不足一分钟`() {
        assertEquals("0:05", formatTime(5))
        assertEquals("0:30", formatTime(30))
        assertEquals("0:59", formatTime(59))
    }

    @Test
    fun `formatTime-整分钟`() {
        assertEquals("1:00", formatTime(60))
        assertEquals("5:00", formatTime(300))
        assertEquals("30:00", formatTime(1800))
    }

    @Test
    fun `formatTime-跨分钟`() {
        assertEquals("1:05", formatTime(65))
        assertEquals("2:30", formatTime(150))
        assertEquals("10:15", formatTime(615))
    }

    @Test
    fun `formatTime-一小时以上`() {
        assertEquals("60:00", formatTime(3600))
        assertEquals("90:00", formatTime(5400))
        assertEquals("120:30", formatTime(7230))
    }

    @Test
    fun `formatTime-个位数秒补零`() {
        assertEquals("1:01", formatTime(61))
        assertEquals("2:02", formatTime(122))
        assertEquals("3:03", formatTime(183))
    }

    @Test
    fun `formatTime-只有秒`() {
        assertEquals("0:01", formatTime(1))
        assertEquals("0:07", formatTime(7))
    }

    @Test
    fun `formatTime-边界值`() {
        assertEquals("0:00", formatTime(0))
        assertEquals("0:01", formatTime(1))
        assertEquals("100:00", formatTime(6000))
    }

    // ============ absoluteDayCount 测试 ============

    @Test
    fun `absoluteDayCount-同一年相隔一天`() {
        val day1 = absoluteDayCount(2025, 1, 26)
        val day2 = absoluteDayCount(2025, 1, 27)
        assertEquals(1, day2 - day1)
    }

    @Test
    fun `absoluteDayCount-跨年`() {
        val day1 = absoluteDayCount(2025, 12, 31)
        val day2 = absoluteDayCount(2026, 1, 1)
        assertEquals(1, day2 - day1)
    }

    @Test
    fun `absoluteDayCount-跨月`() {
        val day1 = absoluteDayCount(2025, 1, 31)
        val day2 = absoluteDayCount(2025, 2, 1)
        assertEquals(1, day2 - day1)
    }

    @Test
    fun `absoluteDayCount-同一天`() {
        val day1 = absoluteDayCount(2025, 1, 26)
        val day2 = absoluteDayCount(2025, 1, 26)
        assertEquals(0, day2 - day1)
    }

    @Test
    fun `absoluteDayCount-闰年二月`() {
        val day1 = absoluteDayCount(2024, 2, 28)
        val day2 = absoluteDayCount(2024, 3, 1)
        assertEquals(2, day2 - day1) // 2024是闰年，2月有29天
    }

    @Test
    fun `absoluteDayCount-非闰年二月`() {
        val day1 = absoluteDayCount(2025, 2, 28)
        val day2 = absoluteDayCount(2025, 3, 1)
        assertEquals(1, day2 - day1) // 2025不是闰年
    }

    @Test
    fun `absoluteDayCount-闰年判断-能被4和400整除`() {
        val day1 = absoluteDayCount(2000, 2, 28)
        val day2 = absoluteDayCount(2000, 3, 1)
        assertEquals(2, day2 - day1) // 2000是闰年
    }

    @Test
    fun `absoluteDayCount-世纪年不是闰年`() {
        val day1 = absoluteDayCount(2100, 2, 28)
        val day2 = absoluteDayCount(2100, 3, 1)
        assertEquals(1, day2 - day1) // 2100不是闰年
    }

    @Test
    fun `absoluteDayCount-大跨度年份`() {
        val day1 = absoluteDayCount(2020, 1, 1)
        val day2 = absoluteDayCount(2025, 1, 1)
        assertEquals(1827, day2 - day1) // 2020是闰年 + 1826普通天
    }

    // ============ daysBetweenStrings 测试 ============

    @Test
    fun `daysBetween-相邻日期差1`() {
        assertEquals(1, daysBetweenStrings("2025-01-26", "2025-01-27"))
    }

    @Test
    fun `daysBetween-同一天差0`() {
        assertEquals(0, daysBetweenStrings("2025-01-26", "2025-01-26"))
    }

    @Test
    fun `daysBetween-跨月差7天`() {
        assertEquals(7, daysBetweenStrings("2025-01-25", "2025-02-01"))
    }

    @Test
    fun `daysBetween-跨年差1天`() {
        assertEquals(1, daysBetweenStrings("2025-12-31", "2026-01-01"))
    }

    @Test
    fun `daysBetween-非法格式返回大值`() {
        // 对于无效输入，返回 Int.MAX_VALUE
        val result = daysBetweenStrings("invalid", "2025-01-27")
        assertTrue(result > 1000000 || result < 0)
    }

    // ============ areConsecutiveDays 测试 ============

    @Test
    fun `areConsecutiveDays-相邻返回true`() {
        assertTrue(areConsecutiveDays("2025-01-26", "2025-01-27"))
    }

    @Test
    fun `areConsecutiveDays-同一天返回false`() {
        assertFalse(areConsecutiveDays("2025-01-26", "2025-01-26"))
    }

    @Test
    fun `areConsecutiveDays-相隔2天返回false`() {
        assertFalse(areConsecutiveDays("2025-01-26", "2025-01-28"))
    }

    @Test
    fun `areConsecutiveDays-跨月相邻`() {
        assertTrue(areConsecutiveDays("2025-01-31", "2025-02-01"))
    }

    @Test
    fun `areConsecutiveDays-跨年相邻`() {
        assertTrue(areConsecutiveDays("2025-12-31", "2026-01-01"))
    }

    @Test
    fun `areConsecutiveDays-反向顺序返回false`() {
        // 因为是相邻测试，daysBetweenStrings(from, to) 要求 from 早于 to
        val result = areConsecutiveDays("2025-01-27", "2025-01-26")
        assertEquals(-1, daysBetweenStrings("2025-01-27", "2025-01-26"))
    }

    // ============ calculateConsecutiveDays 测试 ============

    @Test
    fun `calcConsecutiveDays-空列表返回0`() {
        assertEquals(0, calculateConsecutiveDays(emptyList()))
    }

    @Test
    fun `calcConsecutiveDays-单日返回1`() {
        assertEquals(1, calculateConsecutiveDays(listOf("2025-01-26")))
    }

    @Test
    fun `calcConsecutiveDays-连续三天`() {
        val dates = listOf("2025-01-20", "2025-01-21", "2025-01-22")
        assertEquals(3, calculateConsecutiveDays(dates))
    }

    @Test
    fun `calcConsecutiveDays-有间隔取最大连续`() {
        val dates = listOf("2025-01-20", "2025-01-21", "2025-01-23", "2025-01-24", "2025-01-25")
        assertEquals(3, calculateConsecutiveDays(dates)) // 23 24 25
    }

    @Test
    fun `calcConsecutiveDays-等长的两段连续取任意一段`() {
        val dates = listOf("2025-01-20", "2025-01-21", "2025-01-24", "2025-01-25")
        assertEquals(2, calculateConsecutiveDays(dates)) // 两段都是2
    }

    @Test
    fun `calcConsecutiveDays-全部不连续`() {
        val dates = listOf("2025-01-20", "2025-01-22", "2025-01-24")
        assertEquals(1, calculateConsecutiveDays(dates)) // 每个都是孤单1天
    }

    @Test
    fun `calcConsecutiveDays-跨月连续`() {
        val dates = listOf("2025-01-30", "2025-01-31", "2025-02-01")
        assertEquals(3, calculateConsecutiveDays(dates))
    }

    @Test
    fun `calcConsecutiveDays-跨年连续`() {
        val dates = listOf("2025-12-30", "2025-12-31", "2026-01-01")
        assertEquals(3, calculateConsecutiveDays(dates))
    }

    @Test
    fun `calcConsecutiveDays-未排序的日期列表`() {
        val dates = listOf("2025-01-22", "2025-01-20", "2025-01-21")
        assertEquals(3, calculateConsecutiveDays(dates)) // 排序后20 21 22
    }

    @Test
    fun `calcConsecutiveDays-大连续天数`() {
        val dates = (1..14).map { day ->
            "2025-01-${day.toString().padStart(2, '0')}"
        }
        assertEquals(14, calculateConsecutiveDays(dates))
    }

    @Test
    fun `calcConsecutiveDays-月末月初连续-2月`() {
        val dates = listOf(
            "2024-02-27", "2024-02-28", "2024-02-29", // 闰年2月
            "2024-03-01"
        )
        assertEquals(4, calculateConsecutiveDays(dates))
    }

    @Test
    fun `calcConsecutiveDays-非闰年2月末3月初`() {
        val dates = listOf(
            "2025-02-27", "2025-02-28",
            "2025-03-01"
        )
        assertEquals(3, calculateConsecutiveDays(dates))
    }

    @Test
    fun `calcConsecutiveDays-同一天出现多次`() {
        val dates = listOf("2025-01-20", "2025-01-20", "2025-01-21")
        // distinct后是20, 21 -> 连续2天
        val distinct = dates.distinct().sorted()
        assertEquals(2, calculateConsecutiveDays(distinct))
    }
}
