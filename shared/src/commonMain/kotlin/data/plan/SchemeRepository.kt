package data.plan

import data.GroupItem
import data.SportTaskRepository
import data.remote.PlatformStorageProvider
import kotlinx.serialization.json.Json
import pages.dayOfWeekFromDate
import pages.daysAgoDateString
import pages.todayDateString

/**
 * 方案仓库：管理「方案实例 + 当前方案」，存 PlatformStorage，不动 SQLite 表结构。
 *
 * 兼容性：老用户没有 scheme 状态，但可能已有康复计划的分组。
 * load() 首次读取时若发现 PlanRepository 里有已激活计划且没有方案状态，
 * 会自动把该计划转成一个 Scheme 并设为当前，保证升级后首页仍能正确归类。
 */
object SchemeRepository {

    private const val KEY_STATE = "scheme_state_v1"
    private const val SCHEME_REHAB_ID = "scheme-rehab-plan"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private var cached: SchemeState? = null

    fun load(): SchemeState {
        cached?.let { return it }
        val restored = runCatching {
            PlatformStorageProvider.get().load(KEY_STATE)
                ?.let { json.decodeFromString<SchemeState>(it) }
        }.getOrNull()

        val state = restored ?: migrateLegacyPlan()
        cached = state
        return state
    }

    /** 把旧的 8 周康复计划转成方案（仅在首次无方案状态时执行） */
    private fun migrateLegacyPlan(): SchemeState {
        val plan = runCatching { PlanRepository.loadState() }.getOrNull() ?: return SchemeState()
        if (plan.groupIds.isEmpty()) return SchemeState()
        val scheme = Scheme(
            id = SCHEME_REHAB_ID,
            name = plan.name.ifBlank { "8 周康复计划" },
            groupPrefix = "康复",
            startDate = plan.startDate,
            groupIds = plan.groupIds.values.toList(),
            source = "rehab",
            phases = plan.phases
        )
        val state = SchemeState(listOf(scheme), currentId = scheme.id)
        runCatching { save(state) }
        return state
    }

    private fun save(state: SchemeState) {
        val text = json.encodeToString(SchemeState.serializer(), state)
        PlatformStorageProvider.get().save(KEY_STATE, text)
        cached = state
    }

    fun all(): List<Scheme> = load().schemes

    fun current(): Scheme? {
        val state = load()
        return state.schemes.firstOrNull { it.id == state.currentId }
    }

    /** 当前方案的分组；currentId=null（全部分组）或方案已失效时返回全部分组 */
    fun currentGroups(allGroups: List<GroupItem> = SportTaskRepository.loadGroupItemsSync()): List<GroupItem> {
        val scheme = current() ?: return allGroups
        val matched = allGroups.filter { it.id in scheme.groupIds }
        // 分组可能被用户删掉：若一个都不剩，退回全部，避免首页空白
        return matched.ifEmpty { allGroups }
    }

    /** 固定 id：同一来源模板重复导入时复用同一方案槽位，天然去重 */
    fun idForSource(source: String): String = "scheme-$source"

    /**
     * 登记（或替换）方案并设为当前。
     * 同一 source 只保留一份——重复导入时覆盖旧方案，返回被替换方案的旧 groupIds 供调用方清理 DB。
     */
    data class RegisterResult(val scheme: Scheme, val replacedGroupIds: List<Long>)

    fun register(
        templateName: String,
        groupPrefix: String,
        startDate: String,
        groupIds: List<Long>,
        source: String,
        phases: List<PlanPhase> = emptyList()
    ): RegisterResult {
        val id = idForSource(source)
        val scheme = Scheme(id, templateName, groupPrefix, startDate, groupIds, source, phases)
        val state = load()
        val replaced = state.schemes.firstOrNull { it.id == id }
        val updated = state.schemes.filterNot { it.id == id } + scheme
        save(state.copy(schemes = updated, currentId = id))
        return RegisterResult(scheme, replaced?.groupIds ?: emptyList())
    }

    /** 手动新建空方案并设为当前 */
    fun create(name: String, startDate: String): Scheme {
        val id = "scheme-custom-${startDate}-${(0..999999).random()}"
        val scheme = Scheme(id, name, "", startDate, emptyList(), "custom")
        val state = load()
        save(state.copy(schemes = state.schemes + scheme, currentId = id))
        return scheme
    }

    /** 把新分组归属到方案（方案内新建分组时调用） */
    fun addGroupToScheme(schemeId: String, groupId: Long) {
        val state = load()
        val updated = state.schemes.map { scheme ->
            if (scheme.id == schemeId) scheme.copy(groupIds = scheme.groupIds + groupId) else scheme
        }
        save(state.copy(schemes = updated))
    }

    /** 从方案移除分组归属（删除分组时调用） */
    fun removeGroupFromScheme(groupId: Long) {
        val state = load()
        val updated = state.schemes.map { scheme ->
            if (groupId in scheme.groupIds) scheme.copy(groupIds = scheme.groupIds - groupId) else scheme
        }
        save(state.copy(schemes = updated))
    }

