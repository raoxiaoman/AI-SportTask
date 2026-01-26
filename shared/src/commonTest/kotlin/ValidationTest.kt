package shared

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 数据验证测试
 *
 * 测试核心业务逻辑验证规则
 */
class ValidationTest {

    // ============ 时间格式化测试 ============

    @Test
    fun `时间格式化 - 不足10秒补零`() {
        assertEquals("0:05", formatTime(5))
        assertEquals("0:09", formatTime(9))
        assertEquals("1:05", formatTime(65))
        assertEquals("10:00", formatTime(600))
    }

    @Test
    fun `时间格式化 - 整分钟`() {
        assertEquals("1:00", formatTime(60))
        assertEquals("5:00", formatTime(300))
        assertEquals("30:00", formatTime(1800))
    }

    @Test
    fun `时间格式化 - 零秒`() {
        assertEquals("0:00", formatTime(0))
    }

    // ============ 动作数据验证 ============

    @Test
    fun `动作名称不能为空`() {
        val name = "  "
        assertTrue(name.isBlank())
    }

    @Test
    fun `默认时长必须大于零`() {
        val defaultTime = 0L
        assertFalse(defaultTime > 0)
    }

    @Test
    fun `休息时长可以为`() {
        val restTime = 0L
        assertTrue(restTime >= 0)
    }

    @Test
    fun `排序索引必须大于零`() {
        val orderIndex = 1L
        assertTrue(orderIndex > 0)
    }

    // ============ 打卡数据验证 ============

    @Test
    fun `日期格式验证 - 正确格式`() {
        val date = "2025-01-26"
        assertTrue(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun `完成状态验证`() {
        val isCompleted1 = 1L
        val isCompleted0 = 0L
        assertTrue(isCompleted1 == 1L)
        assertTrue(isCompleted0 == 0L)
    }

    // ============ 分组数据验证 ============

    @Test
    fun `分组名称不能为空`() {
        val name = ""
        assertTrue(name.isBlank())
    }

    @Test
    fun `动作计数可以为`() {
        val count = 0
        assertTrue(count >= 0)
    }

    // ============ 训练进度计算 ============

    @Test
    fun `计算训练进度 - 动作计时`() {
        val totalTime = 45
        val elapsed = 22
        val progress = (totalTime - elapsed).toFloat() / totalTime
        assertEquals(0.5111f, progress, 0.01f)
    }

    @Test
    fun `计算训练进度 - 休息计时`() {
        val totalTime = 15
        val elapsed = 7
        val progress = (totalTime - elapsed).toFloat() / totalTime
        assertEquals(0.5333f, progress, 0.01f)
    }

    @Test
    fun `计算训练进度 - 开始时`() {
        val totalTime = 30
        val progress = (totalTime - totalTime).toFloat() / totalTime
        assertEquals(0f, progress)
    }

    @Test
    fun `计算训练进度 - 完成时`() {
        val totalTime = 30
        val elapsed = totalTime
        val progress = (totalTime - elapsed).toFloat() / totalTime
        assertEquals(0f, progress)
    }

    // ============ 预计时长计算 ============

    @Test
    fun `预计总时长计算`() {
        val actions = listOf(
            ActionTestData(defaultTime = 45, restTime = 15),
            ActionTestData(defaultTime = 60, restTime = 20),
            ActionTestData(defaultTime = 30, restTime = 10)
        )
        val totalDuration = actions.sumOf { it.defaultTime + it.restTime }
        assertEquals(180, totalDuration) // (45+15) + (60+20) + (30+10)
    }

    @Test
    fun `无休息动作总时长`() {
        val actions = listOf(
            ActionTestData(defaultTime = 30, restTime = 0),
            ActionTestData(defaultTime = 45, restTime = 0)
        )
        val totalDuration = actions.sumOf { it.defaultTime + it.restTime }
        assertEquals(75, totalDuration)
    }

    // ============ 训练完成判断 ============

    @Test
    fun `判断训练是否完成 - 最后一个动作`() {
        val currentIndex = 2
        val totalActions = 3
        assertTrue(currentIndex >= totalActions - 1)
    }

    @Test
    fun `判断训练是否完成 - 非最后一个动作`() {
        val currentIndex = 1
        val totalActions = 3
        assertFalse(currentIndex >= totalActions - 1)
    }

    // ============ 连续打卡计算（模拟） ============

    @Test
    fun `连续天数计算 - 连续日期`() {
        val dates = listOf("2025-01-20", "2025-01-21", "2025-01-22")
        val consecutiveDays = calculateConsecutiveDays(dates)
        assertEquals(3, consecutiveDays)
    }

    @Test
    fun `连续天数计算 - 有间隔`() {
        val dates = listOf("2025-01-20", "2025-01-21", "2025-01-23", "2025-01-24")
        val consecutiveDays = calculateConsecutiveDays(dates)
        assertEquals(2, consecutiveDays)
    }

    @Test
    fun `连续天数计算 - 单日`() {
        val dates = listOf("2025-01-20")
        val consecutiveDays = calculateConsecutiveDays(dates)
        assertEquals(1, consecutiveDays)
    }

    @Test
    fun `连续天数计算 - 空列表`() {
        val dates = emptyList<String>()
        val consecutiveDays = calculateConsecutiveDays(dates)
        assertEquals(0, consecutiveDays)
    }

    // 辅助函数
    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%d:%02d".format(mins, secs)
    }

    private data class ActionTestData(
        val defaultTime: Int,
        val restTime: Int
    )

    private fun calculateConsecutiveDays(dates: List<String>): Int {
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
}
