package ui

/**
 * 动作详细指导库 —— 「动作要点」的数据源。
 *
 * 结构：按动作名精确匹配；计划内置动作全部覆盖。
 * 用户自建动作没有匹配时回退到 [guideFor] 的通用指导（自动把要点文案拆成步骤）。
 *
 * videoUrl 为视频演示直链的预留字段：非空时优先打开该链接；为空时
 * 界面用 [videoSearchUrl] 生成对应动作的视频搜索入口，保证每个动作都有视频例子。
 */
data class ExerciseGuide(
    val name: String,
    /** 一句话概括（执行页要点卡顶部展示） */
    val summary: String,
    /** 分步骤操作说明 */
    val steps: List<String>,
    /** 质量要点（checklist 风格） */
    val tips: List<String>,
    /** 疼痛信号 / 禁忌 */
    val cautions: List<String>,
    /** 示意图姿势 */
    val pose: PoseSpec,
    /** 视频演示链接（预留） */
    val videoUrl: String? = null
)

private fun pt(x: Float, y: Float) = Pt(x, y)

/** 通用垫子（仰卧/侧卧/俯卧类动作） */
private val mat = PoseBox(6f, 84f, 88f, 5f)

private val guides: Map<String, ExerciseGuide> = mapOf(

    // ==================== 阶段 1 激活 ====================

    "踝泵" to ExerciseGuide(
        name = "踝泵",
        summary = "勾脚—绷脚，像踩缝纫机一样缓慢交替",
        steps = listOf(
            "坐位或仰卧，双腿伸直放松，脚踝离开床面/地面",
            "脚尖用力向自己勾起，到最大角度后保持 2 秒",
            "再用力向下绷直，保持 2 秒",
            "缓慢连贯地交替，节奏约 3 秒一次"
        ),
        tips = listOf(
            "全程无痛，速度越慢效果越好",
            "幅度比次数重要：勾、绷都要做到极限角度",
            "小腿肚应有轻微牵拉或酸胀感"
        ),
        cautions = listOf(
            "出现刺痛、关节内卡顿感立即停止并记录",
            "肿胀明显加重时减少次数"
        ),
        pose = PoseSpec(
            head = pt(12f, 75f), neck = pt(18f, 78f), hip = pt(42f, 80f),
            armNear = listOf(pt(28f, 85f), pt(34f, 88f)),
            armFar = listOf(pt(26f, 84f), pt(32f, 88f)),
            legNear = listOf(pt(68f, 81f), pt(87f, 77f)),
            legFar = listOf(pt(66f, 80f), pt(86f, 80f)),
            arrows = listOf(
                pt(91f, 64f) to pt(91f, 73f),
                pt(91f, 86f) to pt(91f, 77f)
            ),
            boxes = listOf(mat)
        )
    ),

    "踝四方向等长" to ExerciseGuide(
        name = "踝四方向等长",
        summary = "手或带子给对抗，脚踝用力顶但关节不动",
        steps = listOf(
            "坐位，毛巾/弹力带套在前脚掌，手抓住两端",
            "脚用力向上勾、向下绷、向外翻、向内翻，每个方向与对抗力较劲",
            "只用力、关节不移动（等长收缩），每方向保持 10 秒",
            "统一用三成力；内翻方向力度减半"
        ),
        tips = listOf(
            "关键在「顶住不动」：脚踝位置始终不变",
            "表情不费力、呼吸不憋气，说明力度合适",
            "重点是外侧（外翻方向），内侧保守"
        ),
        cautions = listOf(
            "关节出现位移感或疼痛即力度过大，立即减轻",
            "内翻方向有任何不适直接跳过"
        ),
        pose = PoseSpec(
            head = pt(38f, 39f), neck = pt(38f, 48f), hip = pt(38f, 74f),
            armNear = listOf(pt(54f, 64f), pt(72f, 62f)),
            armFar = listOf(pt(56f, 68f), pt(74f, 66f)),
            legNear = listOf(pt(62f, 76f), pt(80f, 72f)),
            legFar = listOf(pt(60f, 78f), pt(78f, 76f)),
            arrows = listOf(
                pt(80f, 64f) to pt(80f, 58f),
                pt(80f, 80f) to pt(80f, 86f),
                pt(86f, 72f) to pt(92f, 72f),
                pt(74f, 72f) to pt(68f, 72f)
            ),
            props = listOf(PoseLine(pt(74f, 64f), pt(84f, 74f)))
        )
    ),

    "弹力带踝外翻" to ExerciseGuide(
        name = "弹力带踝外翻",
        summary = "小阻力把脚向外翻，慢慢放回",
        steps = listOf(
            "坐位，弹力带套在前脚掌并向外侧固定",
            "脚踝用力向外翻（小趾侧抬起），对抗带子阻力",
            "到最大角度停 1 秒，再用 3-4 秒缓慢放回",
            "全程膝盖和腿保持不动，只动脚踝"
        ),
        tips = listOf(
            "阻力宁小勿大，目标是控制不是力量",
            "回放阶段慢放比拉起更重要",
            "躯干不要跟着歪，骨盆保持中立"
        ),
        cautions = listOf(
            "外踝出现锐痛立即停止",
            "次日酸痛超过 24 小时说明阻力偏大"
        ),
        pose = PoseSpec(
            head = pt(36f, 39f), neck = pt(36f, 48f), hip = pt(36f, 74f),
            armNear = listOf(pt(52f, 64f), pt(68f, 60f)),
            armFar = listOf(pt(54f, 68f), pt(70f, 64f)),
            legNear = listOf(pt(60f, 76f), pt(76f, 70f)),
            legFar = listOf(pt(58f, 78f), pt(74f, 74f)),
            arrows = listOf(pt(80f, 66f) to pt(88f, 60f)),
            props = listOf(PoseLine(pt(78f, 68f), pt(92f, 62f)))
        )
    ),

    "小腿后侧拉伸" to ExerciseGuide(
        name = "小腿后侧拉伸",
        summary = "弓步推墙，后腿膝盖伸直、脚跟踩实",
        steps = listOf(
            "面向墙站立，双手扶墙，弓步姿势",
            "后腿膝盖完全伸直，后脚脚跟用力踩住地面",
            "前腿慢慢屈膝，身体向墙前倾，直到小腿后侧有明显紧绷感",
            "保持 30 秒，自然呼吸，然后换边"
        ),
        tips = listOf(
            "拉伸感在「紧」而不在「痛」，强度约 6/10",
            "后脚脚尖朝正前方，不要外八",
            "脚跟全程不离地"
        ),
        cautions = listOf(
            "跟腱处出现尖锐疼痛立即放松",
            "拉伸中感到小腿抽筋，说明幅度过快，放缓"
        ),
        pose = PoseSpec(
            head = pt(42f, 36f), neck = pt(46f, 44f), hip = pt(58f, 62f),
            armNear = listOf(pt(34f, 50f), pt(16f, 46f)),
            armFar = listOf(pt(36f, 56f), pt(16f, 56f)),
            legNear = listOf(pt(46f, 74f), pt(42f, 90f)),
            legFar = listOf(pt(78f, 75f), pt(88f, 90f)),
            arrows = listOf(pt(88f, 78f) to pt(88f, 87f)),
            props = listOf(PoseLine(pt(12f, 16f), pt(12f, 91f)))
        )
    ),

    "直腿抬高" to ExerciseGuide(
        name = "直腿抬高",
        summary = "仰卧勾脚直腿抬起 30-45°，慢起慢落",
        steps = listOf(
            "仰卧，一条腿弯曲踩地保护腰部，另一条腿完全伸直",
            "脚尖勾向自己，膝盖始终不弯",
            "用大腿前侧发力把腿抬到约 30-45°（一条腿的长度）",
            "顶端停 2 秒，再用 3-4 秒缓慢放下，但不要完全落地"
        ),
        tips = listOf(
            "抬腿时腰不要拱起、不憋气",
            "发力感应在大腿前侧，而不是髋部弹动",
            "落腿比抬腿更要慢"
        ),
        cautions = listOf(
            "腰部出现疼痛立即屈膝、停止",
            "膝盖窝牵扯痛说明幅度过高，降低抬起角度"
        ),
        pose = PoseSpec(
            head = pt(14f, 75f), neck = pt(20f, 78f), hip = pt(46f, 80f),
            armNear = listOf(pt(30f, 85f), pt(36f, 88f)),
            armFar = listOf(pt(28f, 84f), pt(34f, 88f)),
            legNear = listOf(pt(66f, 72f), pt(82f, 60f)),
            legFar = listOf(pt(72f, 81f), pt(90f, 81f)),
            arrows = listOf(pt(74f, 70f) to pt(82f, 62f)),
            boxes = listOf(mat)
        )
    ),

    "臀桥" to ExerciseGuide(
        name = "臀桥",
        summary = "脚跟发力抬起臀部，肩-髋-膝一条线",
        steps = listOf(
            "仰卧屈膝，双脚踩地与髋同宽，脚跟靠近臀部",
            "呼气时用脚跟向下踩，把臀部抬离地面",
            "抬到肩、髋、膝呈一条直线，顶端收紧臀部停 2 秒",
            "用 3-4 秒缓慢下放，脊柱一节一节落回"
        ),
        tips = listOf(
            "发力感在臀部，大腿后侧辅助，腰不应酸",
            "顶端肋骨不要翻起来，保持核心收紧",
            "双脚不要使劲蹬地外撇，膝盖方向与脚尖一致"
        ),
        cautions = listOf(
            "腰部出现刺痛立即放下休息",
            "抽筋（多为腘绳肌）时伸直腿缓慢拉伸缓解"
        ),
        pose = PoseSpec(
            head = pt(16f, 74f), neck = pt(24f, 82f), hip = pt(46f, 70f),
            armNear = listOf(pt(34f, 85f), pt(40f, 88f)),
            armFar = listOf(pt(32f, 84f), pt(38f, 88f)),
            legNear = listOf(pt(66f, 76f), pt(74f, 88f)),
            legFar = listOf(pt(64f, 76f), pt(72f, 88f)),
            arrows = listOf(pt(46f, 62f) to pt(46f, 54f)),
            boxes = listOf(mat)
        )
    ),

    "麦肯基俯卧撑起" to ExerciseGuide(
        name = "麦肯基俯卧撑起",
        summary = "俯卧用手撑起上身，髋不离地",
        steps = listOf(
            "俯卧，双手放在肩膀两侧，像做俯卧撑的准备",
            "手臂缓慢用力把上身撑起来，骨盆和髋始终贴地",
            "撑到无痛的最大幅度，停留 2-3 秒并自然呼气",
            "缓慢放下，重复 8 次"
        ),
        tips = listOf(
            "下背应有轻松的伸展感，而不是疼痛",
            "撑起时呼气、放松肩颈，不要耸肩",
            "幅度循序渐进：先手肘支撑，再完全撑直"
        ),
        cautions = listOf(
            "腿部出现放射痛/麻木立即停止",
            "撑起后疼痛加重而非减轻，说明该动作暂不适合"
        ),
        pose = PoseSpec(
            head = pt(68f, 56f), neck = pt(62f, 64f), hip = pt(40f, 82f),
            armNear = listOf(pt(62f, 74f), pt(60f, 87f)),
            armFar = listOf(pt(66f, 76f), pt(64f, 87f)),
            legNear = listOf(pt(26f, 82f), pt(14f, 84f)),
            legFar = listOf(pt(24f, 83f), pt(12f, 85f)),
            arrows = listOf(pt(76f, 62f) to pt(80f, 54f)),
            boxes = listOf(mat)
        )
    ),

    "死虫式" to ExerciseGuide(
        name = "死虫式",
        summary = "仰卧腰贴地，对侧手脚缓慢伸展",
        steps = listOf(
            "仰卧，手臂伸直指向天花板，双腿抬起屈膝 90°",
            "先确保腰部完全贴地（腰与地面之间无缝隙）",
            "呼气时左臂向头顶方向伸展，同时右腿缓慢向前向下伸直",
            "伸展到腰即将离地的极限，停 1 秒，收回换对侧"
        ),
        tips = listOf(
            "核心始终压住腰，伸展越慢越有效",
            "动作中呼吸不能停",
            "幅度以「腰贴得住」为准，宁可小不可拱腰"
        ),
        cautions = listOf(
            "腰部拱起出现酸痛，立即减小伸展幅度",
            "颈肩紧张时在头下垫小毛巾"
        ),
        pose = PoseSpec(
            head = pt(20f, 74f), neck = pt(26f, 77f), hip = pt(52f, 79f),
            armNear = listOf(pt(16f, 72f), pt(8f, 68f)),
            armFar = listOf(pt(28f, 68f), pt(28f, 56f)),
            legNear = listOf(pt(72f, 68f), pt(76f, 54f)),
            legFar = listOf(pt(76f, 80f), pt(92f, 80f)),
            arrows = listOf(
                pt(28f, 50f) to pt(28f, 42f),
                pt(84f, 76f) to pt(90f, 76f)
            ),
            boxes = listOf(mat)
        )
    ),

    "收下巴" to ExerciseGuide(
        name = "收下巴",
        summary = "下巴水平向后收，做出「双下巴」",
        steps = listOf(
            "坐直或站直，视线平视前方",
            "下巴保持水平，整个头部向后平移，像挤出双下巴",
            "保持 10 秒，感受颈后深层发力",
            "缓慢放松，重复 8-10 次"
        ),
        tips = listOf(
            "是「平移」不是「低头」，鼻尖始终水平向前",
            "肩部放松，不要耸肩后仰",
            "久坐时每小时做一组"
        ),
        cautions = listOf(
            "出现头晕立即停止",
            "颈后尖锐疼痛说明用力过猛"
        ),
        pose = PoseSpec(
            head = pt(50f, 32f), neck = pt(47f, 42f), hip = pt(48f, 62f),
            armNear = listOf(pt(53f, 50f), pt(56f, 61f)),
            armFar = listOf(pt(43f, 50f), pt(40f, 61f)),
            legNear = listOf(pt(48f, 76f), pt(48f, 89f)),
            legFar = listOf(pt(45f, 76f), pt(45f, 89f)),
            arrows = listOf(pt(62f, 32f) to pt(54f, 32f))
        )
    ),

    // ==================== 阶段 2 稳定 ====================

    "单腿站立" to ExerciseGuide(
        name = "单腿站立",
        summary = "扶墙单腿站 20-30 秒，踝不晃",
        steps = listOf(
            "侧身靠近墙面，单手轻扶墙作保护",
            "重心移到一条腿，另一条腿屈膝抬起、悬空",
            "睁眼保持 20-30 秒，支撑腿的脚踝尽量不左右摇晃",
            "换腿，各 3 组"
        ),
        tips = listOf(
            "扶墙只是保命，手指轻搭不要发力",
            "脚趾「抓地」能显著提高稳定",
            "身体小幅晃动是正常的，追求的是越来越稳"
        ),
        cautions = listOf(
            "踝关节出现疼痛性松动感立即停止",
            "不要在光滑/斜面上练习"
        ),
        pose = PoseSpec(
            head = pt(52f, 30f), neck = pt(54f, 38f), hip = pt(58f, 58f),
            armNear = listOf(pt(68f, 44f), pt(86f, 42f)),
            armFar = listOf(pt(48f, 48f), pt(46f, 60f)),
            legNear = listOf(pt(72f, 68f), pt(68f, 82f)),
            legFar = listOf(pt(56f, 74f), pt(56f, 89f)),
            props = listOf(PoseLine(pt(90f, 16f), pt(90f, 91f)))
        )
    ),

    "弹力带踝外翻+背屈" to ExerciseGuide(
        name = "弹力带踝外翻+背屈",
        summary = "带子斜向外下方，边外翻边向上勾",
        steps = listOf(
            "坐位，弹力带绕前脚掌，固定在斜向外下方的位置",
            "脚踝同时完成两个动作：向外翻 + 向自己勾起",
            "到极限停 1 秒，3 秒缓慢放回",
            "控制轨迹不晃，各 12 次 × 2 组"
        ),
        tips = listOf(
            "两个方向合成一条斜线轨迹，不要拆成两次动作",
            "阻力保持小，重点是轨迹干净",
            "膝盖不动，小腿不要跟着旋转"
        ),
        cautions = listOf(
            "外踝或胫骨前侧锐痛立即停止",
            "动作变形（用腿部代偿）时先减小阻力"
        ),
        pose = PoseSpec(
            head = pt(36f, 39f), neck = pt(36f, 48f), hip = pt(36f, 74f),
            armNear = listOf(pt(52f, 64f), pt(68f, 60f)),
            armFar = listOf(pt(54f, 68f), pt(70f, 64f)),
            legNear = listOf(pt(60f, 76f), pt(76f, 68f)),
            legFar = listOf(pt(58f, 78f), pt(74f, 74f)),
            arrows = listOf(
                pt(80f, 60f) to pt(86f, 55f),
                pt(74f, 58f) to pt(72f, 50f)
            ),
            props = listOf(PoseLine(pt(78f, 66f), pt(92f, 60f)))
        )
    ),

    "提踵" to ExerciseGuide(
        name = "提踵",
        summary = "慢起慢落踮脚尖，双脚进阶到单脚",
        steps = listOf(
            "站立，单手扶墙/椅背，双脚与髋同宽",
            "用 2 秒缓慢抬起脚跟到最高点，前脚掌支撑",
            "顶端停 1 秒，感受小腿收缩",
            "再用 3 秒缓慢下放，脚跟轻轻落地；双脚熟练后进阶单脚"
        ),
        tips = listOf(
            "全程膝伸直，重心不前后摇晃",
            "「慢落」是重建跟腱弹性的关键",
            "幅度做到自己极限，不必追求很高"
        ),
        cautions = listOf(
            "跟腱处刺痛或弹响伴痛立即停止",
            "下放时不要「咣当」落地"
        ),
        pose = PoseSpec(
            head = pt(50f, 30f), neck = pt(52f, 38f), hip = pt(56f, 58f),
            armNear = listOf(pt(66f, 44f), pt(84f, 42f)),
            armFar = listOf(pt(46f, 48f), pt(44f, 60f)),
            legNear = listOf(pt(60f, 72f), pt(66f, 84f)),
            legFar = listOf(pt(52f, 74f), pt(48f, 89f)),
            arrows = listOf(pt(56f, 78f) to pt(56f, 70f)),
            props = listOf(PoseLine(pt(88f, 16f), pt(88f, 91f)))
        )
    ),

    "蚌式开合" to ExerciseGuide(
        name = "蚌式开合",
        summary = "侧卧屈膝，上膝像贝壳一样打开",
        steps = listOf(
            "侧卧，头枕手臂，双腿屈膝叠在一起，脚跟并拢",
            "骨盆保持垂直不后倒，核心轻轻收紧",
            "上方的膝盖向上打开，像张开的贝壳，双脚始终不分离",
            "打开到无痛最大幅度停 1 秒，缓慢合拢，12 次 × 2"
        ),
        tips = listOf(
            "发力感在臀部外侧，不是大腿",
            "骨盆一倒说明幅度过大，减小角度",
            "脚跟全程贴住"
        ),
        cautions = listOf(
            "髋部出现弹响伴痛减小幅度",
            "腰部代偿（扭腰）说明速度太快"
        ),
        pose = PoseSpec(
            head = pt(14f, 74f), neck = pt(20f, 78f), hip = pt(44f, 80f),
            armNear = listOf(pt(26f, 82f), pt(30f, 88f)),
            armFar = listOf(pt(24f, 80f), pt(28f, 88f)),
            legNear = listOf(pt(66f, 58f), pt(79f, 85f)),
            legFar = listOf(pt(64f, 68f), pt(78f, 86f)),
            arrows = listOf(pt(66f, 50f) to pt(66f, 42f)),
            boxes = listOf(mat)
        )
    ),

    "靠墙静蹲" to ExerciseGuide(
        name = "靠墙静蹲",
        summary = "背贴墙下滑，膝屈不超过 60°",
        steps = listOf(
            "背靠墙站立，双脚向前迈出一大步，与髋同宽",
            "背部贴着墙缓慢下滑，直到大腿接近水平（膝屈约 45-60°）",
            "膝盖方向对准脚尖，不内扣",
            "保持 20-30 秒自然呼吸，然后站起来休息"
        ),
        tips = listOf(
            "膝盖不超过脚尖太多，小腿尽量垂直地面",
            "重量压在脚跟和脚中，不是前脚掌",
            "腰背全程贴墙，肩部放松"
        ),
        cautions = listOf(
            "膝盖前侧出现压迫痛立即站高一些",
            "角度宁浅勿深，随康复进度慢慢加深"
        ),
        pose = PoseSpec(
            head = pt(26f, 32f), neck = pt(28f, 40f), hip = pt(30f, 64f),
            armNear = listOf(pt(42f, 58f), pt(52f, 62f)),
            armFar = listOf(pt(44f, 62f), pt(54f, 66f)),
            legNear = listOf(pt(58f, 64f), pt(58f, 89f)),
            legFar = listOf(pt(54f, 64f), pt(54f, 89f)),
            arrows = listOf(pt(28f, 52f) to pt(28f, 60f)),
            props = listOf(PoseLine(pt(18f, 14f), pt(18f, 91f)))
        )
    ),

    "侧卧髋外展" to ExerciseGuide(
        name = "侧卧髋外展",
        summary = "侧卧直腿上抬，脚尖朝前不外旋",
        steps = listOf(
            "侧卧，下方手臂枕头，身体呈一条直线",
            "上方腿完全伸直，脚尖朝正前方（不要向外旋）",
            "整条腿缓慢向上抬起约 30-40°，感受臀部外侧发力",
            "停 1 秒，用 3 秒缓慢放下，不落地继续下一组"
        ),
        tips = listOf(
            "腿抬得不高没关系，关键是「脚尖朝前 + 髋不后倒」",
            "躯干不要向后翻转借力",
            "发力点始终在臀部外侧"
        ),
        cautions = listOf(
            "髋关节弹响伴痛或腰部酸痛减小幅度",
            "大腿前侧发酸说明用错肌肉，放慢速度"
        ),
        pose = PoseSpec(
            head = pt(14f, 74f), neck = pt(20f, 78f), hip = pt(46f, 80f),
            armNear = listOf(pt(26f, 82f), pt(30f, 88f)),
            armFar = listOf(pt(24f, 80f), pt(28f, 88f)),
            legNear = listOf(pt(66f, 70f), pt(82f, 56f)),
            legFar = listOf(pt(70f, 81f), pt(88f, 81f)),
            arrows = listOf(pt(72f, 74f) to pt(80f, 64f)),
            boxes = listOf(mat)
        )
    ),

    "鸟狗式" to ExerciseGuide(
        name = "鸟狗式",
        summary = "四点跪位，对侧手脚缓慢伸展",
        steps = listOf(
            "四点跪位：手在肩正下方，膝在髋正下方，腰背平直",
            "先收紧核心，想象腰背上放一杯水",
            "呼气时右臂向前伸直，同时左腿向后伸直，与躯干同高",
            "停 2 秒，缓慢收回，换对侧；8 次 × 2 组"
        ),
        tips = listOf(
            "腰背始终水平，既不塌腰也不拱背",
            "伸出去的是「长」不是「高」，重点是稳定",
            "支持侧的手臂不要锁死肘关节"
        ),
        cautions = listOf(
            "腰晃动或旋转说明幅度过大",
            "手腕疼痛可用拳头支撑"
        ),
        pose = PoseSpec(
            head = pt(30f, 50f), neck = pt(36f, 58f), hip = pt(62f, 72f),
            armNear = listOf(pt(26f, 58f), pt(16f, 58f)),
            armFar = listOf(pt(36f, 72f), pt(36f, 88f)),
            legNear = listOf(pt(58f, 88f), pt(66f, 89f)),
            legFar = listOf(pt(78f, 76f), pt(90f, 78f)),
            arrows = listOf(
                pt(16f, 52f) to pt(10f, 52f),
                pt(84f, 72f) to pt(90f, 72f)
            )
        )
    ),

    "俯卧 YTWL" to ExerciseGuide(
        name = "俯卧 YTWL",
        summary = "俯卧肩胛发力手臂上抬成 Y，不耸肩",
        steps = listOf(
            "俯卧，额头轻点地面，双臂向前上方伸出呈 Y 形",
            "拇指朝上，先让肩胛骨向脊柱方向收拢",
            "手臂整体缓慢抬离地面 5-10cm，停 2 秒",
            "缓慢放下，重复 8 次；之后可做 T / W / L 形变式"
        ),
        tips = listOf(
            "发力感在上背和肩胛之间，肩颈不应紧张",
            "抬得低没关系，不能靠耸肩凑高度",
            "脖子保持放松，像含着一颗鸡蛋"
        ),
        cautions = listOf(
            "肩峰处疼痛（夹挤感）降低抬起高度",
            "腰部不适可在腹部垫枕头"
        ),
        pose = PoseSpec(
            head = pt(68f, 72f), neck = pt(62f, 78f), hip = pt(44f, 82f),
            armNear = listOf(pt(76f, 66f), pt(84f, 56f)),
            armFar = listOf(pt(72f, 72f), pt(78f, 62f)),
            legNear = listOf(pt(28f, 82f), pt(14f, 84f)),
            legFar = listOf(pt(26f, 83f), pt(12f, 85f)),
            arrows = listOf(pt(86f, 50f) to pt(88f, 44f)),
            boxes = listOf(mat)
        )
    ),

    "门框胸肌拉伸" to ExerciseGuide(
        name = "门框胸肌拉伸",
        summary = "站在门框中，肘与肩同高向前压",
        steps = listOf(
            "站在门框中间，双肘与肩同高，前臂贴住门框两侧",
            "一脚在前呈小弓步，身体整体缓慢向前穿过门框",
            "胸前侧出现明显牵拉感后保持 30 秒",
            "自然呼吸，可微调手的高度拉不同束的胸肌"
        ),
        tips = listOf(
            "牵拉感在胸前方，不是肩关节里",
            "肋骨不要向前翻，收一点腹",
            "头保持在两臂中间，不要前探"
        ),
        cautions = listOf(
            "肩关节内出现疼痛立即减小前压幅度",
            "有肩袖损伤史者手位低于肩"
        ),
        pose = PoseSpec(
            head = pt(50f, 28f), neck = pt(50f, 38f), hip = pt(50f, 60f),
            armNear = listOf(pt(38f, 50f), pt(28f, 42f)),
            armFar = listOf(pt(62f, 50f), pt(72f, 42f)),
            legNear = listOf(pt(46f, 74f), pt(46f, 89f)),
            legFar = listOf(pt(54f, 74f), pt(54f, 89f)),
            arrows = listOf(pt(50f, 48f) to pt(50f, 40f)),
            props = listOf(
                PoseLine(pt(26f, 14f), pt(26f, 91f)),
                PoseLine(pt(74f, 14f), pt(74f, 91f))
            )
        )
    ),

    // ==================== 阶段 3 功能 ====================

    "浅台阶上下" to ExerciseGuide(
        name = "浅台阶上下",
        summary = "10-15cm 台阶，全脚踩实，膝对脚尖",
        steps = listOf(
            "面对 10-15cm 的稳固台阶站立",
            "全脚掌踩上台阶，重心垂直向上送（不是向前扑）",
            "膝关节方向始终对准脚尖，踩实后稍停",
            "用后腿控制，2-3 秒缓慢下台阶；10 次 × 2 组"
        ),
        tips = listOf(
            "上去靠前腿发力，下来也由前腿控制",
            "全程脚不出声，说明控制到位",
            "可单手扶扶手作保护"
        ),
        cautions = listOf(
            "膝盖前侧疼痛说明台阶过高或速度过快",
            "踝不稳时先降低台阶高度"
        ),
        pose = PoseSpec(
            head = pt(30f, 28f), neck = pt(32f, 36f), hip = pt(36f, 56f),
            armNear = listOf(pt(42f, 44f), pt(48f, 50f)),
            armFar = listOf(pt(26f, 44f), pt(22f, 54f)),
            legNear = listOf(pt(50f, 56f), pt(62f, 66f)),
            legFar = listOf(pt(38f, 72f), pt(34f, 88f)),
            arrows = listOf(pt(64f, 60f) to pt(70f, 54f)),
            boxes = listOf(PoseBox(52f, 68f, 36f, 22f))
        )
    ),

    "扶墙单腿硬拉" to ExerciseGuide(
        name = "扶墙单腿硬拉",
        summary = "屈髋向后抬腿，腰背平直不弯腰",
        steps = listOf(
            "单手扶墙站稳，重心放在支撑腿",
            "保持腰背平直，以髋为轴整个上半身向前倾",
            "同时另一条腿向后伸直抬起，头-背-腿接近一条线",
            "感受支撑腿臀部和腘绳肌发力，停 1 秒缓慢收回，8 次 × 2"
        ),
        tips = listOf(
            "是「髋铰链」不是弯腰：折叠发生在髋，脊柱不变",
            "两条髋同高，骨盆不旋转",
            "后腿抬得不高没关系，背平最重要"
        ),
        cautions = listOf(
            "下背出现弓背疼痛立即减小前倾幅度",
            "支撑腿膝盖不要超伸锁死"
        ),
        pose = PoseSpec(
            head = pt(32f, 46f), neck = pt(38f, 50f), hip = pt(54f, 62f),
            armNear = listOf(pt(60f, 56f), pt(86f, 58f)),
            armFar = listOf(pt(42f, 58f), pt(44f, 70f)),
            legNear = listOf(pt(56f, 76f), pt(56f, 89f)),
            legFar = listOf(pt(72f, 68f), pt(84f, 74f)),
            arrows = listOf(pt(46f, 56f) to pt(54f, 52f)),
            props = listOf(PoseLine(pt(90f, 16f), pt(90f, 91f)))
        )
    ),

    "坐姿伸膝" to ExerciseGuide(
        name = "坐姿伸膝",
        summary = "坐位把膝盖伸直，顶端停 2 秒",
        steps = listOf(
            "坐在稳固的椅子上，腰背挺直，双脚自然落地",
            "一条腿缓慢向上、向前伸直膝盖",
            "伸直到最大角度后勾脚尖，停 2 秒，感受大腿前侧收缩",
            "用 2-3 秒缓慢放回，12 次 × 2 组"
        ),
        tips = listOf(
            "大腿始终贴住椅面，只有小腿在动",
            "顶端「勾脚」能强化内侧股四头肌",
            "弹力带/轻负荷进阶时从小阻力开始"
        ),
        cautions = listOf(
            "膝内出现卡痛或弹响伴痛减小角度",
            "不要猛地甩腿到伸直"
        ),
        pose = PoseSpec(
            head = pt(38f, 28f), neck = pt(38f, 36f), hip = pt(38f, 58f),
            armNear = listOf(pt(48f, 46f), pt(54f, 56f)),
            armFar = listOf(pt(46f, 50f), pt(52f, 58f)),
            legNear = listOf(pt(60f, 58f), pt(82f, 56f)),
            legFar = listOf(pt(60f, 60f), pt(64f, 88f)),
            arrows = listOf(pt(72f, 50f) to pt(80f, 48f)),
            boxes = listOf(
                PoseBox(20f, 34f, 4f, 30f),
                PoseBox(24f, 62f, 22f, 4f),
                PoseBox(26f, 66f, 4f, 24f),
                PoseBox(42f, 66f, 4f, 24f)
            )
        )
    ),

    "平衡垫站立" to ExerciseGuide(
        name = "平衡垫站立",
        summary = "软垫上单腿站 30 秒，进阶加抬头/抛接球",
        steps = listOf(
            "平衡垫/软垫放在稳固地面上，旁边有可扶持物",
            "单脚踩在垫子中央，另一脚悬空",
            "手臂平举帮助平衡，睁眼保持 30 秒",
            "稳定后进阶：抬头看远方 → 闭眼 → 轻抛接球"
        ),
        tips = listOf(
            "踝关节不断微调是正常现象，这是本体感觉训练",
            "脚趾抓垫、膝盖微屈不锁死",
            "每次进步以秒数记录"
        ),
        cautions = listOf(
            "踝部出现明显打软立即停下扶稳",
            "旁边必须有扶持物，避免跌倒"
        ),
        pose = PoseSpec(
            head = pt(50f, 26f), neck = pt(50f, 36f), hip = pt(50f, 58f),
            armNear = listOf(pt(38f, 46f), pt(26f, 46f)),
            armFar = listOf(pt(62f, 46f), pt(74f, 46f)),
            legNear = listOf(pt(46f, 72f), pt(46f, 83f)),
            legFar = listOf(pt(58f, 66f), pt(64f, 76f)),
            boxes = listOf(PoseBox(36f, 83f, 28f, 7f))
        )
    ),

    "有氧快走" to ExerciseGuide(
        name = "有氧快走",
        summary = "RPE 12-13：能说话、微微喘的强度",
        steps = listOf(
            "穿缓震好的鞋，从 5 分钟慢走热身",
            "加快到「微喘但能完整说话」的速度，保持 15-20 分钟",
            "手臂自然前后摆动，步幅适中，脚跟过渡到脚尖",
            "结束后 5 分钟慢走放松"
        ),
        tips = listOf(
            "强度自测：能唱歌=太轻，说不出整句=太重",
            "摆臂配合步频，躯干正直不前倾",
            "可在跑步机或平地完成"
        ),
        cautions = listOf(
            "踝/膝出现渐进性疼痛立即降速或停止",
            "当天下肢训练后可把时长减半"
        ),
        pose = PoseSpec(
            head = pt(44f, 32f), neck = pt(46f, 40f), hip = pt(48f, 58f),
            armNear = listOf(pt(56f, 48f), pt(64f, 56f)),
            armFar = listOf(pt(36f, 48f), pt(28f, 56f)),
            legNear = listOf(pt(60f, 70f), pt(68f, 88f)),
            legFar = listOf(pt(40f, 72f), pt(30f, 88f)),
            arrows = listOf(pt(74f, 80f) to pt(84f, 80f))
        )
    )
)

