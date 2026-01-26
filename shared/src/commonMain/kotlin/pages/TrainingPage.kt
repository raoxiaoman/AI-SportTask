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

    if (showPrepareScreen && selectedGroup != null) {
        TrainingPrepareScreen(
            group = selectedGroup!!,
            onBackClick = {
                showPrepareScreen = false
                selectedGroup = null
            },
            onStartClick = {
                showPrepareScreen = false
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
