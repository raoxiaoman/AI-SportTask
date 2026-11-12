package pages
import ui.AppIcon
import ui.AppIcons
import ui.theme.*


import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.SportTaskRepository
import data.GroupItem
import ui.EmptyState
import ui.IconBadge
import ui.LiteCard
import ui.PageHeader
import ui.PlatformBackHandler
import ui.SectionHeader
import ui.ActionGuideDialog
import ui.ExerciseGuide
import ui.buildFallbackGuide
import ui.guideFor
import data.ActionItem
import data.TrainingResult
import data.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 训练屏幕
@Composable
fun TrainingScreen(
    onTrainingComplete: (() -> Unit)? = null,
    onImmersiveChange: (Boolean) -> Unit = {}
) {
    val repository = SportTaskRepository
    // 同步读取首帧数据：不再显示依赖异步加载的全屏转圈页
    var groups by remember { mutableStateOf(repository.loadGroupItemsSync()) }
    var selectedGroup by remember { mutableStateOf<GroupItem?>(null) }
    var showPrepareScreen by remember { mutableStateOf(false) }
    var showExecuteScreen by remember { mutableStateOf(false) }
    var planTask by remember {
        mutableStateOf(runCatching { data.plan.PlanRepository.todayTask() }.getOrNull())
    }
    var weekRhythm by remember {
        mutableStateOf(runCatching { data.plan.PlanRepository.weekRhythm() }.getOrNull())
    }
    var isImporting by remember { mutableStateOf(false) }
    var planMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        groups = repository.loadGroupItemsSync()
        planTask = runCatching { data.plan.PlanRepository.todayTask() }.getOrNull()
        weekRhythm = runCatching { data.plan.PlanRepository.weekRhythm() }.getOrNull()
        data.plan.PlanRepository.refreshReminder()
    }

    fun importPlan() {
        if (isImporting) return
        isImporting = true
        scope.launch {
            runCatching { data.plan.PlanRepository.importRehabPlan() }
                .onSuccess {
                    planMessage = "已导入 8 周康复计划（${it.groupIds.size} 个阶段）"
                    val config = data.plan.ReminderController.current()
                    data.plan.ReminderController.setEnabled(true, config.hour, config.minute)
                }
                .onFailure { planMessage = "导入失败：${it.message}" }
            reload()
            isImporting = false
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    // 执行页为沉浸式：隐藏底部导航，离开时恢复
    LaunchedEffect(showPrepareScreen, showExecuteScreen) {
        onImmersiveChange(showPrepareScreen || showExecuteScreen)
    }
    DisposableEffect(Unit) { onDispose { onImmersiveChange(false) } }

    if (showExecuteScreen && selectedGroup != null) {
        TrainingExecuteScreen(
            group = selectedGroup!!,
            onBackClick = {
                showExecuteScreen = false
                selectedGroup = null
            },
            onComplete = { result ->
                // Fix: 保存打卡记录到数据库
                scope.launch {
                    withContext(Dispatchers.Default) {
                        val today = todayDateString()
                        repository.addCheckin(
                            date = today,
                            groupId = result.groupId,
                            actionId = null,
                            duration = result.duration.toLong(),
                            isCompleted = if (result.completed) 1L else 0L
                        )
                    }
                    planTask = data.plan.PlanRepository.todayTask()
                    weekRhythm = data.plan.PlanRepository.weekRhythm()
                    data.plan.PlanRepository.refreshReminder()
                    onTrainingComplete?.invoke()
                }
                showExecuteScreen = false
                selectedGroup = null
            }
        )
        return@TrainingScreen
    }

    if (showPrepareScreen && selectedGroup != null) {
        TrainingPrepareScreen(
            group = selectedGroup!!,
            onBackClick = {
                showPrepareScreen = false
                selectedGroup = null
            },
            onStartClick = {
                showPrepareScreen = false
                showExecuteScreen = true
            }
        )
        return@TrainingScreen
    }

    LazyColumn(
        // 边距由各 item 自带（PageHeader/SectionHeader/卡片各自套 screenPadding），
        // 不在列表层再叠一层，避免与 PageHeader 内部 padding 双重叠加
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
        verticalArrangement = Arrangement.spacedBy(AppDimens.s2)
    ) {
        // 页头：日期 + 问候语 + 连续打卡徽标
        item {
            PageHeader(
                eyebrow = displayDate(todayDateString()),
                title = greetingForHour(currentHourOfDay()),
                trailing = {
                    IconBadge(
                        icon = AppIcons.Streak,
                        contentDescription = "连续打卡",
                        tint = AppTheme.colors.warning,
                        container = AppTheme.colors.warningSoft
                    )
                }
            )
        }

        // 今日任务主卡
        item {
            TodayTaskCard(
                task = planTask,
                isImporting = isImporting,
                message = planMessage,
                onImport = { importPlan() },
                onStart = { groupId ->
                    groups.firstOrNull { it.id == groupId }?.let { group ->
                        selectedGroup = group
                        showPrepareScreen = true
                    }
                }
            )
        }

        // 本周节奏
        weekRhythm?.let { rhythm ->
            item {
                val completedCount = rhythm.count { it.isCompleted }
                SectionHeader(
                    text = "本周节奏",
                    trailing = {
                        Text(
                            text = if (completedCount > 0) "已完成 ${completedCount} 天"
                            else "本周还未打卡",
                            style = MaterialTheme.typography.caption,
                            color = if (completedCount > 0) AppTheme.colors.success
                            else AppTheme.colors.inkMuted
                        )
                    }
                )
            }
            item { WeekRhythmChips(days = rhythm) }
        }

        // 康复阶段（训练分组）
        if (groups.isNotEmpty()) {
            item { SectionHeader(text = "康复阶段") }
            items(groups) { group ->
                TrainingGroupRow(group = group) {
                    selectedGroup = group
                    showPrepareScreen = true
                }
            }
        }

        item { Spacer(modifier = Modifier.height(AppDimens.s6)) }
    }
}

// 本周节奏 chips 行：完成=绿色、今天=品牌蓝、计划内训练日=浅蓝、其余=浅灰
@Composable
fun WeekRhythmChips(days: List<data.plan.WeekDayStatus>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.s1)
    ) {
        days.forEach { day ->
            val (chipBg, chipFg) = when {
                day.isCompleted -> AppTheme.colors.successSoft to AppTheme.colors.success
                day.isToday -> AppTheme.colors.brand to AppTheme.colors.onBrand
                day.inPlan && day.isTrainingDay -> AppTheme.colors.brandSoft to AppTheme.colors.brandStrong
                else -> AppTheme.colors.surfaceAlt to AppTheme.colors.inkMuted
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(AppDimens.weekChipHeight)
                    .background(color = chipBg, shape = AppShapes.chip),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.caption,
                    color = chipFg
                )
            }
        }
    }
}

