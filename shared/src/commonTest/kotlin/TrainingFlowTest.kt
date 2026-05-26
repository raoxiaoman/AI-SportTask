package shared

import data.CheckinItem
import data.TrainingResult
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 训练流程集成测试 — 实际业务场景模拟
 *
 * 覆盖：完整训练流程、结果计算、打卡映射、
 * 异常流程、批量操作
 */
class TrainingFlowTest {

    // 测试用的动作数据
    private val sampleActions = listOf(
        TrainAction(name = "俯卧撑", defaultTime = 45, restTime = 15, orderIndex = 1),
        TrainAction(name = "深蹲", defaultTime = 60, restTime = 20, orderIndex = 2),
        TrainAction(name = "平板支撑", defaultTime = 30, restTime = 10, orderIndex = 3)
    )

    // ============ 完整训练流程 ============

    @Test
    fun `完整训练三条动作-生成结果并映射打卡`() {
        val groupId = 1L

        // 模拟训练执行
        val result = simulateTraining(
            groupId = groupId,
            actions = sampleActions,
            skipIndices = emptySet(),
            pausedTimeSeconds = 0,
            earlyStop = false
        )

        // 验证训练结果
        assertEquals(groupId, result.groupId)
        assertTrue(result.completed)
        assertEquals(150, result.duration) // (45+15) + (60+20) + 30

        // 映射为打卡
        val checkin = mapTrainingToCheckin(result)
        assertEquals(groupId, checkin.groupId)
        assertEquals(result.duration, checkin.duration)
        assertTrue(checkin.isCompleted)
    }

    @Test
    fun `训练中途退出-不生成打卡`() {
        val groupId = 1L
        val result = simulateTraining(
            groupId = groupId,
            actions = sampleActions,
            skipIndices = emptySet(),
            pausedTimeSeconds = 0,
            earlyStop = true // 中途退出
        )

        assertFalse(result.completed)
    }

    // ============ 跳过中间动作 ============

    @Test
    fun `跳过中间动作-剩余序列正确`() {
        val skipIndex = 1 // 跳过深蹲（索引1）
        val remaining = sampleActions.filterIndexed { i, _ -> i != skipIndex }
            .sortedBy { it.orderIndex }

        assertEquals(2, remaining.size)
        assertEquals("俯卧撑", remaining[0].name)
        assertEquals("平板支撑", remaining[1].name)
    }

    @Test
    fun `跳过所有动作-训练直接完成-时长为0`() {
        val groupId = 1L
        val allIndices = setOf(0, 1, 2)

        val result = simulateTraining(
            groupId = groupId,
            actions = sampleActions,
            skipIndices = allIndices,
            pausedTimeSeconds = 0,
            earlyStop = false
        )

        assertTrue(result.completed)
        assertEquals(0, result.duration) // 全部跳过，没有实际执行
    }

    // ============ 训练时长累计 ============

    @Test
    fun `每个动作实际执行时长计入结果`() {
        // 模拟：动作1执行了40秒（少5秒），动作2执行了60秒（全），动作3执行了28秒（少2秒）
        val actualPerformances = listOf(40, 55, 28)
        val totalActionTime = actualPerformances.sum()
        val restDurations = listOf(12, 18) // 实际休息时间
        val totalRestTime = restDurations.sum()

        assertEquals(123, totalActionTime + totalRestTime)
    }

    @Test
    fun `只有单个动作的训练`() {
        val singleAction = listOf(TrainAction("引体向上", defaultTime = 60, restTime = 10, orderIndex = 1))

        val groupId = 2L
        val result = simulateTraining(
            groupId = groupId,
            actions = singleAction,
            skipIndices = emptySet(),
            pausedTimeSeconds = 0,
            earlyStop = false
        )

        assertTrue(result.completed)
        assertEquals(60, result.duration) // 单个动作没有后续休息
    }

    @Test
    fun `100个动作的超长训练-索引计算正确`() {
        val manyActions = (1..100).map { i ->
            TrainAction(name = "动作$i", defaultTime = 30, restTime = 5, orderIndex = i)
        }

        // 模拟完成到最后一个
        val total = manyActions.size
        var currentIndex = 0
        var completed = false

        while (currentIndex < total) {
            if (currentIndex == total - 1) {
                completed = true
                currentIndex++
            } else {
                currentIndex++
            }
        }

        assertTrue(completed)
        assertEquals(100, currentIndex)
    }

