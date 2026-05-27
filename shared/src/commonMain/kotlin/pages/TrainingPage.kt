package pages

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import ui.FullScreenLoading
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
    onTrainingComplete: (() -> Unit)? = null
) {
    val repository = SportTaskRepository
    var groups by remember { mutableStateOf<List<GroupItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedGroup by remember { mutableStateOf<GroupItem?>(null) }
    var showPrepareScreen by remember { mutableStateOf(false) }
    var showExecuteScreen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val actionGroups = repository.getGroups()
        groups = actionGroups.map { group ->
            GroupItem(
                id = group.id,
                name = group.name,
                actionCount = repository.countActions(group.id).toInt()
            )
        }
        isLoading = false
    }

    if (showExecuteScreen && selectedGroup != null) {
        TrainingExecuteScreen(
            group = selectedGroup!!,
            onBackClick = {
                showExecuteScreen = false
                selectedGroup = null
            },
            onComplete = { result ->
                // 🔴 Fix: 保存打卡记录到数据库
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择训练") }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            FullScreenLoading(modifier = Modifier.padding(innerPadding))
        } else if (groups.isEmpty()) {
            EmptyState(
                icon = "🏋️",
                title = "暂无训练分组",
                subtitle = "请先在「分组」页面创建训练分组和动作",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups) { group ->
                    TrainingGroupCard(
                        group = group,
                        onClick = {
                            selectedGroup = group
                            showPrepareScreen = true
                        }
                    )
                }
            }
        }
    }
}