// 今日任务主卡：品牌深蓝渐变 + KPI 半透明块 + 白色全宽 CTA
@Composable
fun TodayTaskCard(
    task: data.plan.TodayTask?,
    isImporting: Boolean,
    message: String?,
    onImport: () -> Unit,
    onStart: (Long) -> Unit
) {
    // CTA 文字按主题取色：浅色用深蓝（白底 9.8:1），深色用亮蓝（深底约 7:1）
    val ctaTextColor = if (AppTheme.colors.isLight) AppTheme.colors.brandDeep else AppTheme.colors.brand

    val label: String
    val title: String
    val subtitle: String
    val pillText: String?
    val kpis: List<Pair<String, String>>?
    val showStartCta: Boolean
    val startCtaText: String
    var startGroupId: Long? = null

    if (task == null) {
        label = "训练计划"
        title = "开始你的第一周训练"
        subtitle = "导入 8 周康复计划，按阶段自动安排每天的动作"
        pillText = null
        kpis = null
        showStartCta = false
        startCtaText = "导入康复计划"
    } else {
        label = if (task.finished) "训练计划" else "今日训练 · ${task.phaseTitle}"
        title = task.planName
        subtitle = when {
            task.finished -> "8 周计划已完成，保持每周 2 次训练"
            task.notStarted -> "计划未开始 · 起始日 ${task.startDate}"
            task.completed -> "今天已完成 · 睡前做 5 分钟拉伸"
            !task.isTrainingDay -> "今天是休息日 · 建议 5 分钟踝泵 + 拉伸"
            else -> "康复计划 · 训练日"
        }
        pillText = if (task.finished || task.notStarted) null else "第 ${task.dayInPhase + 1}/${task.phaseDays} 天"
        val reminder = data.plan.ReminderController.current()
        val reminderText = if (reminder.enabled)
            "${reminder.hour.toString().padStart(2, '0')}:${reminder.minute.toString().padStart(2, '0')}"
        else "未开启"
        kpis = if (task.finished || task.notStarted) null
        else listOf(
            "${task.actionCount}" to "动作",
            "${task.estimatedMinutes} min" to "预计",
            reminderText to "提醒"
        )
        showStartCta = task.isTrainingDay && !task.completed && task.groupId != null
        startGroupId = if (showStartCta) task.groupId else null
        startCtaText = "开始今日训练"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(AppTheme.colors.brandDeep, AppTheme.colors.brand),
                    start = Offset(0f, 0f),
                    end = Offset(1200f, 1200f)
                ),
                shape = AppShapes.cardLarge
            )
            .padding(AppDimens.cardPadding)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.onBrand.copy(alpha = 0.85f)
                )
                pillText?.let {
                    Box(
                        modifier = Modifier
                            .background(
                                color = AppTheme.colors.onBrand.copy(alpha = 0.15f),
                                shape = AppShapes.pill
                            )
                            .padding(horizontal = AppDimens.s2, vertical = AppDimens.s1)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.caption,
                            color = AppTheme.colors.onBrand
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(AppDimens.s2))
            Text(
                text = title,
                style = MaterialTheme.typography.h3,
                fontWeight = AppTypeWeight.emphasis,
                color = AppTheme.colors.onBrand
            )
            Spacer(modifier = Modifier.height(AppDimens.s1))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.caption,
                color = AppTheme.colors.onBrand.copy(alpha = 0.78f)
            )

            kpis?.let { list ->
                Spacer(modifier = Modifier.height(AppDimens.s3))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.s2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    list.forEach { (value, name) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = AppTheme.colors.onBrand.copy(alpha = 0.12f),
                                    shape = AppShapes.button
                                )
                                .padding(horizontal = AppDimens.s2, vertical = AppDimens.s2)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.subtitle2,
                                    fontWeight = AppTypeWeight.emphasis,
                                    color = AppTheme.colors.onBrand
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.caption,
                                    color = AppTheme.colors.onBrand.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            }

            if (showStartCta) {
                Spacer(modifier = Modifier.height(AppDimens.s3))
                Button(
                    onClick = { startGroupId?.let(onStart) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.button,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppTheme.colors.surface,
                        contentColor = ctaTextColor
                    )
                ) {
                    Text(text = startCtaText, color = ctaTextColor)
                }
            }
            if (task == null) {
                Spacer(modifier = Modifier.height(AppDimens.s3))
                Button(
                    onClick = onImport,
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.button,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppTheme.colors.surface,
                        contentColor = ctaTextColor
                    )
                ) {
                    Text(
                        text = if (isImporting) "导入中…" else "导入康复计划",
                        color = ctaTextColor
                    )
                }
            }
            if (task != null && task.finished) {
                Spacer(modifier = Modifier.height(AppDimens.s3))
                Button(
                    onClick = onImport,
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.button,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppTheme.colors.surface,
                        contentColor = ctaTextColor
                    )
                ) {
                    Text(
                        text = if (isImporting) "导入中…" else "重新开始 8 周计划",
                        color = ctaTextColor
                    )
                }
            }
            message?.let {
                Spacer(modifier = Modifier.height(AppDimens.s2))
                Text(
                    text = it,
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.onBrand.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// 康复阶段行卡：图标容器 + 标题/副标题 + 右侧箭头
@Composable
fun TrainingGroupRow(group: GroupItem, onClick: () -> Unit) {
    LiteCard(
        modifier = Modifier.padding(horizontal = AppDimens.screenPadding),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDimens.s3,
                vertical = AppDimens.s3
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = AppIcons.Groups,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(AppDimens.s3))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.body1,
                    fontWeight = AppTypeWeight.medium,
                    color = AppTheme.colors.ink
                )
                Text(
                    text = "${group.actionCount} 个动作",
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.inkSub
                )
            }
            AppIcon(
                imageVector = AppIcons.Chevron,
                contentDescription = "进入",
                tint = AppTheme.colors.inkMuted,
                size = AppDimens.iconSmall
            )
        }
    }
}

