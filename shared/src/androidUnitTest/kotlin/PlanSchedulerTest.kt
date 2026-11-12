package shared

import data.plan.PlanScheduler
import data.plan.PlanState
import data.plan.TodayTask
import data.plan.rehabPhases
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanSchedulerTest {

    private val phases = rehabPhases()

    @Test
    fun `dayIndex-同一天为0`() {
        assertEquals(0, PlanScheduler.dayIndex("2026-11-10", "2026-11-10"))
    }

    @Test
    fun `dayIndex-跨月计算正确`() {
        assertEquals(20, PlanScheduler.dayIndex("2026-11-10", "2026-11-30"))
        assertEquals(21, PlanScheduler.dayIndex("2026-11-10", "2026-12-01"))
    }

    @Test
    fun `dayIndex-开始日之前返回-1`() {
        assertEquals(-1, PlanScheduler.dayIndex("2026-11-10", "2026-11-09"))
    }

    @Test
    fun `计划总天数为56天`() {
        assertEquals(56, PlanScheduler.totalDays(phases))
    }

    @Test
    fun `resolve-阶段边界`() {
        assertEquals(1, PlanScheduler.resolve(phases, 0)?.phase?.index)
        assertEquals(1, PlanScheduler.resolve(phases, 13)?.phase?.index)
        assertEquals(2, PlanScheduler.resolve(phases, 14)?.phase?.index)
        assertEquals(2, PlanScheduler.resolve(phases, 27)?.phase?.index)
        assertEquals(3, PlanScheduler.resolve(phases, 28)?.phase?.index)
        assertEquals(3, PlanScheduler.resolve(phases, 55)?.phase?.index)
        assertNull(PlanScheduler.resolve(phases, 56))
        assertNull(PlanScheduler.resolve(phases, -1))
    }

    @Test
    fun `resolve-阶段内天数从0开始`() {
        assertEquals(0, PlanScheduler.resolve(phases, 14)?.dayInPhase)
        assertEquals(5, PlanScheduler.resolve(phases, 19)?.dayInPhase)
        assertEquals(0, PlanScheduler.resolve(phases, 28)?.dayInPhase)
    }

    @Test
    fun `isTrainingDay-阶段1每天训练`() {
        val slot = PlanScheduler.resolve(phases, 3)!!
        assertTrue(PlanScheduler.isTrainingDay(slot))
    }

    @Test
    fun `isTrainingDay-阶段2隔日训练`() {
        val day0 = PlanScheduler.resolve(phases, 14)!!
        val day1 = PlanScheduler.resolve(phases, 15)!!
        val day2 = PlanScheduler.resolve(phases, 16)!!
        assertTrue(PlanScheduler.isTrainingDay(day0))
        assertFalse(PlanScheduler.isTrainingDay(day1))
        assertTrue(PlanScheduler.isTrainingDay(day2))
    }

    @Test
    fun `isTrainingDay-阶段3同样隔日`() {
        assertTrue(PlanScheduler.isTrainingDay(PlanScheduler.resolve(phases, 28)!!))
        assertFalse(PlanScheduler.isTrainingDay(PlanScheduler.resolve(phases, 29)!!))
    }

    @Test
    fun `estimatedMinutes-阶段1约15分钟`() {
        val minutes = PlanScheduler.estimatedMinutes(phases[0])
        assertTrue(minutes in 10..18, "实际 $minutes 分钟")
    }

    @Test
    fun `estimatedMinutes-阶段3包含有氧约30分钟`() {
        val minutes = PlanScheduler.estimatedMinutes(phases[2])
        assertTrue(minutes in 25..35, "实际 $minutes 分钟")
    }

    @Test
    fun `reminderText-训练日`() {
        val text = PlanScheduler.reminderText(task(isTrainingDay = true))
        assertTrue(text.contains("阶段2"))
        assertTrue(text.contains("9 个动作"))
    }

    @Test
    fun `reminderText-休息日`() {
        val text = PlanScheduler.reminderText(task(isTrainingDay = false))
        assertTrue(text.contains("休息日"))
    }

    @Test
    fun `reminderText-已完成优先于休息日`() {
        val text = PlanScheduler.reminderText(task(isTrainingDay = false, completed = true))
        assertTrue(text.contains("已完成"))
    }

    @Test
    fun `reminderText-计划结束`() {
        val text = PlanScheduler.reminderText(task(finished = true))
        assertTrue(text.contains("已完成"))
    }

    @Test
    fun `reminderText-未导入`() {
        assertEquals("今天还没训练呢？来动一动吧！", PlanScheduler.reminderText(null))
    }

    @Test
    fun `PlanState-默认阶段为内置计划`() {
        val state = PlanState(startDate = "2026-11-10")
        assertEquals(3, state.phases.size)
        assertEquals(emptyMap(), state.groupIds)
    }

    private fun task(
        isTrainingDay: Boolean = true,
        completed: Boolean = false,
        finished: Boolean = false
    ) = TodayTask(
        planName = "8 周吃练计划",
        startDate = "2026-11-10",
        phaseTitle = "阶段2 稳定",
        phaseIndex = 2,
        dayIndex = 14,
        dayInPhase = 0,
        phaseDays = 14,
        isTrainingDay = isTrainingDay,
        groupId = 1L,
        actionCount = 9,
        estimatedMinutes = 20,
        completed = completed,
        finished = finished
    )
}
