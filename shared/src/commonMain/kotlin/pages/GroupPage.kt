package pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import data.SportTaskRepository
import data.GroupItem
import data.ActionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import ui.EmptyState
import ui.FullScreenLoading

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

    // 加载状态
    var isLoading by remember { mutableStateOf(true) }

    suspend fun loadGroupsData() {
        loadGroups(repository) { groups = it; isLoading = false }
    }

    // 初始加载分组
    LaunchedEffect(true) {
        loadGroupsData()
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
        Column(modifier = Modifier
            .padding(innerPadding)
            .padding(8.dp)) {
            when {
                isLoading -> {
                    FullScreenLoading()
                }
                groups.isEmpty() -> {
                    EmptyState(
                        icon = "📂",
                        title = "还没有训练分组",
                        subtitle = "创建一个分组来管理你的训练动作",
                        actionText = "新建分组",
                        onAction = { showAddDialog = true }
                    )
                }
                else -> {
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

// 分组详情页面 — 支持拖拽排序
@Composable
fun GroupDetailScreen(group: GroupItem, onBackClick: () -> Unit) {
    val repository = SportTaskRepository()
    val scope = rememberCoroutineScope()

    var actions by remember { mutableStateOf<List<ActionItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var editingAction by remember { mutableStateOf<ActionItem?>(null) }
    var showEditScreen by remember { mutableStateOf(false) }

    // 拖拽排序状态
    var draggedIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var itemHeights by remember { mutableStateOf<Map<Int, Float>>(emptyMap()) }

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

    fun saveOrder() {
        scope.launch {
            actions.forEachIndexed { index, action ->
                val newOrder = index + 1
                if (action.orderIndex != newOrder) {
                    repository.updateActionOrder(action.id, newOrder.toLong())
                }
            }
            loadActions()
        }
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
                title = { Text(group.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editingAction = null
                        showEditScreen = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "添加动作")
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
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // 提示条
            if (actions.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colors.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "长按 ⠿ 拖拽排序，放手自动保存",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (isLoading) {
                FullScreenLoading()
            } else if (actions.isEmpty()) {
                EmptyState(
                    icon = "💪",
                    title = "暂无动作",
                    subtitle = "点击右下角 + 添加第一个训练动作",
                    actionText = "添加动作",
                    onAction = {
                        editingAction = null
                        showEditScreen = true
                    }
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    actions.forEachIndexed { index, action ->
                        val isThisDragged = index == draggedIndex && isDragging

                        DraggableActionCard(
                            action = action,
                            index = index,
                            isDragging = isThisDragged,
                            dragOffsetY = if (isThisDragged) dragOffset else 0f,
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
                            onDragStart = {
                                draggedIndex = index
                                dragOffset = 0f
                                isDragging = true
                            },
                            onDrag = { deltaY ->
                                dragOffset += deltaY
                                val currentIndex = draggedIndex
                                // 交换逻辑：跨越一半高度时触发
                                actions.getOrNull(currentIndex)?.let { currentAction ->
                                    val height = itemHeights[currentIndex] ?: 100f
                                    val threshold = height * 0.5f

                                    if (dragOffset < -threshold && currentIndex > 0) {
                                        val list = actions.toMutableList()
                                        val temp = list[currentIndex]
                                        list[currentIndex] = list[currentIndex - 1]
                                        list[currentIndex - 1] = temp
                                        actions = list
                                        draggedIndex = currentIndex - 1
                                        dragOffset += threshold
                                    }
                                    if (dragOffset > threshold && currentIndex < actions.size - 1) {
                                        val list = actions.toMutableList()
                                        val temp = list[currentIndex]
                                        list[currentIndex] = list[currentIndex + 1]
                                        list[currentIndex + 1] = temp
                                        actions = list
                                        draggedIndex = currentIndex + 1
                                        dragOffset -= threshold
                                    }
                                }
                            },
                            onDragEnd = {
                                if (draggedIndex >= 0) {
                                    isDragging = false
                                    dragOffset = 0f
                                    saveOrder()
                                }
                            },
                            onHeightMeasured = { h -> itemHeights = itemHeights + (index to h) }
                        )
                    }

                    Spacer(Modifier.height(80.dp))
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

// 拖拽卡片组件 — 支持长按拖拽排序
@Composable
fun DraggableActionCard(
    action: ActionItem,
    index: Int,
    isDragging: Boolean,
    dragOffsetY: Float,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onHeightMeasured: (Float) -> Unit
) {
    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 8f else 2f,
        animationSpec = tween(150),
        label = "elevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else 0f
                scaleX = if (isDragging) 1.03f else 1f
                scaleY = if (isDragging) 1.03f else 1f
            }
            .onGloballyPositioned { coordinates ->
                onHeightMeasured(coordinates.size.height.toFloat())
            },
        elevation = elevation.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 拖拽手柄
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⠿",
                    style = MaterialTheme.typography.body1,
                    color = if (isDragging) MaterialTheme.colors.primary else Color.Gray
                )
            }

            // 序号
            Text(
                text = "${index + 1}.",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(32.dp)
            )

            // 名称和详情
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.name,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "${action.defaultTime}秒 / 休息${action.restTime}秒",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }

            // 操作按钮
            if (!isDragging) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
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