// 训练准备屏幕 - 显示即将执行的动作列表
@Composable
fun TrainingPrepareScreen(
    group: GroupItem,
    onBackClick: () -> Unit,
    onStartClick: () -> Unit
) {
    val repository = SportTaskRepository
    // 首帧同步读取动作，避免进入页面时先闪一个「白屏 + 转圈」
    val actions by remember(group.id) { mutableStateOf(repository.loadActionItemsSync(group.id)) }
    var expandedActionId by remember { mutableStateOf<Long?>(null) }

    // 物理返回键与左上角返回按钮走同一逻辑
    PlatformBackHandler(enabled = true, onBack = onBackClick)

    // 总时长：每个动作的时长 + 除最后一个动作外的休息时长
    val totalDuration = if (actions.isEmpty()) 0 else {
        actions.sumOf { it.defaultTime + it.restTime } - actions.last().restTime
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        // 顶部返回行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = AppIcons.Back,
                    contentDescription = "返回"
                )
            }
            Text(
                text = "准备训练",
                style = MaterialTheme.typography.h6,
                fontWeight = AppTypeWeight.emphasis,
                color = AppTheme.colors.ink
            )
        }

        if (actions.isEmpty()) {
            EmptyState(
                icon = AppIcons.Strength,
                title = "该分组暂无动作",
                subtitle = "请先在「分组」页编辑此分组补充动作"
            )
        } else {
            Column(
                modifier = Modifier
                    // 顶部返回行已占据高度，这里必须按剩余空间撑开；
                    // 用 fillMaxSize 会让内层整体超过屏幕，把底部按钮挤出可视区。
                    .weight(1f)
                    .padding(horizontal = AppDimens.screenPadding)
            ) {
                // 训练概览
                LiteCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.s4),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = AppTypeWeight.emphasis,
                            color = AppTheme.colors.ink
                        )
                        Spacer(modifier = Modifier.height(AppDimens.s3))
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${actions.size}",
                                    style = MaterialTheme.typography.h3,
                                    fontWeight = AppTypeWeight.emphasis,
                                    color = AppTheme.colors.brandStrong
                                )
                                Text(
                                    text = "动作数",
                                    style = MaterialTheme.typography.caption,
                                    color = AppTheme.colors.inkSub
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${totalDuration / 60}:${(totalDuration % 60).toString().padStart(2, '0')}",
                                    style = MaterialTheme.typography.h3,
                                    fontWeight = AppTypeWeight.emphasis,
                                    color = AppTheme.colors.brandStrong
                                )
                                Text(
                                    text = "预计时长",
                                    style = MaterialTheme.typography.caption,
                                    color = AppTheme.colors.inkSub
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.s3))
                SectionHeader(text = "动作序列")

                LazyColumn(
                    // 列表吃掉剩余空间，底部主按钮始终固定在可视区域
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.s2)
                ) {
                    items(actions) { action ->
                        val expanded = expandedActionId == action.id
                        LiteCard(onClick = {
                            expandedActionId = if (expanded) null else action.id
                        }) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = AppDimens.s3,
                                    vertical = AppDimens.s3
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${action.orderIndex}.",
                                        style = MaterialTheme.typography.body2,
                                        color = AppTheme.colors.brandStrong,
                                        modifier = Modifier.width(AppDimens.s6)
                                    )
                                    Text(
                                        text = action.name,
                                        style = MaterialTheme.typography.body1,
                                        modifier = Modifier.weight(1f),
                                        color = AppTheme.colors.ink
                                    )
                                    Text(
                                        text = "${action.defaultTime}s",
                                        style = MaterialTheme.typography.caption,
                                        color = AppTheme.colors.inkSub
                                    )
                                    AppIcon(
                                        imageVector = if (expanded)
                                            AppIcons.ChevronUp
                                        else AppIcons.ChevronDown,
                                        contentDescription = if (expanded) "收起" else "展开",
                                        tint = AppTheme.colors.inkSub,
                                        size = AppDimens.iconSmall
                                    )
                                }
                                if (expanded) {
                                    Spacer(modifier = Modifier.height(AppDimens.s2))
                                    Text(
                                        text = "怎么做",
                                        style = MaterialTheme.typography.caption,
                                        color = AppTheme.colors.brandStrong,
                                        fontWeight = AppTypeWeight.emphasis
                                    )
                                    Spacer(modifier = Modifier.height(AppDimens.s1))
                                    Text(
                                        text = if (action.stepsText.isBlank())
                                            "暂无说明，可在「分组」页编辑此动作补充。"
                                        else action.stepsText,
                                        style = MaterialTheme.typography.body2,
                                        color = AppTheme.colors.ink
                                    )
                                    Spacer(modifier = Modifier.height(AppDimens.s2))
                                    Text(
                                        text = "时长 ${action.defaultTime} 秒 · 组后休息 ${action.restTime} 秒",
                                        style = MaterialTheme.typography.caption,
                                        color = AppTheme.colors.inkSub
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(AppDimens.s3)) }
                }

                Spacer(modifier = Modifier.height(AppDimens.s2))

                Button(
                    onClick = onStartClick,
                    enabled = actions.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimens.buttonHeight),
                    shape = AppShapes.button
                ) {
                    AppIcon(AppIcons.Play, contentDescription = null, size = AppDimens.iconSmall)
                    Spacer(modifier = Modifier.width(AppDimens.s2))
                    Text("开始训练")
                }

                Spacer(modifier = Modifier.height(AppDimens.s4))
            }
        }
    }
}

