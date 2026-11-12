package data.plan

import data.daysBetweenStrings

/**
 * 阶段定位结果：当前处于哪个阶段、是阶段的第几天（0 起）
 */
data class PhaseSlot(
    val phase: PlanPhase,
    val dayInPhase: Int
)

/**
 * 今日任务快照
 */
data class TodayTask(
    val planName: String,
    val startDate: String,
    val phaseTitle: String,
    val phaseIndex: Int,
    val dayIndex: Int,
    val dayInPhase: Int,
    val phaseDays: Int,
    val isTrainingDay: Boolean,
    val groupId: Long?,
    val actionCount: Int,
    val estimatedMinutes: Int,
    val completed: Boolean,
    val finished: Boolean = false,
    val notStarted: Boolean = false
)

/**
 * 计划调度：纯函数，便于单测；不依赖数据库与平台
 */
object PlanScheduler {

    /** 起始日到今天的自然日差；开始日之前返回 -1 */
    fun dayIndex(startDate: String, today: String): Int {
        val diff = daysBetweenStrings(startDate, today)
        if (diff == Int.MAX_VALUE || diff < 0) return -1
        return diff
    }

    fun totalDays(phases: List<PlanPhase>): Int = phases.sumOf { it.days }

    fun phaseStartDay(phases: List<PlanPhase>, phaseIndex: Int): Int =
        phases.filter { it.index < phaseIndex }.sumOf { it.days }

    /** 按天序号解析阶段；超出计划范围返回 null */
    fun resolve(phases: List<PlanPhase>, dayIndex: Int): PhaseSlot? {
        if (dayIndex < 0) return null
        var start = 0
        for (phase in phases) {
            if (dayIndex < start + phase.days) {
                return PhaseSlot(phase, dayIndex - start)
            }
            start += phase.days
        }
        return null
    }

    /** 训练日判断：每天训练的阶段恒为 true；隔日阶段按阶段内偶数天 */
    fun isTrainingDay(slot: PhaseSlot): Boolean =
        !slot.phase.everyOtherDay || slot.dayInPhase % 2 == 0

    /** 估算时长（分钟）：动作时长 + 休息时长，向上取整 */
    fun estimatedMinutes(phase: PlanPhase): Int {
        val seconds = phase.exercises.sumOf { it.seconds + it.restSeconds }
        return (seconds + 59) / 60
    }

    /** 通知/卡片上的一句话摘要 */
    fun reminderText(task: TodayTask?): String = when {
        task == null -> "今天还没训练呢？来动一动吧！"
        task.notStarted -> "康复计划 ${task.startDate} 开始，今天先做 5 分钟踝泵热身"
        task.finished -> "8 周计划已完成，保持每周 2 次训练"
        task.completed -> "今天已完成，睡前做 5 分钟拉伸即可"
        !task.isTrainingDay -> "今天是休息日，做 5 分钟踝泵 + 小腿拉伸"
        else -> "今天：${task.phaseTitle} · ${task.actionCount} 个动作 · 约 ${task.estimatedMinutes} 分钟"
    }
}
