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
import data.GroupItem
import data.ActionItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 分组屏幕
@Composable
fun GroupScreen() {
    // 当前选中的分组，null表示在列表页面
    var selectedGroup by remember { mutableStateOf<GroupItem?>(null) }
    // 使用局部变量来避免智能转换问题
    val currentSelectedGroup = selectedGroup

    if (currentSelectedGroup == null) {
        // 分组列表页面
        GroupListScreen(onGroupClick = { group ->
            selectedGroup = group
        })
    } else {
        // 分组详情页面
        GroupDetailScreen(
            group = currentSelectedGroup,
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
    val repository = SportTaskRepository()
    val scope = rememberCoroutineScope()

    // 动作列表状态
    var actions by remember { mutableStateOf<List<ActionItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 编辑状态
    var editingAction by remember { mutableStateOf<ActionItem?>(null) }
    var showEditScreen by remember { mutableStateOf(false) }
    var isSorting by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    // 加载动作列表
    suspend fun loadActions() {
        isLoading = true
        val dbActions = repository.getActions(group.id)
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
        isLoading = false
    }

    LaunchedEffect(group.id) {
        loadActions()
    }

    if (showEditScreen) {
        ActionEditScreen(
            groupId = group.id,
            action = editingAction,
            onBackClick = {
                showEditScreen = false
                editingAction = null
            },
            onSaveClick = { action ->
                scope.launch {
                    val now = java.time.LocalDate.now().toString()
                    if (editingAction != null) {
                        // 编辑模式
                        repository.updateAction(
                            id = action.id,
                            groupId = group.id,
                            name = action.name,
                            stepsText = action.stepsText,
                            defaultTime = action.defaultTime.toLong(),
                            restTime = action.restTime.toLong(),
                            orderIndex = action.orderIndex.toLong(),
                            createdAt = now
                        )
                    } else {
                        // 添加模式
                        repository.addAction(
                            groupId = group.id,
                            name = action.name,
                            stepsText = action.stepsText,
                            defaultTime = action.defaultTime.toLong(),
                            restTime = action.restTime.toLong(),
                            orderIndex = action.orderIndex.toLong(),
                            createdAt = now
                        )
                    }
                    loadActions()
                    showEditScreen = false
                    editingAction = null
                }
            }
        )
        return@GroupDetailScreen
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSorting) "调整排序" else group.name) },
                navigationIcon = {
                    if (isSorting) {
                        IconButton(onClick = {
                            isSorting = false
                            // 恢复原始顺序
                            scope.launch {
                                loadActions()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "取消排序"
                            )
                        }
                    } else {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                },
                actions = {
                    if (isSorting) {
                        // 排序模式下的保存按钮
                        if (hasUnsavedChanges) {
                            IconButton(onClick = {
                                scope.launch {
                                    // 保存排序
                                    actions.forEachIndexed { index, action ->
                                        if (action.orderIndex != index + 1) {
                                            repository.updateActionOrder(action.id, (index + 1).toLong())
                                        }
                                    }
                                    hasUnsavedChanges = false
                                    isSorting = false
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "保存排序"
                                )
                            }
                        }
                    } else {
                        // 正常模式
                        IconButton(onClick = {
                            isSorting = true
                            hasUnsavedChanges = false
                        }) {
                            Icon(
                                imageVector = Icons.Filled.List,
                                contentDescription = "排序"
                            )
                        }
                        IconButton(onClick = {
                            editingAction = null
                            showEditScreen = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "添加动作"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingAction = null
                showEditScreen = true
            }) {
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

            // 加载状态
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (actions.isEmpty()) {
                Text(
                    text = "暂无动作，点击 + 添加",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn {
                    items(actions) { action ->
                        val index = actions.indexOf(action)
                        ActionCard(
                            action = action,
                            isSorting = isSorting,
                            canMoveUp = index > 0,
                            canMoveDown = index < actions.size - 1,
                            onEditClick = {
                                editingAction = action
                                showEditScreen = true
                            },
                            onDeleteClick = {
                                scope.launch {
                                    repository.deleteAction(action.id)
                                    loadActions()
                                }
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    val newActions = actions.toMutableList()
                                    val temp = newActions[index - 1]
                                    newActions[index - 1] = newActions[index]
                                    newActions[index] = temp.copy(orderIndex = index)
                                    actions = newActions.mapIndexed { i, a -> a.copy(orderIndex = i + 1) }
                                    hasUnsavedChanges = true
                                }
                            },
                            onMoveDown = {
                                if (index < actions.size - 1) {
                                    val newActions = actions.toMutableList()
                                    val temp = newActions[index + 1]
                                    newActions[index + 1] = newActions[index]
                                    newActions[index] = temp.copy(orderIndex = index + 2)
                                    actions = newActions.mapIndexed { i, a -> a.copy(orderIndex = i + 1) }
                                    hasUnsavedChanges = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

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
fun ActionCard(
    action: ActionItem,
    isSorting: Boolean = false,
    canMoveUp: Boolean = true,
    canMoveDown: Boolean = true,
    onEditClick: (ActionItem) -> Unit = {},
    onDeleteClick: (ActionItem) -> Unit = {},
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = if (isSorting) 4.dp else 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${action.orderIndex}. ${action.name}",
                    style = MaterialTheme.typography.subtitle1
                )
                if (isSorting) {
                    // 排序模式：显示移动按钮
                    Row {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = "上移",
                                tint = if (canMoveUp) Color.Gray else Color.LightGray
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "下移",
                                tint = if (canMoveDown) Color.Gray else Color.LightGray
                            )
                        }
                    }
                } else {
                    // 正常模式：显示编辑/删除按钮
                    Row {
                        IconButton(onClick = { onEditClick(action) }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "编辑",
                                tint = Color.Gray
                            )
                        }
                        IconButton(onClick = { onDeleteClick(action) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "删除",
                                tint = Color.Gray
                            )
                        }
                    }
                }
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

// 动作编辑屏幕
@Composable
fun ActionEditScreen(
    groupId: Long,
    action: ActionItem? = null,
    onBackClick: () -> Unit,
    onSaveClick: (ActionItem) -> Unit
) {
    val isEditing = action != null

    var name by remember { mutableStateOf(action?.name ?: "") }
    var stepsText by remember { mutableStateOf(action?.stepsText ?: "") }
    var defaultTime by remember { mutableStateOf(action?.defaultTime?.toString() ?: "30") }
    var restTime by remember { mutableStateOf(action?.restTime?.toString() ?: "10") }
    var orderIndex by remember { mutableStateOf(action?.orderIndex?.toString() ?: "1") }
    var nameError by remember { mutableStateOf(false) }
    var timeError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val repository = SportTaskRepository()

    // 如果是编辑模式，获取下一个排序索引
    LaunchedEffect(groupId) {
        if (!isEditing) {
            val nextIndex = withContext(Dispatchers.Default) {
                repository.getNextOrderIndex(groupId)
            }
            orderIndex = nextIndex.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑动作" else "添加动作") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 动作名称
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text("动作名称 *") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError
            )

            if (nameError) {
                Text(
                    text = "请输入动作名称",
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption
                )
            }

            // 步骤说明
            OutlinedTextField(
                value = stepsText,
                onValueChange = { stepsText = it },
                label = { Text("步骤说明") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            // 默认时长和休息时长
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = defaultTime,
                    onValueChange = { newValue: String ->
                        defaultTime = newValue.filter { ch: Char -> ch.isDigit() }
                        timeError = false
                    },
                    label = { Text("默认时长(秒) *") },
                    modifier = Modifier.weight(1f),
                    isError = timeError
                )

                OutlinedTextField(
                    value = restTime,
                    onValueChange = { newValue: String ->
                        restTime = newValue.filter { ch: Char -> ch.isDigit() }
                        timeError = false
                    },
                    label = { Text("休息时长(秒) *") },
                    modifier = Modifier.weight(1f),
                    isError = timeError
                )
            }

            // 排序索引
            OutlinedTextField(
                value = orderIndex,
                onValueChange = { newValue: String ->
                    orderIndex = newValue.filter { ch: Char -> ch.isDigit() }
                    timeError = false
                },
                label = { Text("排序索引") },
                modifier = Modifier.fillMaxWidth(),
                isError = timeError
            )

            if (timeError) {
                Text(
                    text = errorMessage.ifEmpty { "请输入有效数字" },
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 保存按钮
            Button(
                onClick = {
                    // 验证
                    nameError = name.isBlank()
                    val defaultTimeLong = defaultTime.toLongOrNull()
                    val restTimeLong = restTime.toLongOrNull()
                    val orderIndexLong = orderIndex.toLongOrNull()

                    when {
                        name.isBlank() -> {
                            errorMessage = "请输入动作名称"
                        }
                        defaultTimeLong == null || defaultTimeLong <= 0 -> {
                            timeError = true
                            errorMessage = "默认时长必须大于0"
                        }
                        restTimeLong == null || restTimeLong < 0 -> {
                            timeError = true
                            errorMessage = "休息时长必须大于等于0"
                        }
                        orderIndexLong == null || orderIndexLong <= 0 -> {
                            timeError = true
                            errorMessage = "排序索引必须大于0"
                        }
                        else -> {
                            val newAction = ActionItem(
                                id = action?.id ?: 0,
                                name = name.trim(),
                                stepsText = stepsText.trim(),
                                defaultTime = defaultTimeLong.toInt(),
                                restTime = restTimeLong.toInt(),
                                orderIndex = orderIndexLong.toInt()
                            )
                            onSaveClick(newAction)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}