// 训练执行屏幕（沉浸式，无底部导航）
@Composable
fun TrainingExecuteScreen(
    group: GroupItem,
    onBackClick: () -> Unit,
    onComplete: (TrainingResult) -> Unit,
    onTrainingComplete: (() -> Unit)? = null
) {
    val repository = SportTaskRepository
    // 首帧同步读取动作，进入执行页立刻显示倒计时，不再闪全屏加载页
    val actions by remember(group.id) { mutableStateOf(repository.loadActionItemsSync(group.id)) }

    // 训练状态
    var currentActionIndex by remember { mutableStateOf(0) }
    var isResting by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var remainingSeconds by remember(group.id) {
        mutableStateOf(actions.firstOrNull()?.defaultTime ?: 0)
    }
    var totalElapsedSeconds by remember { mutableStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }
    // 首次开始与暂停后恢复播报不同文案；过半提示每个动作只响一次
    var hasStarted by remember { mutableStateOf(false) }
    var halfwayAnnounced by remember { mutableStateOf(false) }

    // 计时器
    val scope = rememberCoroutineScope()

    // 物理返回键：说明弹窗打开时先关弹窗，其余情况与左上角关闭按钮一致
    PlatformBackHandler(enabled = true) {
        if (showDetailDialog) showDetailDialog = false else onBackClick()
    }

    // 预加载本地音频片段，离开执行页时停止并释放
    DisposableEffect(Unit) {
        prepareTrainingAudio()
        onDispose {
            stopTrainingAudio()
            releaseTrainingAudio()
        }
    }

    // 计时器逻辑
    LaunchedEffect(isRunning, currentActionIndex, isResting) {
        if (isRunning && !isCompleted) {
            val action = actions.getOrNull(currentActionIndex) ?: return@LaunchedEffect
            val phaseDuration = if (isResting) action.restTime else action.defaultTime

            while (remainingSeconds > 0 && isRunning) {
                delay(1000)
                remainingSeconds--
                totalElapsedSeconds++

                val left = remainingSeconds
                if (left > 0) {
                    // 全程木质秒针；最后 5 秒叠加中文数字；长动作过半提醒一次
                    TrainingFeedback.onSecondElapsed(left)
                    if (!isResting && !halfwayAnnounced &&
                        phaseDuration >= 40 && left == phaseDuration / 2
                    ) {
                        halfwayAnnounced = true
                        TrainingFeedback.onHalfway()
                    }
                }
            }

            // 时间到：先播报阶段变化，再切换状态
            if (isRunning) {
                val willFinish = if (isResting) {
                    currentActionIndex >= actions.size - 1
                } else {
                    action.restTime <= 0 && currentActionIndex >= actions.size - 1
                }
                when {
                    willFinish -> TrainingFeedback.onComplete()
                    isResting -> TrainingFeedback.onRestEnd()
                    action.restTime > 0 -> TrainingFeedback.onEnterRest()
                    else -> TrainingFeedback.onNextAction()
                }
                halfwayAnnounced = false

                if (isResting) {
                    // 休息结束，下一个动作
                    if (currentActionIndex < actions.size - 1) {
                        currentActionIndex++
                        isResting = false
                        remainingSeconds = actions[currentActionIndex].defaultTime
                    } else {
                        isCompleted = true
                        isRunning = false
                    }
                } else {
                    // 动作结束，进入休息
                    val currentActionItem = actions[currentActionIndex]
                    if (currentActionItem.restTime > 0) {
                        isResting = true
                        remainingSeconds = currentActionItem.restTime
                    } else {
                        // 无休息，直接下一个
                        if (currentActionIndex < actions.size - 1) {
                            currentActionIndex++
                            remainingSeconds = actions[currentActionIndex].defaultTime
                        } else {
                            isCompleted = true
                            isRunning = false
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        if (actions.isEmpty()) {
            EmptyState(
                icon = AppIcons.Strength,
                title = "该分组暂无动作",
                subtitle = "请先在「分组」页编辑此分组补充动作"
            )
        } else if (isCompleted) {
            // 训练完成页面
            CompletedContent(
                group = group,
                totalSeconds = totalElapsedSeconds,
                actionCount = actions.size,
                onSaveCheckin = {
                    val result = TrainingResult(
                        groupId = group.id,
                        duration = totalElapsedSeconds,
                        completed = true
                    )
                    onComplete(result)
                },
                onSkip = {
                    val result = TrainingResult(
                        groupId = group.id,
                        duration = totalElapsedSeconds,
                        completed = false
                    )
                    onComplete(result)
                }
            )
        } else {
            // 训练进行中
            val currentAction = actions[currentActionIndex]

            if (showDetailDialog) {
                val detailAction = if (isResting)
                    actions.getOrNull(currentActionIndex + 1) ?: currentAction
                else currentAction
                ActionGuideDialog(
                    action = detailAction,
                    guide = guideFor(detailAction.name)
                        ?: buildFallbackGuide(detailAction.name, detailAction.stepsText),
                    onDismiss = { showDetailDialog = false },
                    showTimingHint = true
                )
            }

            val totalTimeForPhase = if (isResting) currentAction.restTime else currentAction.defaultTime
            val progress = if (totalTimeForPhase > 0) {
                (totalTimeForPhase - remainingSeconds).toFloat() / totalTimeForPhase
            } else 0f

            // 动画值
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 300),
                label = "progress"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppDimens.screenPadding)
            ) {
                // 顶部行：关闭 + 阶段名 + 进度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        AppIcon(
                            imageVector = AppIcons.Close,
                            contentDescription = "退出训练",
                            tint = AppTheme.colors.ink,
                            size = AppDimens.icon
                        )
                    }
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = AppTypeWeight.emphasis,
                        color = AppTheme.colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${currentActionIndex + 1} / ${actions.size}",
                        style = MaterialTheme.typography.caption,
                        color = AppTheme.colors.inkSub,
                        modifier = Modifier.padding(horizontal = AppDimens.s4)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(AppDimens.s2))
                    Text(
                        text = if (isResting) "休息一下" else "当前动作 · ${currentAction.name}",
                        style = MaterialTheme.typography.caption,
                        color = AppTheme.colors.inkMuted
                    )

                    Spacer(modifier = Modifier.height(AppDimens.s3))

                    // 进度环 + 倒计时
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(AppDimens.dialogWidth)
                    ) {
                        // 读取颜色（捕获到 Canvas 外，因为 Canvas 内不是 @Composable 上下文）
                        val progressColor = if (isResting) AppTheme.colors.warning else AppTheme.colors.brandStrong
                        val trackColor = if (isResting) AppTheme.colors.warningSoft else AppTheme.colors.brandSoft

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = AppDimens.s4.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2
                            val topLeft = Offset(
                                (size.width - radius * 2) / 2,
                                (size.height - radius * 2) / 2
                            )

                            drawArc(
                                color = trackColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            drawArc(
                                color = progressColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatTime(remainingSeconds),
                                style = MaterialTheme.typography.h1.copy(fontSize = AppTypeScale.countdown),
                                fontWeight = AppTypeWeight.emphasis,
                                color = when {
                                    remainingSeconds <= 3 -> AppTheme.colors.danger
                                    isResting -> AppTheme.colors.warning
                                    else -> AppTheme.colors.ink
                                }
                            )
                            Text(
                                text = "剩余时间",
                                style = MaterialTheme.typography.caption,
                                color = AppTheme.colors.inkSub
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppDimens.s4))

                    // 当前动作名称
                    AnimatedContent(
                        targetState = if (isResting) "休息" else currentAction.name,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith
                                    fadeOut(animationSpec = tween(200))
                        },
                        label = "actionName"
                    ) { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.h5,
                            textAlign = TextAlign.Center,
                            fontWeight = AppTypeWeight.emphasis,
                            color = AppTheme.colors.ink
                        )
                    }

                    Spacer(modifier = Modifier.height(AppDimens.s1))

                    Text(
                        text = if (isResting)
                            "下一个：${actions.getOrNull(currentActionIndex + 1)?.name ?: "完成"}"
                        // 名称下方为动作描述：取要点文案第一句，短小居中
                        else currentAction.stepsText
                            .substringBefore("。")
                            .substringBefore("；")
                            .substringBefore(";")
                            .ifBlank { "慢速均匀，全程无痛" },
                        style = MaterialTheme.typography.caption,
                        color = AppTheme.colors.inkSub,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = AppDimens.s4),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(AppDimens.s4))

                    // 动作要点卡片：一句话摘要 + 完整图解入口
                    val actionGuide = guideFor(currentAction.name)
                    LiteCard(onClick = { showDetailDialog = true }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.s3)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconBadge(
                                    icon = AppIcons.Info,
                                    contentDescription = null,
                                    tint = AppTheme.colors.brand,
                                    container = AppTheme.colors.brandSoft
                                )
                                Spacer(modifier = Modifier.width(AppDimens.s2))
                                Text(
                                    text = "动作要点",
                                    style = MaterialTheme.typography.subtitle2,
                                    fontWeight = AppTypeWeight.emphasis,
                                    color = AppTheme.colors.ink
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "查看完整图解",
                                    style = MaterialTheme.typography.caption,
                                    color = AppTheme.colors.brand
                                )
                                AppIcon(
                                    imageVector = AppIcons.Chevron,
                                    contentDescription = "查看完整图解",
                                    tint = AppTheme.colors.brand,
                                    size = AppDimens.iconSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(AppDimens.s2))
                            Text(
                                text = actionGuide?.summary ?: currentAction.stepsText.ifBlank {
                                    "保持关节在中立位，速度均匀；出现刺痛或弹响立即停止。"
                                },
                                style = MaterialTheme.typography.body2,
                                color = AppTheme.colors.inkSub
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppDimens.s4))

                    // 休息提示：独立一行，位于要点卡与控制区之间
                    if (!isResting && currentAction.restTime > 0) {
                        Text(
                            text = "完成后休息 ${currentAction.restTime} 秒",
                            style = MaterialTheme.typography.caption,
                            color = AppTheme.colors.inkSub
                        )
                    }

                    // 控制区前留出呼吸间距，避免按钮紧贴提示行
                    Spacer(modifier = Modifier.height(AppDimens.s6))

                    // 控制区：跳过 - 播放/暂停 - 重来（紧跟内容，不沉底）
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.s6),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                TrainingFeedback.onSkip()
                                halfwayAnnounced = false
                                if (isResting) {
                                    if (currentActionIndex < actions.size - 1) {
                                        currentActionIndex++
                                        isResting = false
                                        remainingSeconds = actions[currentActionIndex].defaultTime
                                    } else {
                                        isCompleted = true
                                        isRunning = false
                                    }
                                } else {
                                    if (currentActionIndex < actions.size - 1) {
                                        if (actions[currentActionIndex].restTime > 0) {
                                            isResting = true
                                            remainingSeconds = actions[currentActionIndex].restTime
                                        } else {
                                            currentActionIndex++
                                            remainingSeconds = actions[currentActionIndex].defaultTime
                                        }
                                    } else {
                                        isCompleted = true
                                        isRunning = false
                                    }
                                }
                            },
                            modifier = Modifier.height(AppDimens.touchTarget)
                        ) {
                            Text("跳过", color = AppTheme.colors.inkSub)
                        }

                        // 暂停/开始（圆形主按钮）
                        FloatingActionButton(
                            onClick = {
                                if (isRunning) {
                                    TrainingFeedback.onPause()
                                } else if (hasStarted) {
                                    TrainingFeedback.onResume()
                                } else {
                                    hasStarted = true
                                    TrainingFeedback.onStart()
                                }
                                isRunning = !isRunning
                            },
                            backgroundColor = if (isRunning) AppTheme.colors.brandDeep else AppTheme.colors.brandStrong,
                            modifier = Modifier.size(AppDimens.playButton)
                        ) {
                            if (isRunning) {
                                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.s1)) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = AppDimens.s2, height = AppDimens.s6)
                                            .background(AppTheme.colors.onBrand, AppShapes.chip)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(width = AppDimens.s2, height = AppDimens.s6)
                                            .background(AppTheme.colors.onBrand, AppShapes.chip)
                                    )
                                }
                            } else {
                                AppIcon(
                                    imageVector = AppIcons.Play,
                                    contentDescription = "开始",
                                    tint = AppTheme.colors.onBrand,
                                    size = AppDimens.s8
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                remainingSeconds = if (isResting) {
                                    actions[currentActionIndex].restTime
                                } else {
                                    actions[currentActionIndex].defaultTime
                                }
                                // 确保计时器继续运行
                                TrainingFeedback.onResume()
                                halfwayAnnounced = false
                                isRunning = true
                            },
                            modifier = Modifier.height(AppDimens.touchTarget)
                        ) {
                            Text("重来", color = AppTheme.colors.inkSub)
                        }
                    }

                    Spacer(modifier = Modifier.height(AppDimens.s3))

                    Text(
                        text = "木质秒针 · 开始 / 休息 / 切换 / 完成语音播报",
                        style = MaterialTheme.typography.caption,
                        color = AppTheme.colors.inkMuted
                    )

                    // 剩余空白收尾：内容整体聚拢在上半屏
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun CompletedContent(
    group: GroupItem,
    totalSeconds: Int,
    actionCount: Int,
    onSaveCheckin: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimens.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconBadge(
            icon = AppIcons.Check,
            contentDescription = "完成",
            tint = AppTheme.colors.success,
            container = AppTheme.colors.successSoft,
            size = AppDimens.iconHero
        )

        Spacer(modifier = Modifier.height(AppDimens.s6))

        Text(
            text = "训练完成！",
            style = MaterialTheme.typography.h4
        )

        Spacer(modifier = Modifier.height(AppDimens.s4))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$actionCount",
                    style = MaterialTheme.typography.h3,
                    fontWeight = AppTypeWeight.emphasis,
                    color = AppTheme.colors.brandStrong
                )
                Text(
                    text = "动作数",
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.inkSub
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(totalSeconds),
                    style = MaterialTheme.typography.h3,
                    fontWeight = AppTypeWeight.emphasis,
                    color = AppTheme.colors.brandStrong
                )
                Text(
                    text = "总时长",
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.inkSub
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.s12))

        Button(
            onClick = onSaveCheckin,
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.button
        ) {
            AppIcon(AppIcons.Check, contentDescription = null, size = AppDimens.iconSmall)
            Spacer(modifier = Modifier.width(AppDimens.s2))
            Text("保存打卡")
        }

        Spacer(modifier = Modifier.height(AppDimens.s4))

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.button
        ) {
            Text("暂不打卡")
        }
    }
}