// 训练分组卡片
@Composable
fun TrainingGroupCard(group: GroupItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = "${group.actionCount} 个动作",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "开始训练",
                tint = MaterialTheme.colors.primary
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
    var actions by remember { mutableStateOf<List<ActionItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalDuration by remember { mutableStateOf(0) }

    LaunchedEffect(group.id) {
        val dbActions = withContext(Dispatchers.Default) {
            repository.getActions(group.id)
        }
        actions = dbActions.map { action ->
            ActionItem(
                id = action.id,
                name = action.name,
                stepsText = action.steps_text ?: "",
                defaultTime = action.default_time.toInt(),
                restTime = action.rest_time.toInt(),
                orderIndex = action.order_index.toInt()
            )
        }
        // 总时长：每个动作的时长 + 除最后一个动作外的休息时长
        totalDuration = if (actions.isEmpty()) 0 else {
            actions.sumOf { it.defaultTime + it.restTime } - actions.last().restTime
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("准备训练") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = onStartClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    enabled = actions.isNotEmpty()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始训练")
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            FullScreenLoading(modifier = Modifier.padding(innerPadding))
        } else if (actions.isEmpty()) {
            EmptyState(
                icon = "💪",
                title = "该分组暂无动作",
                subtitle = "请先在分组详情中添加训练动作",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 训练统计
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.h6
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${actions.size}",
                                    style = MaterialTheme.typography.h5
                                )
                                Text(
                                    text = "动作数",
                                    style = MaterialTheme.typography.caption,
                                    color = Color.Gray
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${totalDuration / 60}:${(totalDuration % 60).toString().padStart(2, '0')}",
                                    style = MaterialTheme.typography.h5
                                )
                                Text(
                                    text = "预计时长",
                                    style = MaterialTheme.typography.caption,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // 动作列表
                Text(
                    text = "动作序列",
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(actions) { action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${action.orderIndex}.",
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.width(24.dp)
                            )
                            Text(
                                text = action.name,
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${action.defaultTime}s",
                                style = MaterialTheme.typography.caption,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// 训练执行屏幕
@Composable
fun TrainingExecuteScreen(
    group: GroupItem,
    onBackClick: () -> Unit,
    onComplete: (TrainingResult) -> Unit,
    onTrainingComplete: (() -> Unit)? = null
) {
    val repository = SportTaskRepository
    var actions by remember { mutableStateOf<List<ActionItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 训练状态
    var currentActionIndex by remember { mutableStateOf(0) }
    var isResting by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(0) }
    var totalElapsedSeconds by remember { mutableStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }

    // 计时器
    val scope = rememberCoroutineScope()

    LaunchedEffect(group.id) {
        val dbActions = withContext(Dispatchers.Default) {
            repository.getActions(group.id)
        }
        actions = dbActions.map { action ->
            ActionItem(
                id = action.id,
                name = action.name,
                stepsText = action.steps_text ?: "",
                defaultTime = action.default_time.toInt(),
                restTime = action.rest_time.toInt(),
                orderIndex = action.order_index.toInt()
            )
        }
        if (actions.isNotEmpty()) {
            remainingSeconds = actions[0].defaultTime
        }
        isLoading = false
    }

    // 计时器逻辑
    LaunchedEffect(isRunning, currentActionIndex, isResting) {
        if (isRunning && !isCompleted) {
            val action = actions.getOrNull(currentActionIndex) ?: return@LaunchedEffect
            val totalTime = if (isResting) action.restTime else action.defaultTime

            while (remainingSeconds > 0 && isRunning) {
                delay(1000)
                
                // 倒计时 3 秒时播放提示音和震动
                if (remainingSeconds <= 3) {
                    playCountdownSound()
                    vibrateShort()
                }
                
                remainingSeconds--
                totalElapsedSeconds++
            }

            // 时间到，切换状态
            if (isRunning) {
                playEndSound()
                vibrateMedium()
                
                if (isResting) {
                    // 休息结束，下一个动作
                    if (currentActionIndex < actions.size - 1) {
                        currentActionIndex++
                        isResting = false
                        remainingSeconds = actions[currentActionIndex].defaultTime
                    } else {
                        // 训练完成
                        isCompleted = true
                        isRunning = false
                        vibrateLong()
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
                            // 训练完成
                            isCompleted = true
                            isRunning = false
                            vibrateLong()
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCompleted) "训练完成" else group.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "退出"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            FullScreenLoading(modifier = Modifier.padding(innerPadding))
        } else if (actions.isEmpty()) {
            EmptyState(
                icon = "💪",
                title = "该分组暂无动作",
                subtitle = "请先在分组详情中添加训练动作",
                modifier = Modifier.padding(innerPadding)
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
                    // onTrainingComplete 由 onComplete 中调用
                },
                onSkip = {
                    val result = TrainingResult(
                        groupId = group.id,
                        duration = totalElapsedSeconds,
                        completed = false
                    )
                    onComplete(result)
                    // onTrainingComplete 由 onComplete 中调用
                }
            )
        } else {
            // 训练进行中
            val currentAction = actions[currentActionIndex]
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
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 状态标签
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 0.dp,
                    backgroundColor = if (isResting)
                        MaterialTheme.colors.secondary.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colors.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isResting) "🧘 休息时间" else "🏋️ 动作 ${currentActionIndex + 1}/${actions.size}",
                        style = MaterialTheme.typography.subtitle1,
                        color = if (isResting) MaterialTheme.colors.secondary else MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 进度环 + 倒计时 (居中区域)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(280.dp)
                ) {
                    // 读取颜色（捕获到 Canvas 外，因为 Canvas 内不是 @Composable 上下文）
                    val progressColor = if (isResting) MaterialTheme.colors.secondary else MaterialTheme.colors.primary

                    // 背景圆环
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 16.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val topLeft = Offset(
                            (size.width - radius * 2) / 2,
                            (size.height - radius * 2) / 2
                        )

                        // 背景弧
                        drawArc(
                            color = if (isResting) Color(0xFFE0E0E0) else Color(0xFFBBDEFB),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 进度弧
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

                    // 倒计时数字
                    Text(
                        text = formatTime(remainingSeconds),
                        style = MaterialTheme.typography.h1.copy(fontSize = 64.sp),
                        fontWeight = FontWeight.Bold,
                        color = when {
                            remainingSeconds <= 3 -> MaterialTheme.colors.error
                            isResting -> MaterialTheme.colors.secondary
                            else -> MaterialTheme.colors.primary
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 当前动作名称 (带动画切换)
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
                        fontWeight = FontWeight.Medium,
                        color = if (isResting) MaterialTheme.colors.secondary else Color.Unspecified
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 下一个提示
                val nextInfo = if (isResting) {
                    if (currentActionIndex < actions.size - 1) {
                        "下一个: ${actions[currentActionIndex + 1].name}"
                    } else {
                        "🎉 训练即将完成"
                    }
                } else {
                    if (currentAction.restTime > 0) {
                        "做完休息 ${currentAction.restTime}秒 →"
                    } else if (currentActionIndex < actions.size - 1) {
                        "下一个: ${actions[currentActionIndex + 1].name}"
                    } else {
                        "🎉 最后一个动作！加油！"
                    }
                }
                Text(
                    text = nextInfo,
                    style = MaterialTheme.typography.body1,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.weight(0.5f))

                // 控制按钮
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // 跳过
                        Button(
                            onClick = {
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
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.surface
                            )
                        ) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("跳过", color = Color.Gray)
                        }

                        // 暂停/开始 (大按钮)
                        FloatingActionButton(
                            onClick = {
                                if (!isRunning) {
                                    playStartSound()
                                }
                                isRunning = !isRunning
                            },
                            backgroundColor = if (isRunning) MaterialTheme.colors.error else MaterialTheme.colors.primary,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Text(
                                text = if (isRunning) "⏸️" else "▶️",
                                style = MaterialTheme.typography.h4,
                                color = Color.White
                            )
                        }

                        // 重复
                        Button(
                            onClick = {
                                remainingSeconds = if (isResting) {
                                    actions[currentActionIndex].restTime
                                } else {
                                    actions[currentActionIndex].defaultTime
                                }
                                // 🔧 确保计时器继续运行
                                if (!isRunning) {
                                    playStartSound()
                                }
                                isRunning = true
                            },
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.surface
                            )
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重来", color = Color.Gray)
                        }
                    }
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "完成",
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "训练完成！",
            style = MaterialTheme.typography.h4
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$actionCount",
                    style = MaterialTheme.typography.h5
                )
                Text(
                    text = "动作数",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(totalSeconds),
                    style = MaterialTheme.typography.h5
                )
                Text(
                    text = "总时长",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSaveCheckin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("保存打卡")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("暂不打卡")
        }
    }
}
