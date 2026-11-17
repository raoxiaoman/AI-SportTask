package data.plan

import kotlinx.serialization.Serializable

/**
 * 可导入的训练方案模板。
 *
 * 与后端的 templates.json 一一对应：阶段沿用 PlanPhase 结构，
 * 因此推送到云端时可直接映射为「分组 + 动作」，无需新增表。
 */
@Serializable
data class TrainingTemplate(
    val id: String,
    val name: String,
    val description: String,
    /** 阶段生成分组时的前缀，如「康复」「办公室」 */
    val groupPrefix: String,
    val phases: List<PlanPhase>
) {
    val actionCount: Int get() = phases.sumOf { it.exercises.size }
    val totalDays: Int get() = phases.sumOf { it.days }
    /** 单次训练时长（各阶段平均，取整分钟） */
    val durationMinutes: Int get() = phases.map { phase ->
        phase.exercises.sumOf { it.seconds + it.restSeconds } / 60
    }.average().toInt().coerceAtLeast(1)

    fun groupName(phase: PlanPhase): String = "$groupPrefix · ${phase.title}"
}

/** GET /api/templates 响应 */
@Serializable
data class TemplateListResponse(
    val templates: List<TrainingTemplate> = emptyList(),
    val version: Int = 1
)

/** 客户端内置模板：服务端不可用时仍可离线导入 */
fun builtInTemplates(): List<TrainingTemplate> = listOf(rehabTemplate(), officeTemplate())

fun rehabTemplate(): TrainingTemplate = TrainingTemplate(
    id = "rehab-8w",
    name = "8 周康复计划",
    description = "踝、腰、髋分三阶段恢复，自动区分训练日与休息日。适合康复期循序渐进。",
    groupPrefix = "康复",
    phases = rehabPhases()
)

fun officeTemplate(): TrainingTemplate = TrainingTemplate(
    id = "office-8w",
    name = "8 周办公室训练",
    description = "无器械、静音、不出汗。颈胸髋踝全覆盖，工位即可完成。",
    groupPrefix = "办公室",
    phases = officePhases()
)

/**
 * 办公室训练方案：8 周共 56 天。
 * 来源 docs/办公室训练方案.md「十、可直接录进 App 的动作表」。
 */
fun officePhases(): List<PlanPhase> = listOf(
    PlanPhase(
        index = 1,
        title = "阶段1 唤醒",
        days = 14,
        everyOtherDay = false,
        exercises = listOf(
            PlanExercise("收下巴", "下巴水平后移做双下巴，停 2 秒回。10 次，头不低不仰", 45, 10),
            PlanExercise("椅背胸椎伸展", "椅背上沿顶肩胛下缘，呼气上背后仰停 2 秒。8 次，不塌腰", 60, 10),
            PlanExercise("墙角胸肌拉伸", "前臂贴墙肘略高于肩，同侧脚前移轻转，每侧 30 秒", 60, 10),
            PlanExercise("弓步髋屈肌拉伸", "先收臀送髋再屈前膝，每侧 30 秒，不塌腰翘臀", 60, 10),
            PlanExercise("提踵加踝泵", "慢起 2 秒停 1 秒慢落 3 秒 12 次，再踝泵 20 次", 60, 10),
            PlanExercise("坐姿夹臀", "坐椅前 1/2，夹臀 5 秒松 2 秒，8 次，腰不动", 60, 10),
            PlanExercise("靠墙天使", "后脑上背臀贴墙，W 沿墙上滑到 Y，8 次，不拱腰", 60, 15)
        )
    ),
    PlanPhase(
        index = 2,
        title = "阶段2 建立",
        days = 14,
        everyOtherDay = false,
        exercises = listOf(
            PlanExercise("收下巴", "下巴水平后移做双下巴，停 2 秒回。10 次，头不低不仰", 45, 10),
            PlanExercise("靠墙天使", "后脑上背臀贴墙，W 上滑到 Y 停 1 秒，10 次", 60, 10),
            PlanExercise("坐姿胸椎旋转", "双膝夹紧，呼气转胸廓骨盆不动，每侧 8 次", 60, 10),
            PlanExercise("弓步髋屈肌拉伸", "先收臀送髋再屈前膝，每侧 30 秒", 60, 10),
            PlanExercise("靠墙静蹲", "背贴墙下滑膝屈 60 度内，膝不过脚尖，45 秒", 60, 20),
            PlanExercise("提踵", "慢起慢落 12 次，不弹震，重心在前脚掌内侧", 60, 10),
            PlanExercise("扶桌单腿站立", "手指轻搭桌沿，单腿 30 秒换侧，骨盆不歪", 60, 10),
            PlanExercise("站姿对侧抬", "扶桌，对侧手脚同时伸出停 2 秒，每侧 8 次", 60, 10),
            PlanExercise("墙俯卧撑", "面对墙一臂距，屈肘胸靠墙再推回，12 次，不塌腰", 45, 10)
        )
    ),
    PlanPhase(
        index = 3,
        title = "阶段3 强化",
        days = 28,
        everyOtherDay = true,
        exercises = listOf(
            PlanExercise("靠墙天使", "W 上滑到 Y 顶端停 2 秒，10 次，肋骨不外翻", 60, 10),
            PlanExercise("墙俯卧撑", "脚后退半步，屈肘胸靠墙推回，15 次", 60, 10),
            PlanExercise("靠墙静蹲加提踵", "静蹲 45 秒后起身提踵 10 次，膝不过脚尖", 90, 20),
            PlanExercise("站姿髋铰链", "臀向后推背保持直，腘绳肌发紧停 1 秒，12 次", 60, 10),
            PlanExercise("扶桌单腿站立", "单腿 30 秒，进阶闭眼 10 秒或转头，骨盆水平", 60, 10),
            PlanExercise("站姿对侧抬", "扶桌，对侧手脚伸出停 2 秒，每侧 8 次", 60, 10),
            PlanExercise("扶桌侧向抬腿", "扶桌，腿外展 30 度停 1 秒，每侧 12 次", 60, 10),
            PlanExercise("坐姿胸椎旋转", "呼气转胸廓，眼睛跟肘走，每侧 10 次", 60, 10),
            PlanExercise("髋屈肌加小腿拉伸", "弓步收臀 20 秒加推墙拉小腿 20 秒，各一侧", 90, 10)
        )
    )
)