/**
 * 按动作名取详细指导；未收录的动作返回 null，
 * 由调用方用 [buildFallbackGuide] 兜底。
 */
fun guideFor(name: String): ExerciseGuide? = guides[name]

/**
 * 生成该动作的视频示范搜索链接。
 *
 * 不内置具体视频 ID：单个视频可能下架或失效，而按动作名检索始终可用。
 * 关键词里含中文，这里用纯 Kotlin 实现 UTF-8 百分号编码，三端通用且不依赖平台 API。
 */
fun videoSearchUrl(actionName: String): String {
    val keyword = "$actionName 康复训练 动作示范"
    return "https://search.bilibili.com/all?keyword=${percentEncode(keyword)}"
}

private fun percentEncode(value: String): String = buildString {
    val hex = "0123456789ABCDEF"
    fun appendByte(byte: Int) {
        append('%')
        append(hex[byte shr 4])
        append(hex[byte and 0x0F])
    }
    for (char in value) {
        val code = char.code
        when {
            code in 0x30..0x39 || code in 0x41..0x5A || code in 0x61..0x7A ||
                char == '-' || char == '_' || char == '.' || char == '~' -> append(char)
            code < 0x80 -> appendByte(code)
            code < 0x800 -> {
                appendByte(0xC0 or (code shr 6))
                appendByte(0x80 or (code and 0x3F))
            }
            else -> {
                appendByte(0xE0 or (code shr 12))
                appendByte(0x80 or ((code shr 6) and 0x3F))
                appendByte(0x80 or (code and 0x3F))
            }
        }
    }
}

/** 未收录动作的通用指导：把要点文案按句拆成步骤 */
fun buildFallbackGuide(name: String, stepsText: String): ExerciseGuide {
    val sentences = stepsText.replace("；", "。").split("。").map { it.trim() }.filter { it.isNotBlank() }
    val steps = if (sentences.size >= 2) sentences else listOf(
        stepsText.ifBlank { "按自己的节奏缓慢完成" },
        "全程无痛范围内进行"
    )
    return ExerciseGuide(
        name = name,
        summary = sentences.firstOrNull() ?: "缓慢、有控制地完成",
        steps = steps,
        tips = listOf(
            "速度放慢，幅度渐进",
            "呼吸自然，不憋气"
        ),
        cautions = listOf(
            "出现刺痛、麻木或关节内卡顿感立即停止",
            "疼痛持续到次日时降低强度并记录"
        ),
        pose = genericPose
    )
}
