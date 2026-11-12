package data.plan

import data.SportTaskRepository
import data.remote.PlatformStorageProvider
import kotlinx.serialization.json.Json
import pages.dayOfWeekFromDate
import pages.daysAgoDateString
import pages.todayDateString

/**
 * 一周节奏中单日的状态（用于首页本周节奏条）
 */
data class WeekDayStatus(
    /** 星期标签：一/二/三/… */
    val label: String,
    /** YYYY-MM-DD */
    val date: String,
    val isToday: Boolean,
    /** 是否在计划日期范围内（可推算出训练日/休息日） */
    val inPlan: Boolean,
    val isTrainingDay: Boolean,
    val isCompleted: Boolean
)

/**
 * 计划仓库：把计划落成现有的「分组 + 动作」结构，状态存 PlatformStorage，
 * 不新增数据库表，避免 schema 迁移。
 */
object PlanRepository {

    private const val KEY_STATE = "rehab_plan_state"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun loadState(): PlanState? = runCatching {
        PlatformStorageProvider.get().load(KEY_STATE)
            ?.let { json.decodeFromString(PlanState.serializer(), it) }
    }.getOrNull()

    fun saveState(state: PlanState) {
        val text = json.encodeToString(PlanState.serializer(), state)
        PlatformStorageProvider.get().save(KEY_STATE, text)
    }

    fun hasPlan(): Boolean = loadState() != null

    /**
     * 导入内置 8 周康复计划；若已有计划先清理旧分组，避免重复
     */
    suspend fun importRehabPlan(startDate: String = todayDateString()): PlanState {
        loadState()?.let { deletePlanState(it) }

        val phases = rehabPhases()
        val groupIds = mutableMapOf<Int, Long>()

        for (phase in phases) {
            SportTaskRepository.addGroup(phase.groupName, startDate)
            val groupId = SportTaskRepository.getGroups().maxByOrNull { it.id }?.id ?: continue
            phase.exercises.forEachIndexed { index, exercise ->
                SportTaskRepository.addAction(
                    groupId = groupId,
                    name = exercise.name,
                    stepsText = exercise.detail,
                    defaultTime = exercise.seconds.toLong(),
                    restTime = exercise.restSeconds.toLong(),
                    orderIndex = (index + 1).toLong(),
                    createdAt = startDate
                )
            }
            groupIds[phase.index] = groupId
        }

        val state = PlanState(startDate = startDate, groupIds = groupIds, phases = phases)
        saveState(state)
        refreshReminder()
        return state
    }

    suspend fun deletePlan() {
        loadState()?.let { deletePlanState(it) }
        PlatformStorageProvider.get().remove(KEY_STATE)
        publishReminderPreview("", "", 0, todayDateString())
    }

    private suspend fun deletePlanState(state: PlanState) {
        state.groupIds.values.forEach { groupId ->
            SportTaskRepository.deleteActionsInGroup(groupId)
            SportTaskRepository.deleteGroup(groupId)
        }
    }

    /**
     * 计算今日任务；未导入计划返回 null
     */
    fun todayTask(today: String = todayDateString()): TodayTask? {
        val state = loadState() ?: return null
        val phases = state.phases.ifEmpty { rehabPhases() }
        val day = PlanScheduler.dayIndex(state.startDate, today)
        val total = PlanScheduler.totalDays(phases)

        if (day < 0) {
            return TodayTask(
                planName = state.name,
                startDate = state.startDate,
                phaseTitle = "计划未开始",
                phaseIndex = 0,
                dayIndex = day,
                dayInPhase = 0,
                phaseDays = 0,
                isTrainingDay = false,
                groupId = null,
                actionCount = 0,
                estimatedMinutes = 0,
                completed = false,
                notStarted = true
            )
        }

        val slot = PlanScheduler.resolve(phases, day)
        if (slot == null || day >= total) {
            return TodayTask(
                planName = state.name,
                startDate = state.startDate,
                phaseTitle = "计划已完成",
                phaseIndex = -1,
                dayIndex = day,
                dayInPhase = 0,
                phaseDays = 0,
                isTrainingDay = false,
                groupId = null,
                actionCount = 0,
                estimatedMinutes = 0,
                completed = false,
                finished = true
            )
        }

        val groupId = state.groupIds[slot.phase.index]
        val actionCount = groupId?.let { SportTaskRepository.countActionsSync(it) } ?: 0
        val completed = groupId != null && SportTaskRepository.hasCompletedSync(groupId, today)

        return TodayTask(
            planName = state.name,
            startDate = state.startDate,
            phaseTitle = slot.phase.title,
            phaseIndex = slot.phase.index,
            dayIndex = day,
            dayInPhase = slot.dayInPhase,
            phaseDays = slot.phase.days,
            isTrainingDay = PlanScheduler.isTrainingDay(slot),
            groupId = groupId,
            actionCount = actionCount,
            estimatedMinutes = PlanScheduler.estimatedMinutes(slot.phase),
            completed = completed
        )
    }

    /**
     * 计算本周（周一至周日）的训练节奏；未导入计划返回 null
     */
    fun weekRhythm(today: String = todayDateString()): List<WeekDayStatus>? {
        val state = loadState() ?: return null
        val phases = state.phases.ifEmpty { rehabPhases() }
        val dow = dayOfWeekFromDate(today) // 1=Monday..7=Sunday
        val labels = listOf("一", "二", "三", "四", "五", "六", "日")
        val monday = daysAgoDateString(dow - 1)
        val completed = SportTaskRepository.completedDatesSync(monday, today)

        return (0 until 7).map { i ->
            // daysAgo 接受负数表示未来日期
            val date = daysAgoDateString(dow - 1 - i)
            val slot = PlanScheduler.resolve(phases, PlanScheduler.dayIndex(state.startDate, date))
            WeekDayStatus(
                label = labels[i],
                date = date,
                isToday = date == today,
                inPlan = slot != null,
                isTrainingDay = slot?.let { PlanScheduler.isTrainingDay(it) } ?: false,
                isCompleted = date in completed
            )
        }
    }

    /**
     * 把今日任务写入通知渠道（Android 读取后展示在提醒通知里）
     */
    suspend fun refreshReminder(today: String = todayDateString()) {
        val state = loadState()
        val task = todayTask(today)
        publishReminderPreview(
            text = PlanScheduler.reminderText(task),
            startDate = state?.startDate.orEmpty(),
            totalDays = state?.let { PlanScheduler.totalDays(it.phases.ifEmpty { rehabPhases() }) } ?: 0,
            generatedDate = today
        )
    }
}

/**
 * 各平台把提醒文案与计划元信息同步给系统通知。
 * Android 写 SharedPreferences；通知触发时若文案不是当天生成的，会依据 startDate 自行计算进度。
 * iOS 暂为空实现。
 */
expect fun publishReminderPreview(text: String, startDate: String, totalDays: Int, generatedDate: String)
