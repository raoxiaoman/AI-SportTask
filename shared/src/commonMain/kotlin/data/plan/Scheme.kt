package data.plan

import kotlinx.serialization.Serializable

/**
 * 方案实例：一套方案包含多个分组（阶段）。
 *
 * 不改动 action_group 表结构——通过 groupIds 记录归属，避免数据库迁移。
 * 既有「8 周康复计划」也作为一个 Scheme 纳入统一切换。
 */
@Serializable
data class Scheme(
    val id: String,
    val name: String,
    val groupPrefix: String,
    val startDate: String,
    /** 该方案包含的本地分组 id（阶段顺序） */
    val groupIds: List<Long> = emptyList(),
    /** 来源模板 id，"rehab" 表示内置 8 周康复计划 */
    val source: String = "",
    /** 阶段调度数据（天数/隔日/动作），用于今日卡片与本周节奏；旧数据为空时按来源回退 */
    val phases: List<PlanPhase> = emptyList()
) {
    val isRehab: Boolean get() = source == "rehab"
    /** 用户手动新建的空方案（非模板导入、非旧康复计划） */
    val isCustom: Boolean get() = source == "custom"
    val phaseCount: Int get() = groupIds.size
}

/** 方案列表与当前选中方案；currentId=null 表示「全部分组」视图 */
@Serializable
data class SchemeState(
    val schemes: List<Scheme> = emptyList(),
    val currentId: String? = null
)
