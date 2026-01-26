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
import androidx.compose.ui.unit.dp
import data.SportTaskRepository
import kotlinx.coroutines.launch

// 分组屏幕
@Composable
fun GroupScreen() {
    // 当前选中的分组，null表示在列表页面
    var selectedGroup by remember { mutableStateOf<GroupItem?>(null) }

    if (selectedGroup == null) {
        // 分组列表页面
        GroupListScreen(onGroupClick = { group ->
            selectedGroup = group
        })
    } else {
        // 分组详情页面
        val group = selectedGroup!!
        GroupDetailScreen(
            group = group,
            onBackClick = {
                selectedGroup = null
            }
        )
    }
}

// 分组列表页面
@Composable
fun GroupListScreen(onGroupClick: (GroupItem) -> Unit) {
    // 控制是否显示添加分组对话框
    var showAddDialog by remember { mutableStateOf(false) }
    // 输入的分组名称
    var groupName by remember { mutableStateOf("") }

    // 控制是否显示编辑分组对话框
    var showEditDialog by remember { mutableStateOf(false) }
    // 当前正在编辑的分组
    var currentEditingGroup by remember { mutableStateOf<GroupItem?>(null) }
    // 编辑的分组名称
    var editGroupName by remember { mutableStateOf("") }

    // 控制是否显示删除分组对话框
    var showDeleteDialog by remember { mutableStateOf(false) }
    // 当前正在删除的分组
    var currentDeletingGroup by remember { mutableStateOf<GroupItem?>(null) }

    // 从数据库获取分组
    val repository = SportTaskRepository()
    val scope = rememberCoroutineScope()

    // 分组列表状态
    var groups by remember { mutableStateOf<List<GroupItem>>(emptyList()) }

    // 初始加载分组
    LaunchedEffect(true) {
        loadGroups(repository) { groups = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("分组管理") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
        }
    ) { innerPadding ->
        // 分组列表
        Column(modifier = Modifier
            .padding(innerPadding)
            .padding(8.dp)) {
            // 列表
            LazyColumn {
                items(groups) { group ->
                    GroupCard(
                        group = group,
                        onClick = { onGroupClick(group) },
                        onEditClick = {
                            currentEditingGroup = group
                            editGroupName = group.name
                            showEditDialog = true
                        },
                        onDeleteClick = {
                            currentDeletingGroup = group
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        // 删除分组对话框
        if (showDeleteDialog && currentDeletingGroup != null) {
            val deletingGroup = currentDeletingGroup!!
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除分组") },
                text = {
                    Text("确定要删除分组 \"${deletingGroup.name}\" 吗？")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val groupToDelete = currentDeletingGroup ?: return@launch
                                // 删除数据库中的分组
                                repository.deleteGroup(groupToDelete.id)

                                // 重新加载分组列表
                                loadGroups(repository) { groups = it }

                                // 重置状态并关闭对话框
                                currentDeletingGroup = null
                                showDeleteDialog = false
                            }
                        }
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        currentDeletingGroup = null
                        showDeleteDialog = false
                    }) {
                        Text("取消")
                    }
                }
            )
        }

        // 编辑分组对话框
        if (showEditDialog && currentEditingGroup != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("编辑分组") },
                text = {
                    Column {
                        Text("请输入新的分组名称：")
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = editGroupName,
                            onValueChange = { editGroupName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("分组名称") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editGroupName.isNotBlank()) {
                                scope.launch {
                                    val groupToEdit = currentEditingGroup ?: return@launch
                                    // 更新数据库
                                    repository.updateGroupName(groupToEdit.id, editGroupName)

                                    // 重新加载分组列表
                                    loadGroups(repository) { groups = it }
                                }

                                // 重置表单并关闭对话框
                                editGroupName = ""
                                currentEditingGroup = null
                                showEditDialog = false
                            }
                        }
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        editGroupName = ""
                        currentEditingGroup = null
                        showEditDialog = false
                    }) {
                        Text("取消")
                    }
                }
            )
        }

        // 添加分组对话框
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("创建新分组") },
                text = {
                    Column {
                        Text("请输入分组名称：")
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("分组名称") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // 创建分组
                            if (groupName.isNotBlank()) {
                                scope.launch {
                                    // 添加到数据库
                                    val now = java.time.LocalDate.now().toString()
                                    repository.addGroup(groupName, now)

                                    // 重新加载分组列表
                                    loadGroups(repository) { groups = it }
                                }

                                // 重置表单并关闭对话框
                                groupName = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("创建")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        groupName = ""
                        showAddDialog = false
                    }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

// 分组详情页面
@Composable
fun GroupDetailScreen(group: GroupItem, onBackClick: () -> Unit) {
    // 模拟动作数据
    val actions = listOf(
        ActionItem("俯卧撑", "1. 双手与肩同宽\n2. 身体保持直线", 45, 15, 1),
        ActionItem("哑铃划船", "1. 单膝跪地\n2. 背部保持平直", 60, 20, 2),
        ActionItem("平板支撑", "1. 肘部支撑地面\n2. 身体保持一条直线", 60, 30, 3)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* 编辑 */ }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "编辑"
                        )
                    }
                    IconButton(onClick = { /* 删除 */ }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "删除"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* 添加动作 */ }) {
                Text("+")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // 开始训练按钮
            Button(
                onClick = { /* 开始训练 */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("开始训练")
            }

            // 动作列表
            Text(
                text = "动作列表",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn {
                items(actions) { action ->
                    ActionCard(action = action)
                }
            }
        }
    }
}

// 分组数据类
data class GroupItem(val id: Long, val name: String, val actionCount: Int)

// 动作数据类
data class ActionItem(
    val name: String,
    val stepsText: String,
    val defaultTime: Int,
    val restTime: Int,
    val orderIndex: Int
)

// 分组卡片组件
@Composable
fun GroupCard(group: GroupItem, onClick: () -> Unit, onEditClick: (GroupItem) -> Unit, onDeleteClick: (GroupItem) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)) {
                Text(text = group.name, style = MaterialTheme.typography.h6)
                Text(
                    text = "${group.actionCount} 个动作",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
            }
            IconButton(onClick = { onEditClick(group) }) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "编辑",
                    tint = Color.Gray
                )
            }
            IconButton(onClick = { onDeleteClick(group) }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = Color.Gray
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "详情",
                tint = Color.Gray
            )
        }
    }
}

// 动作卡片组件
@Composable
fun ActionCard(action: ActionItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = action.name, style = MaterialTheme.typography.subtitle1)
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "展开详情",
                    tint = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "默认时长: ${action.defaultTime}秒", style = MaterialTheme.typography.body2)
            Text(text = "休息时间: ${action.restTime}秒", style = MaterialTheme.typography.body2)
            if (action.stepsText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "步骤:", style = MaterialTheme.typography.body2)
                Text(text = action.stepsText, style = MaterialTheme.typography.body2)
            }
        }
    }
}

// 加载分组列表的辅助函数
private suspend fun loadGroups(
    repository: SportTaskRepository,
    onResult: (List<GroupItem>) -> Unit
) {
    val actionGroups = repository.getGroups()
    onResult(actionGroups.map { group ->
        GroupItem(
            id = group.id,
            name = group.name,
            actionCount = repository.countActions(group.id).toInt()
        )
    })
}