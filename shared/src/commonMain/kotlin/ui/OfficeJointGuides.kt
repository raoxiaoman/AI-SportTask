package ui

/**
 * 办公室 / 久坐三关节方案的专属动作图解。
 *
 * 这两套方案的动作名带「坐姿 / 扶桌 / 站姿 / 靠墙」等场景限定词，
 * 与基础库 [guides] 里的通用康复动作名不同，早期会全部回退成通用站立小人，
 * 导致示意图与动作不符。这里为它们逐一补齐贴合动作的姿势与专属文案。
 *
 * 坐标系与 [ExerciseDiagram] 一致：100×100 归一化，y 向下，地面线 y≈91.5。
 */

// 本文件内的坐标辅助（ExerciseGuides.kt 的 pt 为私有，跨文件不可见）
private fun pt(x: Float, y: Float) = Pt(x, y)
private fun poseLine(x1: Float, y1: Float, x2: Float, y2: Float) =
    PoseLine(pt(x1, y1), pt(x2, y2))

// 场景道具
private val ojChair = PoseBox(20f, 62f, 40f, 4f)          // 椅面（坐姿髋部下方）
private val ojChairBack = poseLine(24f, 62f, 24f, 34f)    // 椅背
private val ojWall = poseLine(16f, 12f, 16f, 91f)         // 墙（左侧）
private val ojWallRight = poseLine(86f, 12f, 86f, 91f)    // 墙（右侧）
private val ojDesk = poseLine(70f, 52f, 94f, 52f)         // 桌面（扶手用）
private val ojMat = PoseBox(6f, 84f, 88f, 5f)             // 垫子

/** 坐姿通用躯干：头/颈/髋固定，供各坐姿动作复用基准点 */
private data class SeatedTorso(val head: Pt, val neck: Pt, val hip: Pt)

private val ojSeated = SeatedTorso(pt(46f, 30f), pt(47f, 39f), pt(48f, 60f))

