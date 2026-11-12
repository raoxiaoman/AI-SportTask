package data.plan

import kotlinx.serialization.Serializable

/**
 * 计划中的单个动作：时长单位秒，与 action.default_time / rest_time 一致
 */
@Serializable
data class PlanExercise(
    val name: String,
    val detail: String,
    val seconds: Int,
    val restSeconds: Int
)

/**
 * 计划阶段：days 为阶段总天数，everyOtherDay=true 表示隔日训练
 */
@Serializable
data class PlanPhase(
    val index: Int,
    val title: String,
    val days: Int,
    val everyOtherDay: Boolean,
    val exercises: List<PlanExercise>
) {
    val groupName: String get() = "康复 · $title"
}

/**
 * 已激活的计划状态：startDate 为起始日，groupIds 为阶段 -> 本地分组 id
 */
@Serializable
data class PlanState(
    val name: String = "8 周吃练计划",
    val startDate: String,
    val groupIds: Map<Int, Long> = emptyMap(),
    val phases: List<PlanPhase> = rehabPhases()
)

/**
 * 内置 8 周康复计划（来源：8周吃练执行表）
 * 阶段1：第 1-14 天，每天 15 分钟
 * 阶段2：第 15-28 天，隔日 20 分钟
 * 阶段3：第 29-56 天，隔日 30 分钟
 */
fun rehabPhases(): List<PlanPhase> = listOf(
    PlanPhase(
        index = 1,
        title = "阶段1 激活",
        days = 14,
        everyOtherDay = false,
        exercises = listOf(
            PlanExercise("踝泵", "勾脚-绷脚，慢速全程无痛。20 次 × 3 组", 60, 15),
            PlanExercise(
                "踝四方向等长",
                "背屈压脚背 / 跖屈托脚底 / 内翻压外侧 / 外翻压内侧。每方向 10 秒 × 3，三成力、关节不动；内翻力度减半，不适则跳过",
                120, 15
            ),
            PlanExercise("弹力带踝外翻", "小阻力，慢回放。12 次 × 2 组", 60, 15),
            PlanExercise("小腿后侧拉伸", "毛巾或推墙，拉到紧不拉痛。30 秒 × 3", 90, 10),
            PlanExercise("直腿抬高", "勾脚、膝伸直，慢起慢落。10 次 × 2 组", 60, 20),
            PlanExercise("臀桥", "臀部发力，腰椎不塌。10 次 × 2 组", 60, 20),
            PlanExercise("麦肯基俯卧撑起", "俯卧手撑起上身，髋不离地，无痛范围。8 次 × 2", 60, 20),
            PlanExercise("死虫式", "腰贴地，对侧手脚缓慢伸展。8 次 × 2 组", 60, 20),
            PlanExercise("收下巴", "水平向后收，不低头。10 秒 × 8", 60, 10)
        )
    ),
    PlanPhase(
        index = 2,
        title = "阶段2 稳定",
        days = 14,
        everyOtherDay = true,
        exercises = listOf(
            PlanExercise("单腿站立", "扶墙，20-30 秒 × 3，踝不晃", 90, 15),
            PlanExercise("弹力带踝外翻+背屈", "各 12 次 × 2 组，控制轨迹", 120, 20),
            PlanExercise("提踵", "双脚→单脚，12 次 × 3 组，慢起慢落", 60, 20),
            PlanExercise("蚌式开合", "骨盆不后倒，无痛小范围。12 次 × 2", 60, 20),
            PlanExercise("靠墙静蹲", "膝屈 ≤60°，膝盖对脚尖。20-30 秒 × 3", 90, 30),
            PlanExercise("侧卧髋外展", "脚尖朝前不外旋。12 次 × 2 组", 60, 20),
            PlanExercise("鸟狗式", "对侧手脚，腰背保持平。8 次 × 2 组", 60, 20),
            PlanExercise("俯卧 YTWL", "肩胛发力不耸肩。8 次 × 2 组", 60, 20),
            PlanExercise("门框胸肌拉伸", "肘与肩同高。30 秒 × 2", 60, 10)
        )
    ),
    PlanPhase(
        index = 3,
        title = "阶段3 功能",
        days = 28,
        everyOtherDay = true,
        exercises = listOf(
            PlanExercise("浅台阶上下", "10-15cm，全脚踩实，膝对脚尖。10 次 × 2", 90, 30),
            PlanExercise("扶墙单腿硬拉", "屈髋不弯腰，髋不过度内旋。8 次 × 2", 60, 30),
            PlanExercise("坐姿伸膝", "弹力带或轻负荷，12 次 × 2 组", 60, 30),
            PlanExercise("平衡垫站立", "30 秒 × 3，进阶加抬头/抛接球", 90, 30),
            PlanExercise("有氧快走", "RPE 12-13，能说话微喘。20 分钟", 1200, 0)
        )
    )
)
