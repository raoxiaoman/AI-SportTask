package data.plan

import data.GroupItem
import data.SportTaskRepository
import data.remote.PlatformStorageProvider
import kotlinx.serialization.json.Json

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
            source = "rehab"
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

    /** 导入方案后登记并设为当前；schemeId 用模板 id + 首个分组 id 保证多次导入不冲突 */
    fun register(templateName: String, groupPrefix: String, startDate: String, groupIds: List<Long>, source: String): Scheme {
        val id = "scheme-$source-${groupIds.firstOrNull() ?: 0}"
        val scheme = Scheme(id, templateName, groupPrefix, startDate, groupIds, source)
        val state = load()
        val updated = state.schemes.filterNot { it.id == id } + scheme
        save(state.copy(schemes = updated, currentId = id))
        return scheme
    }

    /** 旧康复计划导入后同步登记（PlanRepository 调用） */
    fun registerRehab(name: String, startDate: String, groupIds: List<Long>) =
        register(name, "康复", startDate, groupIds, "rehab")

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

    /** 测试清缓存用 */
    internal fun resetCache() { cached = null }
}