    /** 取某方案包含的分组 id（删除方案时供调用方级联删 DB） */
    fun groupIdsOf(schemeId: String): List<Long> =
        load().schemes.firstOrNull { it.id == schemeId }?.groupIds ?: emptyList()

    fun findBySource(source: String): Scheme? = load().schemes.firstOrNull { it.source == source }

    /** 旧康复计划导入后同步登记（PlanRepository 调用） */
    fun registerRehab(name: String, startDate: String, groupIds: List<Long>, phases: List<PlanPhase> = rehabPhases()): RegisterResult =
        register(name, "康复", startDate, groupIds, "rehab", phases)

    fun switchTo(schemeId: String?) {
        val state = load()
        save(state.copy(currentId = schemeId))
    }

    /** 方案被删除时清理；删光后回到「全部分组」 */
    fun remove(schemeId: String) {
        val state = load()
        val updated = state.schemes.filterNot { it.id == schemeId }
        val nextCurrent = if (state.currentId == schemeId) null else state.currentId
        save(state.copy(schemes = updated, currentId = nextCurrent))
    }

    /** 按来源清理（康复计划 id 是动态的，删除时按 source 统一移除） */
    fun removeBySource(source: String) {
        val state = load()
        val removedIds = state.schemes.filter { it.source == source }.map { it.id }.toSet()
        val updated = state.schemes.filterNot { it.source == source }
        val nextCurrent = if (state.currentId in removedIds) null else state.currentId
        save(state.copy(schemes = updated, currentId = nextCurrent))
    }

    /**
     * 按【当前方案】计算今日任务，供首页卡片使用。
     * 数据源完全来自方案自身（名称/起始日/阶段/分组），因此今日卡片永远与下方分组属于同一方案。
     * 没有当前方案（全部分组视图）或方案无调度数据时返回 null。
     */
    fun todayTask(today: String = todayDateString()): TodayTask? {
        val scheme = current() ?: return null
        val phases = scheme.phases
        if (phases.isEmpty() || scheme.groupIds.isEmpty()) return null

        val day = PlanScheduler.dayIndex(scheme.startDate, today)
        val total = PlanScheduler.totalDays(phases)

        fun buildTask(
            phaseTitle: String, phaseIndex: Int, dayInPhase: Int, phaseDays: Int,
            isTrainingDay: Boolean, groupId: Long?, actionCount: Int, estimatedMinutes: Int,
            completed: Boolean, finished: Boolean = false, notStarted: Boolean = false
        ) = TodayTask(
            planName = scheme.name,
            startDate = scheme.startDate,
            phaseTitle = phaseTitle,
            phaseIndex = phaseIndex,
            dayIndex = day,
            dayInPhase = dayInPhase,
            phaseDays = phaseDays,
            isTrainingDay = isTrainingDay,
            groupId = groupId,
            actionCount = actionCount,
            estimatedMinutes = estimatedMinutes,
            completed = completed,
            finished = finished,
            notStarted = notStarted
        )

        if (day < 0) {
            return buildTask("计划未开始", 0, 0, 0, false, null, 0, 0, false, notStarted = true)
        }
        val slot = PlanScheduler.resolve(phases, day)
        if (slot == null || day >= total) {
            return buildTask("计划已完成", -1, 0, 0, false, null, 0, 0, false, finished = true)
        }

        // phase.index 从 1 开始，groupIds 按阶段顺序存放
        val groupId = scheme.groupIds.getOrNull(slot.phase.index - 1)
        val actionCount = groupId?.let { SportTaskRepository.countActionsSync(it) } ?: 0
        val completed = groupId != null && SportTaskRepository.hasCompletedSync(groupId, today)
        return buildTask(
            phaseTitle = slot.phase.title,
            phaseIndex = slot.phase.index,
            dayInPhase = slot.dayInPhase,
            phaseDays = slot.phase.days,
            isTrainingDay = PlanScheduler.isTrainingDay(slot),
            groupId = groupId,
            actionCount = actionCount,
            estimatedMinutes = PlanScheduler.estimatedMinutes(slot.phase),
            completed = completed
        )
    }

    /** 按【当前方案】计算本周节奏；无当前方案或无调度数据时返回 null */
    fun weekRhythm(today: String = todayDateString()): List<WeekDayStatus>? {
        val scheme = current() ?: return null
        val phases = scheme.phases
        if (phases.isEmpty()) return null

        val dow = dayOfWeekFromDate(today) // 1=Monday..7=Sunday
        val labels = listOf("一", "二", "三", "四", "五", "六", "日")
        val monday = daysAgoDateString(dow - 1)
        val completed = SportTaskRepository.completedDatesSync(monday, today)

        return (0 until 7).map { i ->
            val date = daysAgoDateString(dow - 1 - i)
            val slot = PlanScheduler.resolve(phases, PlanScheduler.dayIndex(scheme.startDate, date))
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

    /** 测试清缓存用 */
    internal fun resetCache() { cached = null }
}
