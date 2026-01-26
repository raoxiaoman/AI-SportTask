package pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.SportTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

// 训练屏幕
@Composable
fun TrainingScreen() {
    val repository = SportTaskRepository()
    var groups by remember { mutableStateOf<List<GroupItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedGroup by remember { mutableStateOf<GroupItem?>(null) }
    var showPrepareScreen by remember { mutableStateOf(false) }
    var showExecuteScreen by remember { mutableStateOf(false) }

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "暂无训练分组",
                        style = MaterialTheme.typography.h6,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请先创建分组和动作",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
            }
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
    val repository = SportTaskRepository()
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
        totalDuration = actions.sumOf { it.defaultTime + it.restTime }
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
                        .padding(16.dp),
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (actions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "该分组暂无动作",
                    style = MaterialTheme.typography.body1,
                    color = Color.Gray
                )
            }
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
    onComplete: (TrainingResult) -> Unit
) {
    val repository = SportTaskRepository()
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
                remainingSeconds--
                totalElapsedSeconds++
            }

            // 时间到，切换状态
            if (isRunning) {
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
                    }
                } else {
                    // 动作结束，进入休息
                    val action = actions[currentActionIndex]
                    if (action.restTime > 0) {
                        isResting = true
                        remainingSeconds = action.restTime
                    } else {
                        // 无休息，直接下一个
                        if (currentActionIndex < actions.size - 1) {
                            currentActionIndex++
                            remainingSeconds = actions[currentActionIndex].defaultTime
                        } else {
                            // 训练完成
                            isCompleted = true
                            isRunning = false
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (actions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("该分组暂无动作")
            }
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
            val progress = if (isResting) {
                (currentAction.restTime - remainingSeconds).toFloat() / currentAction.restTime
            } else {
                (currentAction.defaultTime - remainingSeconds).toFloat() / currentAction.defaultTime
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 进度指示
                Text(
                    text = if (isResting) "休息时间" else "动作 ${currentActionIndex + 1}/${actions.size}",
                    style = MaterialTheme.typography.subtitle1,
                    color = if (isResting) MaterialTheme.colors.secondary else MaterialTheme.colors.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 进度条
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 当前动作名称
                Text(
                    text = if (isResting) "休息" else currentAction.name,
                    style = MaterialTheme.typography.h4,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // 倒计时
                Text(
                    text = formatTime(remainingSeconds),
                    style = MaterialTheme.typography.h2,
                    color = if (remainingSeconds <= 5) MaterialTheme.colors.error else MaterialTheme.colors.primary
                )

                Spacer(modifier = Modifier.height(48.dp))

                // 控制按钮
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 跳过
                    FloatingActionButton(
                        onClick = {
                            if (isResting) {
                                // 跳过休息，直接下一个动作
                                if (currentActionIndex < actions.size - 1) {
                                    currentActionIndex++
                                    isResting = false
                                    remainingSeconds = actions[currentActionIndex].defaultTime
                                } else {
                                    isCompleted = true
                                    isRunning = false
                                }
                            } else {
                                // 跳过当前动作
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
                        backgroundColor = MaterialTheme.colors.secondary
                    ) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "跳过")
                    }

                    // 暂停/开始
                    FloatingActionButton(
                        onClick = {
                            isRunning = !isRunning
                        },
                        backgroundColor = if (isRunning) MaterialTheme.colors.error else MaterialTheme.colors.primary
                    ) {
                        Icon(
                            if (isRunning) Icons.Filled.Close else Icons.Filled.PlayArrow,
                            contentDescription = if (isRunning) "暂停" else "开始"
                        )
                    }

                    // 重复
                    FloatingActionButton(
                        onClick = {
                            remainingSeconds = if (isResting) {
                                actions[currentActionIndex].restTime
                            } else {
                                actions[currentActionIndex].defaultTime
                            }
                        },
                        backgroundColor = MaterialTheme.colors.secondary
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "重复")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 下一个提示
                val nextInfo = if (isResting) {
                    if (currentActionIndex < actions.size - 1) {
                        "下一个: ${actions[currentActionIndex + 1].name}"
                    } else {
                        "训练即将完成"
                    }
                } else {
                    if (currentAction.restTime > 0) {
                        "休息: ${currentAction.restTime}秒"
                    } else if (currentActionIndex < actions.size - 1) {
                        "下一个: ${actions[currentActionIndex + 1].name}"
                    } else {
                        "训练即将完成"
                    }
                }
                Text(
                    text = nextInfo,
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
            }
        }
    }
}

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

// 训练结果数据类
data class TrainingResult(
    val groupId: Long,
    val duration: Int,
    val completed: Boolean
)

// 格式化时间
private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