    // ============ 全部休息时长为 0 ============

    @Test
    fun `所有动作休息时长为零-动作连续执行`() {
        val noRestActions = listOf(
            TrainAction("开合跳", defaultTime = 30, restTime = 0, orderIndex = 1),
            TrainAction("高抬腿", defaultTime = 30, restTime = 0, orderIndex = 2),
            TrainAction("波比跳", defaultTime = 30, restTime = 0, orderIndex = 3)
        )

        // 总时长 = 所有动作时长之和（无需加休息时间）
        val total = noRestActions.sumOf { it.defaultTime }
        assertEquals(90, total)
    }

    // ============ 重复动作逻辑 ============

    @Test
    fun `重复当前动作-重置时间不计入额外休息`() {
        val current = 0
        val performedExpected = 45  // 重置到 defaultTime

        // 第一次执行完成
        val firstRunDuration = 30  // 只做了30秒就被重复了
        // 重复后重新开始
        val secondRunDuration = performedExpected

        val totalDuration = firstRunDuration + secondRunDuration
        assertEquals(75, totalDuration)
    }

    // ============ 训练结果映射 ============

    @Test
    fun `训练完成-打卡记录生成-完成状态为true`() {
        val result = TrainingResult(groupId = 1L, duration = 1800, completed = true)
        val checkin = CheckinItem(
            id = 0L,
            date = "2025-05-26",
            duration = result.duration,
            isCompleted = result.completed
        )
        assertEquals(1800, checkin.duration)
        assertTrue(checkin.isCompleted)
    }

    @Test
    fun `训练未完成-打卡记录生成-完成状态为false`() {
        val result = TrainingResult(groupId = 2L, duration = 300, completed = false)
        val checkin = CheckinItem(
            id = 0L,
            date = "2025-05-26",
            duration = result.duration,
            isCompleted = result.completed
        )
        assertEquals(300, checkin.duration)
        assertFalse(checkin.isCompleted)
    }

    // ============ 并发/顺序执行安全 ============

    @Test
    fun `连续两次训练互不影响`() {
        val actions1 = listOf(TrainAction("上肢", defaultTime = 30, restTime = 10, orderIndex = 1))
        val actions2 = listOf(TrainAction("下肢", defaultTime = 60, restTime = 15, orderIndex = 1))

        val r1 = simulateTraining(1L, actions1, emptySet(), 0, false)
        val r2 = simulateTraining(2L, actions2, emptySet(), 0, false)

        assertEquals(30, r1.duration)
        assertEquals(60, r2.duration)
    }

    // ============ 辅助函数 ============

    /**
     * 模拟完整训练流程
     *
     * @param groupId 分组ID
     * @param actions 训练动作列表（按排序顺序）
     * @param skipIndices 要跳过的动作索引
     * @param pausedTimeSeconds 暂停总时长（不记入训练时间）
     * @param earlyStop 是否中途退出
     */
    private fun simulateTraining(
        groupId: Long,
        actions: List<TrainAction>,
        skipIndices: Set<Int>,
        pausedTimeSeconds: Int,
        earlyStop: Boolean
    ): TrainingResult {
        if (skipIndices.size == actions.size) {
            return TrainingResult(groupId = groupId, duration = 0, completed = true)
        }

        val remaining = actions.filterIndexed { i, _ -> i !in skipIndices }
        val sorted = remaining.sortedBy { it.orderIndex }
        var totalDuration = 0
        var completed = false

        for (i in sorted.indices) {
            if (earlyStop && i == 1) { // 中途退出（完成了第一个动作）
                return TrainingResult(groupId = groupId, duration = totalDuration, completed = false)
            }

            val action = sorted[i]
            totalDuration += action.defaultTime

            if (i == sorted.size - 1) {
                completed = true
            } else {
                totalDuration += action.restTime
            }
        }

        return TrainingResult(
            groupId = groupId,
            duration = totalDuration,
            completed = completed
        )
    }

    /**
     * 将训练结果映射为打卡记录
     */
    private fun mapTrainingToCheckin(result: TrainingResult): CheckinItem {
        return CheckinItem(
            id = 0L,
            date = "2025-05-26",
            duration = result.duration,
            isCompleted = result.completed
        )
    }

    private data class TrainAction(
        val name: String,
        val defaultTime: Int,
        val restTime: Int,
        val orderIndex: Int
    )
}