val officeJointGuides: Map<String, ExerciseGuide> = mapOf(

    // ==================== 办公室 / 阶段1 唤醒 ====================

    "椅背胸椎伸展" to ExerciseGuide(
        name = "椅背胸椎伸展",
        summary = "椅背上沿顶住肩胛，呼气时上背后仰",
        steps = listOf(
            "坐椅前 1/2，将椅背上沿对准肩胛骨下缘",
            "双手抱头或扶椅面，呼气时胸椎向后伸展",
            "在肩胛处停顿 2 秒，感受胸前打开",
            "缓慢回到中立位，重复 8 次，腰不要塌"
        ),
        tips = listOf("伸展点在胸椎而非腰椎", "幅度小而稳，不追求大幅度后仰", "下巴微收，避免颈部过度后伸"),
        cautions = listOf("腰部出现挤压痛立即停止", "眩晕者减小后仰幅度"),
        pose = PoseSpec(
            head = pt(50f, 28f), neck = pt(52f, 37f), hip = pt(50f, 60f),
            armNear = listOf(pt(40f, 44f), pt(50f, 34f)),
            armFar = listOf(pt(62f, 46f), pt(66f, 38f)),
            legNear = listOf(pt(60f, 66f), pt(62f, 88f)),
            legFar = listOf(pt(52f, 66f), pt(50f, 88f)),
            arrows = listOf(pt(50f, 30f) to pt(62f, 24f)),
            boxes = listOf(ojChair),
            props = listOf(ojChairBack)
        )
    ),

    "墙角胸肌拉伸" to ExerciseGuide(
        name = "墙角胸肌拉伸",
        summary = "前臂贴墙，身体前移拉胸前方",
        steps = listOf(
            "面对墙角站立，双臂屈肘 90°，前臂贴两侧墙面",
            "肘略高于肩，同侧脚向前迈一小步",
            "身体缓慢向墙角前倾，胸前出现牵拉感",
            "保持 30 秒后换方向，自然呼吸"
        ),
        tips = listOf("牵拉感在胸前方而非肩关节", "收腹，肋骨不要外翻", "手的高度可微调以拉到不同胸肌束"),
        cautions = listOf("肩关节内疼痛立即减小幅度", "肩袖损伤者手位低于肩"),
        pose = PoseSpec(
            head = pt(50f, 30f), neck = pt(50f, 39f), hip = pt(52f, 62f),
            armNear = listOf(pt(62f, 42f), pt(70f, 34f)),
            armFar = listOf(pt(38f, 42f), pt(30f, 34f)),
            legNear = listOf(pt(58f, 76f), pt(62f, 89f)),
            legFar = listOf(pt(48f, 76f), pt(44f, 89f)),
            arrows = listOf(pt(50f, 50f) to pt(50f, 42f)),
            props = listOf(poseLine(24f, 14f, 24f, 91f), poseLine(76f, 14f, 76f, 91f))
        )
    ),

    "弓步髋屈肌拉伸" to ExerciseGuide(
        name = "弓步髋屈肌拉伸",
        summary = "前后弓步，收臀送髋拉大腿根前侧",
        steps = listOf(
            "前后脚分开成弓步，后脚脚背贴地",
            "先收紧臀部、把髋向前送",
            "再轻微屈前膝，感到后侧大腿根前部牵拉",
            "每侧保持 30 秒，不塌腰翘臀"
        ),
        tips = listOf("先收臀再屈膝，避免腰部代偿", "牵拉感在大腿根前侧", "上身保持直立"),
        cautions = listOf("膝关节内有疼痛立即停止", "前膝不要超过脚尖太多"),
        pose = PoseSpec(
            head = pt(50f, 30f), neck = pt(50f, 39f), hip = pt(50f, 60f),
            armNear = listOf(pt(58f, 68f), pt(58f, 82f)),
            armFar = listOf(pt(42f, 68f), pt(42f, 82f)),
            legNear = listOf(pt(66f, 74f), pt(68f, 89f)),
            legFar = listOf(pt(34f, 78f), pt(24f, 88f)),
            arrows = listOf(pt(50f, 62f) to pt(54f, 52f)),
            boxes = listOf(ojMat)
        )
    ),

    "提踵加踝泵" to ExerciseGuide(
        name = "提踵加踝泵",
        summary = "先踮脚慢起慢落，再做踝泵",
        steps = listOf(
            "双脚与髋同宽站立，重心在前脚掌",
            "慢起 2 秒踮到最高，停 1 秒，慢落 3 秒，做 12 次",
            "随后坐姿或站姿做踝泵勾绷脚 20 次",
            "全程不弹震，动作连贯"
        ),
        tips = listOf("踮脚顶点稍停，落下更要慢", "重心放在前脚掌内侧", "踝泵幅度做到极限角度"),
        cautions = listOf("小腿刺痛或跟腱不适立即停止", "平衡差者扶桌椅"),
        pose = PoseSpec(
            head = pt(50f, 28f), neck = pt(50f, 37f), hip = pt(50f, 58f),
            armNear = listOf(pt(42f, 52f), pt(40f, 66f)),
            armFar = listOf(pt(58f, 52f), pt(60f, 66f)),
            legNear = listOf(pt(54f, 74f), pt(54f, 89f)),
            legFar = listOf(pt(46f, 74f), pt(46f, 89f)),
            arrows = listOf(pt(62f, 84f) to pt(62f, 74f))
        )
    ),

    "坐姿夹臀" to ExerciseGuide(
        name = "坐姿夹臀",
        summary = "坐姿收紧臀部再放松，腰不动",
        steps = listOf(
            "坐在椅子前 1/2，双脚踩地，腰背挺直",
            "臀部用力夹紧，保持 5 秒",
            "缓慢放松 2 秒，重复 8 次",
            "全程腰和骨盆保持不动"
        ),
        tips = listOf("发力感在臀部而非大腿", "不要靠挺腰来带动", "自然呼吸不憋气"),
        cautions = listOf("腰部出现代偿痛减小力度", "尾骨不适者调整坐姿"),
        pose = PoseSpec(
            head = ojSeated.head, neck = ojSeated.neck, hip = ojSeated.hip,
            armNear = listOf(pt(38f, 50f), pt(38f, 62f)),
            armFar = listOf(pt(58f, 50f), pt(58f, 62f)),
            legNear = listOf(pt(60f, 66f), pt(60f, 88f)),
            legFar = listOf(pt(50f, 66f), pt(50f, 88f)),
            arrows = listOf(pt(44f, 62f) to pt(52f, 62f), pt(56f, 62f) to pt(48f, 62f)),
            boxes = listOf(ojChair)
        )
    ),

    "靠墙天使" to ExerciseGuide(
        name = "靠墙天使",
        summary = "背贴墙，手臂沿墙由 W 滑到 Y",
        steps = listOf(
            "后脑、上背、臀部贴墙，手臂呈 W 形贴墙",
            "手臂沿墙面缓慢上滑到 Y 形，停 1-2 秒",
            "再沿墙滑回 W，全程手臂不离墙",
            "重复 8-10 次，肋骨不外翻"
        ),
        tips = listOf("手臂始终贴墙不悬空", "腰部与墙的缝隙不要刻意压平", "动作慢，感受肩胛下缘发力"),
        cautions = listOf("肩关节疼痛立即减小上滑幅度", "颈部不适者后脑可离墙"),
        pose = PoseSpec(
            head = pt(50f, 28f), neck = pt(50f, 37f), hip = pt(50f, 60f),
            armNear = listOf(pt(40f, 46f), pt(30f, 34f)),
            armFar = listOf(pt(60f, 46f), pt(70f, 34f)),
            legNear = listOf(pt(54f, 75f), pt(54f, 89f)),
            legFar = listOf(pt(46f, 75f), pt(46f, 89f)),
            arrows = listOf(pt(30f, 34f) to pt(22f, 22f), pt(70f, 34f) to pt(78f, 22f)),
            props = listOf(poseLine(16f, 12f, 16f, 91f))
        )
    ),

    // ==================== 办公室 / 阶段2 建立 ====================

    "坐姿胸椎旋转" to ExerciseGuide(
        name = "坐姿胸椎旋转",
        summary = "坐姿转胸廓，骨盆保持不动",
        steps = listOf(
            "坐直，双膝并拢，双手抱胸或扶肩",
            "呼气时上半身向一侧旋转，眼睛跟随手肘",
            "停在末端 1-2 秒，再缓慢回到中立",
            "每侧 8-10 次，骨盆不跟着转"
        ),
        tips = listOf("转动来自胸椎而非腰椎", "骨盆和膝盖固定朝前", "呼气时旋转幅度更大"),
        cautions = listOf("腰部扭转疼痛立即停止", "避免快速甩动"),
        pose = PoseSpec(
            head = pt(56f, 29f), neck = pt(52f, 38f), hip = pt(48f, 60f),
            armNear = listOf(pt(62f, 46f), pt(72f, 44f)),
            armFar = listOf(pt(44f, 46f), pt(34f, 44f)),
            legNear = listOf(pt(60f, 66f), pt(60f, 88f)),
            legFar = listOf(pt(52f, 66f), pt(52f, 88f)),
            arrows = listOf(pt(44f, 34f) to pt(66f, 32f)),
            boxes = listOf(ojChair)
        )
    ),

    "墙俯卧撑" to ExerciseGuide(
        name = "墙俯卧撑",
        summary = "面对墙屈肘，胸靠墙再推回",
        steps = listOf(
            "面对墙站立，一臂距离，双手扶墙与肩同高",
            "屈肘让胸部缓慢靠近墙面",
            "停 1 秒后推回起始位",
            "重复 12-15 次，身体保持一条直线"
        ),
        tips = listOf("身体从头到脚跟成一条直线", "不要塌腰或撅臀", "推回时不要锁死肘关节"),
        cautions = listOf("肩关节疼痛立即停止", "手腕不适可改用握拳"),
        pose = PoseSpec(
            head = pt(60f, 34f), neck = pt(58f, 43f), hip = pt(48f, 62f),
            armNear = listOf(pt(74f, 46f), pt(84f, 40f)),
            armFar = listOf(pt(70f, 48f), pt(80f, 42f)),
            legNear = listOf(pt(52f, 76f), pt(44f, 89f)),
            legFar = listOf(pt(46f, 76f), pt(38f, 89f)),
            arrows = listOf(pt(62f, 40f) to pt(70f, 40f)),
            props = listOf(poseLine(88f, 14f, 88f, 91f))
        )
    ),

    "站姿髋铰链" to ExerciseGuide(
        name = "站姿髋铰链",
        summary = "臀向后推，背挺直，模拟折叠髋部",
        steps = listOf(
            "双脚与髋同宽，膝微屈，双手扶桌或放大腿",
            "臀部向后推，身体从髋部折叠前倾",
            "感到大腿后侧发紧时停 1 秒",
            "收臀带回身体，重复 12 次，腰背始终平直"
        ),
        tips = listOf("动作从髋部折叠而非弯腰", "背始终平直", "感受腘绳肌（大腿后侧）牵拉"),
        cautions = listOf("腰部发酸立即停止，检查是否弯腰", "膝盖保持微屈不锁死"),
        pose = PoseSpec(
            head = pt(40f, 40f), neck = pt(44f, 47f), hip = pt(58f, 60f),
            armNear = listOf(pt(66f, 66f), pt(76f, 60f)),
            armFar = listOf(pt(64f, 64f), pt(74f, 58f)),
            legNear = listOf(pt(62f, 75f), pt(60f, 89f)),
            legFar = listOf(pt(54f, 75f), pt(52f, 89f)),
            arrows = listOf(pt(70f, 60f) to pt(80f, 56f)),
            props = listOf(poseLine(84f, 52f, 98f, 52f))
        )
    ),

    "扶桌单腿站立" to ExerciseGuide(
        name = "扶桌单腿站立",
        summary = "手指轻搭桌沿，单腿站保持骨盆水平",
        steps = listOf(
            "侧身站在桌旁，手指轻搭桌沿",
            "一腿屈膝抬起悬空，支撑腿站稳",
            "保持 30 秒，骨盆不歪、踝不晃",
            "换腿，进阶可闭眼或转头 10 秒"
        ),
        tips = listOf("手指轻搭不要用力抓", "骨盆保持水平不倾斜", "脚趾抓地提高稳定性"),
        cautions = listOf("踝关节疼痛性松动立即停止", "失去平衡时及时扶桌"),
        pose = PoseSpec(
            head = pt(50f, 28f), neck = pt(50f, 37f), hip = pt(54f, 58f),
            armNear = listOf(pt(66f, 44f), pt(82f, 44f)),
            armFar = listOf(pt(44f, 48f), pt(42f, 60f)),
            legNear = listOf(pt(66f, 68f), pt(62f, 82f)),
            legFar = listOf(pt(54f, 74f), pt(54f, 89f)),
            props = listOf(poseLine(86f, 44f, 98f, 44f))
        )
    ),

    "站姿对侧抬" to ExerciseGuide(
        name = "站姿对侧抬",
        summary = "扶桌，对侧手脚同时伸出",
        steps = listOf(
            "面对桌站立，双手轻扶桌沿",
            "一侧手和对侧腿同时向前伸出，停 2 秒",
            "缓慢收回，换对侧",
            "每侧 8 次，身体不摇晃"
        ),
        tips = listOf("手脚对角伸出", "核心收紧保持躯干稳定", "动作慢不甩动"),
        cautions = listOf("腰部疼痛立即停止", "站立不稳时减小抬腿幅度"),
        pose = PoseSpec(
            head = pt(50f, 28f), neck = pt(50f, 37f), hip = pt(50f, 58f),
            armNear = listOf(pt(38f, 44f), pt(28f, 38f)),
            armFar = listOf(pt(42f, 44f), pt(34f, 40f)),
            legNear = listOf(pt(62f, 70f), pt(72f, 80f)),
            legFar = listOf(pt(48f, 74f), pt(48f, 89f)),
            arrows = listOf(pt(72f, 80f) to pt(82f, 76f)),
            props = listOf(poseLine(20f, 40f, 40f, 40f))
        )
    ),

    "靠墙静蹲加提踵" to ExerciseGuide(
        name = "靠墙静蹲加提踵",
        summary = "先靠墙静蹲，起身后再提踵",
        steps = listOf(
            "背贴墙下滑至膝屈 60° 内，静蹲 45 秒",
            "膝不超过脚尖，保持均匀呼吸",
            "起身站直后做提踵 10 次",
            "慢起慢落，全程不弹震"
        ),
        tips = listOf("膝盖对准脚尖方向", "静蹲时腰背贴墙", "提踵顶点稍停"),
        cautions = listOf("膝关节疼痛减小下蹲深度", "踝部不适减小提踵幅度"),
        pose = PoseSpec(
            head = pt(58f, 34f), neck = pt(56f, 43f), hip = pt(58f, 62f),
            armNear = listOf(pt(48f, 56f), pt(48f, 70f)),
            armFar = listOf(pt(44f, 56f), pt(44f, 70f)),
            legNear = listOf(pt(72f, 74f), pt(70f, 89f)),
            legFar = listOf(pt(64f, 74f), pt(62f, 89f)),
            props = listOf(poseLine(50f, 14f, 50f, 91f))
        )
    ),

    "髋屈肌加小腿拉伸" to ExerciseGuide(
        name = "髋屈肌加小腿拉伸",
        summary = "弓步拉髋屈肌，再推墙拉小腿",
        steps = listOf(
            "弓步站位，收臀送髋拉大腿根前侧 20 秒",
            "换侧后面对墙，一脚前一脚后",
            "后脚脚跟踩实，屈前膝推墙拉小腿 20 秒",
            "两侧各做一遍，动作平缓"
        ),
        tips = listOf("拉髋时先收臀", "拉小腿时后脚跟要踩实", "不塌腰翘臀"),
        cautions = listOf("膝或跟腱疼痛立即停止", "牵拉到轻微酸胀即可"),
        pose = PoseSpec(
            head = pt(52f, 30f), neck = pt(52f, 39f), hip = pt(52f, 60f),
            armNear = listOf(pt(66f, 48f), pt(78f, 44f)),
            armFar = listOf(pt(64f, 48f), pt(76f, 44f)),
            legNear = listOf(pt(62f, 74f), pt(66f, 89f)),
            legFar = listOf(pt(38f, 76f), pt(26f, 88f)),
            arrows = listOf(pt(54f, 62f) to pt(56f, 52f)),
            props = listOf(poseLine(84f, 16f, 84f, 91f))
        )
    ),

    // ==================== 久坐三关节 / 脚踝段 ====================

    "坐姿踝泵" to ExerciseGuide(
        name = "坐姿踝泵",
        summary = "坐姿勾脚绷脚交替，消肿第一优先",
        steps = listOf(
            "坐稳，双脚抬起或脚跟着地，小腿放松",
            "脚尖缓慢用力向自己勾起，停 1 秒",
            "再缓慢向下绷直，停 1 秒",
            "勾绷交替做 30 次，幅度到极限"
        ),
        tips = listOf("速度越慢效果越好", "幅度比次数重要", "久坐每小时都做一次"),
        cautions = listOf("踝关节刺痛立即停止", "肿胀明显加重时减少次数"),
        pose = PoseSpec(
            head = ojSeated.head, neck = ojSeated.neck, hip = ojSeated.hip,
            armNear = listOf(pt(38f, 50f), pt(38f, 62f)),
            armFar = listOf(pt(58f, 50f), pt(58f, 62f)),
            legNear = listOf(pt(64f, 70f), pt(82f, 66f)),
            legFar = listOf(pt(58f, 72f), pt(76f, 70f)),
            arrows = listOf(pt(86f, 58f) to pt(86f, 74f)),
            boxes = listOf(ojChair)
        )
    ),

    "空中画字母" to ExerciseGuide(
        name = "空中画字母",
        summary = "一脚悬空写 A-Z 或画 8 字",
        steps = listOf(
            "坐稳或仰卧，一脚脚踝悬空",
            "用脚尖在空中缓慢写 A 到 Z",
            "或画横 8 字，幅度到无痛极限",
            "脚踝做全范围活动，左右脚交替"
        ),
        tips = listOf("幅度越大越好，但保持无痛", "用整个脚踝画，不是只用脚趾", "速度慢，感受全范围活动"),
        cautions = listOf("踝关节疼痛立即减小幅度", "头晕时改为坐姿"),
        pose = PoseSpec(
            head = ojSeated.head, neck = ojSeated.neck, hip = ojSeated.hip,
            armNear = listOf(pt(38f, 50f), pt(38f, 62f)),
            armFar = listOf(pt(58f, 50f), pt(58f, 62f)),
            legNear = listOf(pt(64f, 70f), pt(84f, 64f)),
            legFar = listOf(pt(58f, 72f), pt(74f, 74f)),
            arrows = listOf(pt(88f, 60f) to pt(92f, 68f), pt(92f, 68f) to pt(88f, 74f))
        )
    ),

    "踝外翻自我抗阻" to ExerciseGuide(
        name = "踝外翻自我抗阻",
        summary = "双脚并拢互顶，患脚向外翻",
        steps = listOf(
            "坐姿或站姿，双脚外侧并拢互相顶住",
            "患脚缓慢用力向外翻，对侧脚给阻力对抗",
            "保持 10 秒后放松，做 4 次",
            "用力温和，关节位置基本不动"
        ),
        tips = listOf("感受小腿外侧腓骨肌发力", "对抗用力约三成即可", "动作缓慢不弹震"),
        cautions = listOf("踝外侧疼痛立即停止", "不要用力过猛扭伤"),
        pose = PoseSpec(
            head = ojSeated.head, neck = ojSeated.neck, hip = ojSeated.hip,
            armNear = listOf(pt(38f, 50f), pt(38f, 62f)),
            armFar = listOf(pt(58f, 50f), pt(58f, 62f)),
            legNear = listOf(pt(66f, 72f), pt(80f, 86f)),
            legFar = listOf(pt(60f, 72f), pt(72f, 86f)),
            arrows = listOf(pt(86f, 86f) to pt(94f, 82f))
        )
    ),

    // ==================== 久坐三关节 / 膝盖段 ====================

    "坐姿提踵" to ExerciseGuide(
        name = "坐姿提踵",
        summary = "坐姿双脚踮起，感受小腿泵血",
        steps = listOf(
            "坐在椅子前 1/2，双脚踩地平放",
            "双脚脚跟缓慢抬起，前脚掌撑地",
            "顶点停 1 秒，再缓慢落下",
            "重复 15 次，感受小腿收缩"
        ),
        tips = listOf("慢起慢落，顶点稍停", "重心在前脚掌", "坐着做对腰更友好"),
        cautions = listOf("小腿刺痛立即停止", "跟腱不适减小幅度"),
        pose = PoseSpec(
            head = ojSeated.head, neck = ojSeated.neck, hip = ojSeated.hip,
            armNear = listOf(pt(38f, 50f), pt(38f, 62f)),
            armFar = listOf(pt(58f, 50f), pt(58f, 62f)),
            legNear = listOf(pt(62f, 74f), pt(62f, 88f)),
            legFar = listOf(pt(52f, 74f), pt(52f, 88f)),
            arrows = listOf(pt(70f, 88f) to pt(70f, 78f)),
            boxes = listOf(ojChair)
        )
    ),

    "坐姿髌骨松动" to ExerciseGuide(
        name = "坐姿髌骨松动",
        summary = "拇指轻推膝盖骨四向移动",
        steps = listOf(
            "坐姿放松，腿伸直或微屈",
            "用拇指指腹轻按膝盖骨（髌骨）边缘",
            "缓慢向上、下、左、右各推 10 次",
            "在无痛范围内活动，改善髌骨活动度"
        ),
        tips = listOf("推动幅度小、动作轻", "只在不痛范围内做", "感受髌骨轻微滑动即可"),
        cautions = listOf("推压疼痛立即停止", "膝关节急性肿胀时不要做"),
        pose = PoseSpec(
            head = ojSeated.head, neck = ojSeated.neck, hip = ojSeated.hip,
            armNear = listOf(pt(56f, 56f), pt(70f, 66f)),
            armFar = listOf(pt(52f, 56f), pt(64f, 64f)),
            legNear = listOf(pt(70f, 72f), pt(88f, 80f)),
            legFar = listOf(pt(60f, 74f), pt(76f, 86f)),
            arrows = listOf(
                pt(80f, 72f) to pt(80f, 66f), pt(80f, 72f) to pt(80f, 78f),
                pt(80f, 72f) to pt(74f, 72f), pt(80f, 72f) to pt(86f, 72f)
            ),
            boxes = listOf(ojChair)
        )
    ),

    "坐姿股四头肌收紧" to ExerciseGuide(
        name = "坐姿股四头肌收紧",
        summary = "伸直腿收紧大腿前侧再放松",
        steps = listOf(
            "坐姿，一侧腿伸直，脚尖回勾",
            "大腿前侧（股四头肌）主动收紧，膝盖下压",
            "保持 5 秒后放松，重复 10 次",
            "每侧交替，腰背保持挺直"
        ),
        tips = listOf("感受大腿前侧变硬", "膝盖下压但不痛", "收紧时正常呼吸"),
        cautions = listOf("膝关节疼痛立即停止", "不要憋气用力"),
        pose = PoseSpec(
            head = ojSeated.head, neck = ojSeated.neck, hip = ojSeated.hip,
            armNear = listOf(pt(38f, 50f), pt(38f, 62f)),
            armFar = listOf(pt(58f, 50f), pt(58f, 62f)),
            legNear = listOf(pt(66f, 70f), pt(88f, 72f)),
            legFar = listOf(pt(56f, 74f), pt(58f, 88f)),
            arrows = listOf(pt(80f, 80f) to pt(80f, 74f)),
            boxes = listOf(ojChair)
        )
    ),

    "扶桌坐站转换" to ExerciseGuide(
        name = "扶桌坐站转换",
        summary = "从椅子站起再缓慢坐下，膝对脚尖",
        steps = listOf(
            "坐在椅子前部，双脚踩地与肩同宽",
            "身体微微前倾，臀部离开椅面站起",
            "站稳后缓慢屈髋屈膝坐下，轻触椅面",
            "重复 10 次，膝盖对准脚尖不内扣"
        ),
        tips = listOf("起身靠臀腿发力而非用手撑", "坐下要慢、有控制", "膝盖方向对准脚尖"),
        cautions = listOf("膝关节疼痛减小下坐深度", "头晕者起身放慢"),
        pose = PoseSpec(
            head = pt(48f, 30f), neck = pt(48f, 39f), hip = pt(50f, 62f),
            armNear = listOf(pt(60f, 48f), pt(74f, 46f)),
            armFar = listOf(pt(56f, 48f), pt(70f, 46f)),
            legNear = listOf(pt(60f, 74f), pt(58f, 89f)),
            legFar = listOf(pt(50f, 74f), pt(48f, 89f)),
            arrows = listOf(pt(50f, 66f) to pt(50f, 54f)),
            boxes = listOf(ojChair),
            props = listOf(poseLine(78f, 46f, 96f, 46f))
        )
    ),

    // ==================== 久坐三关节 / 髋关节段 ====================

    "坐姿髋外展" to ExerciseGuide(
        name = "坐姿髋外展",
        summary = "坐姿双膝分开，手向内对抗",
        steps = listOf(
            "坐椅上，双脚踩地，双膝并拢",
            "双手掌根抵住膝盖内侧，双膝向外打开与手掌对抗",
            "对抗保持 5 秒后放松，重复 10 次",
            "感受臀部外侧发力"
        ),
        tips = listOf("臀部外侧发力而非大腿内侧", "对抗约五成力", "骨盆保持端正"),
        cautions = listOf("髋关节疼痛立即停止", "腰不要跟着后仰"),
        pose = PoseSpec(
            head = ojSeated.head, neck = ojSeated.neck, hip = ojSeated.hip,
            armNear = listOf(pt(40f, 52f), pt(52f, 64f)),
            armFar = listOf(pt(56f, 52f), pt(46f, 64f)),
            legNear = listOf(pt(66f, 68f), pt(70f, 88f)),
            legFar = listOf(pt(32f, 68f), pt(28f, 88f)),
            arrows = listOf(pt(74f, 66f) to pt(84f, 62f), pt(24f, 66f) to pt(14f, 62f)),
            boxes = listOf(ojChair)
        )
    ),

    "坐位腿后侧拉伸" to ExerciseGuide(
        name = "坐位腿后侧拉伸",
        summary = "坐姿伸腿勾脚，从髋前倾拉腿后侧",
        steps = listOf(
            "坐椅前部，一腿伸直脚尖回勾",
            "从髋部开始缓慢向前倾，背保持平直",
            "感到大腿后侧牵拉时保持 30 秒",
            "换腿，不要弓背低头"
        ),
        tips = listOf("从髋部折叠而非弯腰", "背保持平直", "脚尖回勾加强牵拉"),
        cautions = listOf("大腿后侧刺痛立即停止", "腰部不适减小前倾幅度"),
        pose = PoseSpec(
            head = pt(38f, 40f), neck = pt(42f, 47f), hip = pt(50f, 60f),
            armNear = listOf(pt(56f, 66f), pt(74f, 70f)),
            armFar = listOf(pt(54f, 64f), pt(70f, 68f)),
            legNear = listOf(pt(72f, 74f), pt(90f, 84f)),
            legFar = listOf(pt(56f, 74f), pt(56f, 88f)),
            arrows = listOf(pt(46f, 44f) to pt(38f, 58f)),
            boxes = listOf(ojChair)
        )
    ),

    "坐姿二郎腿拉伸" to ExerciseGuide(
        name = "坐姿二郎腿拉伸",
        summary = "一脚踝搭对侧膝，抱腿拉向胸",
        steps = listOf(
            "坐姿，一脚踝搭在另一侧膝盖上成二郎腿",
            "双手抱住下方大腿，缓慢拉向胸口",
            "感到臀部外侧牵拉时保持 30 秒",
            "换腿，背保持挺直不驼背"
        ),
        tips = listOf("背部挺直不弓背", "牵拉感在臀部外侧", "缓慢拉近不要猛拉"),
        cautions = listOf("膝或髋疼痛立即停止", "高血压者避免低头"),
        pose = PoseSpec(
            head = ojSeated.head, neck = ojSeated.neck, hip = ojSeated.hip,
            armNear = listOf(pt(42f, 52f), pt(52f, 64f)),
            armFar = listOf(pt(56f, 54f), pt(60f, 66f)),
            legNear = listOf(pt(66f, 66f), pt(52f, 70f)),
            legFar = listOf(pt(56f, 74f), pt(56f, 88f)),
            arrows = listOf(pt(52f, 66f) to pt(46f, 56f)),
            boxes = listOf(ojChair)
        )
    ),

    "扶桌侧向抬腿" to ExerciseGuide(
        name = "扶桌侧向抬腿",
        summary = "扶桌，腿向外侧抬起，脚尖朝前",
        steps = listOf(
            "侧身站在桌旁，手扶桌面",
            "外侧腿伸直向侧面抬起约 30°",
            "停 1 秒后缓慢落下",
            "每侧 12 次，脚尖始终朝前不外旋"
        ),
        tips = listOf("臀部外侧发力", "脚尖朝前不外旋", "身体保持直立不侧倾"),
        cautions = listOf("髋关节疼痛立即停止", "腰部不适减小抬腿高度"),
        pose = PoseSpec(
            head = pt(48f, 28f), neck = pt(48f, 37f), hip = pt(50f, 58f),
            armNear = listOf(pt(60f, 44f), pt(74f, 44f)),
            armFar = listOf(pt(40f, 46f), pt(38f, 58f)),
            legNear = listOf(pt(62f, 70f), pt(76f, 82f)),
            legFar = listOf(pt(48f, 74f), pt(48f, 89f)),
            arrows = listOf(pt(76f, 82f) to pt(86f, 78f)),
            props = listOf(poseLine(78f, 44f, 96f, 44f))
        )
    ),

    "扶桌单腿站" to ExerciseGuide(
        name = "扶桌单腿站",
        summary = "手指轻搭桌沿，单腿站踝不晃",
        steps = listOf(
            "侧身站在桌旁，手指轻搭桌沿",
            "一腿屈膝抬起悬空，支撑腿站稳",
            "保持 20-30 秒，脚踝尽量不晃",
            "换腿；恢复期可改双脚站"
        ),
        tips = listOf("手指轻搭不发力", "脚趾抓地提高稳定", "骨盆保持水平"),
        cautions = listOf("踝关节疼痛性松动立即停止", "失衡时及时扶桌"),
        pose = PoseSpec(
            head = pt(50f, 28f), neck = pt(50f, 37f), hip = pt(54f, 58f),
            armNear = listOf(pt(66f, 44f), pt(82f, 44f)),
            armFar = listOf(pt(44f, 48f), pt(42f, 60f)),
            legNear = listOf(pt(66f, 68f), pt(62f, 82f)),
            legFar = listOf(pt(54f, 74f), pt(54f, 89f)),
            props = listOf(poseLine(86f, 44f, 98f, 44f))
        )
    )
)
