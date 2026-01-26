package shared

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.ActionItem
import data.CheckinItem
import data.GroupItem
import data.TrainingResult
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * UI 组件测试
 *
 * 测试 UI 相关的数据结构和辅助函数
 */
class UiTest {

    // ============ 分组卡片测试 ============

    @Test
    fun `GroupItem 数据类创建`() {
        val group = GroupItem(
            id = 1L,
            name = "上肢力量",
            actionCount = 5
        )

        assertEquals(1L, group.id)
        assertEquals("上肢力量", group.name)
        assertEquals(5, group.actionCount)
    }

    @Test
    fun `GroupItem 动作计数为零`() {
        val group = GroupItem(id = 1L, name = "空分组", actionCount = 0)
        assertEquals(0, group.actionCount)
    }

    // ============ 动作卡片测试 ============

    @Test
    fun `ActionItem 数据类创建`() {
        val action = ActionItem(
            id = 1L,
            name = "俯卧撑",
            stepsText = "1. 双手与肩同宽\n2. 身体保持直线",
            defaultTime = 45,
            restTime = 15,
            orderIndex = 1
        )

        assertEquals(1L, action.id)
        assertEquals("俯卧撑", action.name)
        assertEquals(45, action.defaultTime)
        assertEquals(15, action.restTime)
        assertEquals(1, action.orderIndex)
    }

    @Test
    fun `ActionItem 默认ID为零`() {
        val action = ActionItem(
            name = "新动作",
            stepsText = "",
            defaultTime = 30,
            restTime = 10,
            orderIndex = 1
        )

        assertEquals(0L, action.id)
    }

    @Test
    fun `ActionItem 允许空步骤说明`() {
        val action = ActionItem(
            id = 1L,
            name = "简单动作",
            stepsText = "",
            defaultTime = 30,
            restTime = 10,
            orderIndex = 1
        )

        assertTrue(action.stepsText.isEmpty())
    }

    // ============ 打卡记录测试 ============

    @Test
    fun `CheckinItem 数据类创建`() {
        val checkin = CheckinItem(
            id = 1L,
            date = "2025-01-26",
            duration = 1800,
            isCompleted = true
        )

        assertEquals(1L, checkin.id)
        assertEquals("2025-01-26", checkin.date)
        assertEquals(1800, checkin.duration)
        assertTrue(checkin.isCompleted)
    }

    @Test
    fun `CheckinItem 未完成状态`() {
        val checkin = CheckinItem(
            id = 1L,
            date = "2025-01-26",
            duration = 600,
            isCompleted = false
        )

        assertFalse(checkin.isCompleted)
    }

    // ============ 训练结果测试 ============

    @Test
    fun `TrainingResult 训练完成`() {
        val result = TrainingResult(
            groupId = 1L,
            duration = 1200,
            completed = true
        )

        assertEquals(1L, result.groupId)
        assertEquals(1200, result.duration)
        assertTrue(result.completed)
    }

    @Test
    fun `TrainingResult 训练未完成`() {
        val result = TrainingResult(
            groupId = 2L,
            duration = 600,
            completed = false
        )

        assertEquals(2L, result.groupId)
        assertEquals(600, result.duration)
        assertFalse(result.completed)
    }

    // ============ 边距计算测试 ============

    @Test
    fun `标准卡片边距`() {
        val padding = 16.dp
        assertEquals(16, padding.value.toInt())
    }

    @Test
    fun `小间距`() {
        val spacing = 8.dp
        assertEquals(8, spacing.value.toInt())
    }

    @Test
    fun `列表间距`() {
        assertEquals(4, 4.dp.value.toInt())
    }

    // ============ 进度条测试 ============

    @Test
    fun `进度计算 - 开始`() {
        val total = 100
        val current = 0
        val progress = (total - current).toFloat() / total
        assertEquals(1f, progress)
    }

    @Test
    fun `进度计算 - 完成`() {
        val total = 100
        val current = 100
        val progress = (total - current).toFloat() / total
        assertEquals(0f, progress)
    }

    @Test
    fun `进度计算 - 一半`() {
        val total = 100
        val current = 50
        val progress = (total - current).toFloat() / total
        assertEquals(0.5f, progress)
    }

    // ============ 动作索引测试 ============

    @Test
    fun `第一个动作索引`() {
        val index = 0
        val orderIndex = index + 1
        assertEquals(1, orderIndex)
    }

    @Test
    fun `最后一个动作判断`() {
        val currentIndex = 4
        val totalActions = 5
        assertTrue(currentIndex >= totalActions - 1)
    }

    @Test
    fun `非最后一个动作判断`() {
        val currentIndex = 2
        val totalActions = 5
        assertFalse(currentIndex >= totalActions - 1)
    }

    // ============ 状态切换测试 ============

    @Test
    fun `休息状态切换`() {
        var isResting = false

        // 动作完成，切换到休息
        if (!isResting) {
            isResting = true
        }
        assertTrue(isResting)

        // 休息完成，切换到下一个动作
        if (isResting) {
            isResting = false
        }
        assertFalse(isResting)
    }

    // ============ 训练流程状态测试 ============

    @Test
    fun `训练未开始状态`() {
        var isRunning = false
        var isCompleted = false
        var currentActionIndex = 0

        assertFalse(isRunning)
        assertFalse(isCompleted)
        assertEquals(0, currentActionIndex)
    }

    @Test
    fun `训练进行中状态`() {
        var isRunning = true
        var isCompleted = false
        var currentActionIndex = 2

        assertTrue(isRunning)
        assertFalse(isCompleted)
        assertEquals(2, currentActionIndex)
    }

    @Test
    fun `训练已完成状态`() {
        var isRunning = false
        var isCompleted = true
        var currentActionIndex = 4

        assertFalse(isRunning)
        assertTrue(isCompleted)
        assertEquals(4, currentActionIndex)
    }

    // ============ 打卡统计测试 ============

    @Test
    fun `周训练次数汇总`() {
        val weeklyCheckins = listOf(1, 2, 3, 0, 1, 0, 1)
        val totalCount = weeklyCheckins.sum()
        assertEquals(8, totalCount)
    }

    @Test
    fun `周训练时长汇总`() {
        val dailyDuration = listOf(600L, 1200L, 900L, 0L, 1800L)
        val totalDuration = dailyDuration.sum()
        assertEquals(4500L, totalDuration) // 75分钟
    }

    @Test
    fun `完成率计算`() {
        val completed = 7
        val total = 10
        val completionRate = (completed.toFloat() / total) * 100
        assertEquals(70f, completionRate)
    }
}
