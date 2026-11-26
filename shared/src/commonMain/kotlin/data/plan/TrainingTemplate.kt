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
fun builtInTemplates(): List<TrainingTemplate> = listOf(rehabTemplate(), officeTemplate(), jointTemplate())

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

fun jointTemplate(): TrainingTemplate = TrainingTemplate(
    id = "joint-office",
    name = "久坐三关节恢复",
    description = "脚踝、膝盖、髋关节各成一段，坐姿站姿即可完成。扶桌椅、无跳跃，工位旁就能做。",
    groupPrefix = "关节",
    phases = jointPhases()
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
            PlanExercise("提踵加踝泵", "慢起 2 秒停 1 秒慢落 3 秒 12 次，再踝泵 20 次", 105, 15),
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
            PlanExercise("靠墙静蹲加提踵", "静蹲 45 秒后起身提踵 10 次，膝不过脚尖", 105, 20),
            PlanExercise("站姿髋铰链", "臀向后推背保持直，腘绳肌发紧停 1 秒，12 次", 60, 10),
            PlanExercise("扶桌单腿站立", "单腿 30 秒，进阶闭眼 10 秒或转头，骨盆水平", 60, 10),
            PlanExercise("站姿对侧抬", "扶桌，对侧手脚伸出停 2 秒，每侧 8 次", 60, 10),
            PlanExercise("扶桌侧向抬腿", "扶桌，腿外展 30 度停 1 秒，每侧 12 次", 60, 10),
            PlanExercise("坐姿胸椎旋转", "呼气转胸廓，眼睛跟肘走，每侧 10 次", 60, 10),
            PlanExercise("髋屈肌加小腿拉伸", "弓步收臀 20 秒加推墙拉小腿 20 秒，各一侧", 90, 10)
        )
    )
)

/**
 * 久坐三关节恢复：脚踝 / 膝盖 / 髋关节各一段，全程坐姿或扶桌椅站姿，无需器械。
 * 每段约 8 分钟，可按关节单独练，也可三段连做。全程无痛为界，脚踝或膝盖仍在恢复期先选坐姿版。
 */
fun jointPhases(): List<PlanPhase> = listOf(
    PlanPhase(
        index = 1,
        title = "脚踝段",
        days = 21,
        everyOtherDay = false,
        exercises = listOf(
            PlanExercise("坐姿踝泵", "坐稳勾脚停 1 秒、绷脚停 1 秒，30 次。消肿第一优先，久坐每小时都做", 60, 10),
            PlanExercise("空中画字母", "一脚悬空写 A-Z 或画 8 字，幅度到无痛极限，脚踝全范围活动", 60, 10),
            PlanExercise("踝四方向等长", "手或桌腿给阻力，背屈跖屈内翻外翻各 10 秒 × 2，三成力、关节不动", 90, 15),
            PlanExercise("踝外翻自我抗阻", "手掌根抵脚外侧，脚向外翻对抗 10 秒 × 4，腓骨肌别偷懒", 45, 10),
            PlanExercise("坐姿提踵", "坐椅前 1/2，双脚慢起 2 秒停 1 秒慢落，15 次，感受小腿泵血", 75, 10),
            PlanExercise("扶桌单腿站", "手指轻搭桌沿，单腿 20-30 秒换侧，踝不晃；恢复期改双脚站", 60, 15)
        )
    ),
    PlanPhase(
        index = 2,
        title = "膝盖段",
        days = 21,
        everyOtherDay = false,
        exercises = listOf(
            PlanExercise("坐姿伸膝", "坐稳缓慢伸直小腿停 2 秒再落，10 次/侧，膝盖对准脚尖，不弹震", 90, 10),
            PlanExercise("坐姿髌骨松动", "拇指轻推膝盖骨上下左右各 10 次，无痛范围，改善髌骨活动度", 45, 10),
            PlanExercise("扶桌坐站转换", "不借手从椅上站起再缓慢坐下，10 次，膝对脚尖不内扣", 60, 15),
            PlanExercise("靠墙静蹲", "背贴墙下滑膝屈不超过 60°，膝不过脚尖，30 秒；前侧痛就蹲浅", 60, 20),
            PlanExercise("坐姿股四头肌收紧", "腿伸直收紧大腿前侧，膝盖下压停 5 秒放松，10 次/侧", 120, 10),
            PlanExercise("坐位腿后侧拉伸", "腿伸直脚尖回勾，从髋部往前倾，每侧 30 秒，背不弓", 60, 10)
        )
    ),
    PlanPhase(
        index = 3,
        title = "髋关节段",
        days = 21,
        everyOtherDay = false,
        exercises = listOf(
            PlanExercise("坐姿髋外展", "坐椅上双膝用力分开，手向内抵挡对抗 5 秒，10 次，臀侧发力", 60, 10),
            PlanExercise("坐姿夹臀", "坐椅前 1/2，夹臀 5 秒松 2 秒，10 次，腰不动", 75, 10),
            PlanExercise("坐姿二郎腿拉伸", "一脚踝搭对侧膝，双手抱大腿轻拉向胸，每侧 30 秒，不驼背", 60, 10),
            PlanExercise("弓步髋屈肌拉伸", "前后脚弓步，先收臀送髋再微屈前膝，每侧 30 秒，不塌腰", 60, 10),
            PlanExercise("站姿髋铰链", "扶桌臀向后推、背挺直，腘绳肌发紧停 1 秒，12 次，不弯腰", 60, 10),
            PlanExercise("扶桌侧向抬腿", "扶桌腿向外侧抬 30°停 1 秒，每侧 12 次，脚尖朝前不外旋", 60, 10)
        )
    )
)
