package pages
import ui.AppConfirmDialog
import ui.AppDialog
import ui.AppDialogTextField
import ui.AppIcon
import ui.AppTextField
import ui.AppIcons
import ui.theme.*


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
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
import ui.IconBadge
import ui.LiteCard
import ui.PageHeader
import ui.ActionGuideDialog
import ui.ExerciseGuide
import ui.PlatformBackHandler
import ui.buildFallbackGuide
import ui.guideFor

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
    val repository = SportTaskRepository
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        when {
            isLoading -> {
                FullScreenLoading()
            }
            groups.isEmpty() -> {
                Column {
                    GroupListHeader(onAddClick = { showAddDialog = true })
                    EmptyState(
                        icon = AppIcons.Folder,
                        title = "还没有训练分组",
                        subtitle = "创建一个分组来管理你的训练动作",
                        actionText = "新建分组",
                        onAction = { showAddDialog = true }
                    )
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(AppDimens.s2)
                ) {
                    item { GroupListHeader(onAddClick = { showAddDialog = true }) }
                    items(groups) { group ->
                        GroupRow(
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
                    item { Spacer(modifier = Modifier.height(AppDimens.s8)) }
                }
            }
        }

        // 删除分组对话框
        if (showDeleteDialog && currentDeletingGroup != null) {
            val deletingGroup = currentDeletingGroup!!
            AppConfirmDialog(
                title = "删除分组",
                message = "确定要删除分组「${deletingGroup.name}」吗？分组下的动作也会一并删除，此操作无法撤销。",
                onDismissRequest = {
                    currentDeletingGroup = null
                    showDeleteDialog = false
                },
                onConfirm = {
                    scope.launch {
                        val groupToDelete = currentDeletingGroup ?: return@launch
                        repository.deleteGroup(groupToDelete.id)
                        loadGroups(repository) { groups = it }
                        currentDeletingGroup = null
                        showDeleteDialog = false
                    }
                },
                confirmLabel = "删除",
                danger = true
            )
        }

        // 编辑分组对话框
        if (showEditDialog && currentEditingGroup != null) {
            val closeEdit = {
                editGroupName = ""
                currentEditingGroup = null
                showEditDialog = false
            }
            AppDialog(
                onDismissRequest = closeEdit,
                title = "编辑分组",
                confirmLabel = "保存",
                confirmEnabled = editGroupName.isNotBlank(),
                dismissLabel = "取消",
                onDismiss = closeEdit,
                onConfirm = {
                    if (editGroupName.isNotBlank()) {
                        scope.launch {
                            val groupToEdit = currentEditingGroup ?: return@launch
                            repository.updateGroupName(groupToEdit.id, editGroupName)
                            loadGroups(repository) { groups = it }
                        }
                        closeEdit()
                    }
                }
            ) {
                Text(
                    text = "请输入新的分组名称：",
                    style = MaterialTheme.typography.body1,
                    color = AppTheme.colors.inkSub
                )
                Spacer(modifier = Modifier.height(AppDimens.s2))
                AppDialogTextField(
                    value = editGroupName,
                    onValueChange = { editGroupName = it },
                    label = "分组名称",
                    singleLine = true
                )
            }
        }

        // 添加分组对话框
        if (showAddDialog) {
            val closeAdd = {
                groupName = ""
                showAddDialog = false
            }
            AppDialog(
                onDismissRequest = closeAdd,
                title = "创建新分组",
                confirmLabel = "创建",
                confirmEnabled = groupName.isNotBlank(),
                dismissLabel = "取消",
                onDismiss = closeAdd,
                onConfirm = {
                    if (groupName.isNotBlank()) {
                        scope.launch {
                            repository.addGroup(groupName, todayDateString())
                            loadGroups(repository) { groups = it }
                        }
                        closeAdd()
                    }
                }
            ) {
                Text(
                    text = "请输入分组名称：",
                    style = MaterialTheme.typography.body1,
                    color = AppTheme.colors.inkSub
                )
                Spacer(modifier = Modifier.height(AppDimens.s2))
                AppDialogTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = "分组名称",
                    singleLine = true
                )
            }
        }
    }
}

// 分组详情页面 — 支持拖拽排序
@Composable
fun GroupDetailScreen(group: GroupItem, onBackClick: () -> Unit) {
    val repository = SportTaskRepository
    val scope = rememberCoroutineScope()

    var actions by remember { mutableStateOf<List<ActionItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var editingAction by remember { mutableStateOf<ActionItem?>(null) }
    var showEditScreen by remember { mutableStateOf(false) }

    // 查看动作详情（图解）
    var guideAction by remember { mutableStateOf<ActionItem?>(null) }

    // 物理返回：详情弹窗打开时先关弹窗（Dialog 自带），否则返回分组列表
    PlatformBackHandler(enabled = !showEditScreen) { onBackClick() }

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
                    val now = todayDateString()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        // 头部行：返回 + 分组名 + 添加动作
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppDimens.s1,
                    vertical = AppDimens.s2
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                AppIcon(
                    imageVector = AppIcons.Back,
                    contentDescription = "返回",
                    tint = AppTheme.colors.ink
                )
            }
            Text(
                text = group.name,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = AppTypeWeight.emphasis,
                color = AppTheme.colors.ink,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(AppDimens.touchTarget)
                    .clickable {
                        editingAction = null
                        showEditScreen = true
                    },
                contentAlignment = Alignment.Center
            ) {
                IconBadge(
                    icon = AppIcons.Add,
                    contentDescription = "添加动作"
                )
            }
            Spacer(modifier = Modifier.width(AppDimens.s3))
        }

        // 提示文字
        if (actions.isNotEmpty()) {
            Text(
                text = "长按拖拽把手排序，放手自动保存",
                style = MaterialTheme.typography.caption,
                color = AppTheme.colors.inkMuted,
                modifier = Modifier.padding(horizontal = AppDimens.s4, vertical = AppDimens.s2)
            )
        }

        if (isLoading) {
            FullScreenLoading()
        } else if (actions.isEmpty()) {
            EmptyState(
                icon = AppIcons.Strength,
                title = "暂无动作",
                subtitle = "点击右上角 + 添加第一个训练动作",
                actionText = "添加动作",
                onAction = {
                    editingAction = null
                    showEditScreen = true
                }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppDimens.s2)
            ) {
                    actions.forEachIndexed { index, action ->
                        val isThisDragged = index == draggedIndex && isDragging

                        DraggableActionCard(
                            action = action,
                            index = index,
                            isDragging = isThisDragged,
                            dragOffsetY = if (isThisDragged) dragOffset else 0f,
                            onGuideClick = { guideAction = action },
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

                    Spacer(Modifier.height(AppDimens.logoSize))
                }
        }
    }

    // 动作详情（图解）弹窗
    guideAction?.let { action ->
        ActionGuideDialog(
            action = action,
            guide = guideFor(action.name) ?: buildFallbackGuide(action.name, action.stepsText),
            onDismiss = { guideAction = null }
        )
    }
}

// 分组页头：标题 + 新建入口（替代原 FAB）
@Composable
fun GroupListHeader(onAddClick: () -> Unit) {
    PageHeader(
        title = "训练分组",
        trailing = {
            IconBadge(
                icon = AppIcons.Add,
                contentDescription = "新建分组",
                // 直接可点击，规格与其他页页头徽标一致，不撑高页头行
                modifier = Modifier.clickable(onClick = onAddClick)
            )
        }
    )
}

// 分组行卡：图标容器 + 名称/副标题 + 编辑/删除 + 箭头
@Composable
fun GroupRow(
    group: GroupItem,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
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
            Box(
                modifier = Modifier
                    .size(AppDimens.touchTarget)
                    .clickable(onClick = onEditClick),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    imageVector = AppIcons.Edit,
                    contentDescription = "编辑",
                    tint = AppTheme.colors.inkMuted,
                    size = AppDimens.iconSmall
                )
            }
            Box(
                modifier = Modifier
                    .size(AppDimens.touchTarget)
                    .clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    imageVector = AppIcons.Delete,
                    contentDescription = "删除",
                    tint = AppTheme.colors.inkMuted,
                    size = AppDimens.iconSmall
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

// 拖拽卡片组件 — 支持长按拖拽排序
@Composable
fun DraggableActionCard(
    action: ActionItem,
    index: Int,
    isDragging: Boolean,
    dragOffsetY: Float,
    onGuideClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onHeightMeasured: (Float) -> Unit
) {
    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 8f else 0f,
        animationSpec = tween(150),
        label = "elevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding, vertical = AppDimens.s1)
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else 0f
                scaleX = if (isDragging) 1.03f else 1f
                scaleY = if (isDragging) 1.03f else 1f
            }
            .onGloballyPositioned { coordinates ->
                onHeightMeasured(coordinates.size.height.toFloat())
            },
        shape = AppShapes.chip,
        backgroundColor = AppTheme.colors.surface,
        elevation = elevation.dp,
        border = BorderStroke(AppDimens.divider, AppTheme.colors.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppDimens.s1, end = AppDimens.s2, top = AppDimens.s2, bottom = AppDimens.s2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 拖拽手柄
            Box(
                modifier = Modifier
                    .size(AppDimens.s10)
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
                AppIcon(
                    imageVector = AppIcons.DragHandle,
                    contentDescription = "拖拽排序",
                    size = AppDimens.iconSmall,
                    tint = if (isDragging) AppTheme.colors.brandStrong else AppTheme.colors.inkSub
                )
            }

            // 序号
            Text(
                text = "${index + 1}.",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.primary,
                fontWeight = AppTypeWeight.medium,
                modifier = Modifier.width(AppDimens.s8)
            )

            // 名称和详情
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.name,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = if (isDragging) AppTypeWeight.emphasis else AppTypeWeight.regular
                )
                Text(
                    text = "${action.defaultTime}秒 / 休息${action.restTime}秒",
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.inkSub
                )
                if (action.stepsText.isNotBlank()) {
                    Text(
                        text = action.stepsText,
                        style = MaterialTheme.typography.caption,
                        color = AppTheme.colors.inkSub,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 操作按钮
            if (!isDragging) {
                IconButton(onClick = onGuideClick, modifier = Modifier.size(AppDimens.s8)) {
                    Icon(AppIcons.Book, contentDescription = "查看图解", tint = AppTheme.colors.brandStrong, modifier = Modifier.size(AppDimens.s4))
                }
                IconButton(onClick = onEditClick, modifier = Modifier.size(AppDimens.s8)) {
                    Icon(AppIcons.Edit, contentDescription = "编辑", tint = AppTheme.colors.inkSub, modifier = Modifier.size(AppDimens.s4))
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(AppDimens.s8)) {
                    Icon(AppIcons.Delete, contentDescription = "删除", tint = AppTheme.colors.inkSub, modifier = Modifier.size(AppDimens.s4))
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

    val repository = SportTaskRepository

    // 物理返回 = 返回分组详情，不再退出 App
    PlatformBackHandler(enabled = true) { onBackClick() }

    // 如果是编辑模式，获取下一个排序索引
    LaunchedEffect(groupId) {
        if (!isEditing) {
            val nextIndex = withContext(Dispatchers.Default) {
                repository.getNextOrderIndex(groupId)
            }
            orderIndex = nextIndex.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(horizontal = AppDimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimens.s4)
    ) {
        // 头部行：返回 + 标题（外层 Column 已有 screenPadding，这里只补垂直间距）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimens.s2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                AppIcon(
                    imageVector = AppIcons.Back,
                    contentDescription = "返回",
                    tint = AppTheme.colors.ink
                )
            }
            Text(
                text = if (isEditing) "编辑动作" else "添加动作",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = AppTypeWeight.emphasis,
                color = AppTheme.colors.ink,
                modifier = Modifier.weight(1f)
            )
        }

        LiteCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.s4),
                verticalArrangement = Arrangement.spacedBy(AppDimens.s3)
            ) {
            // 动作名称
            AppTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = "动作名称 *",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nameError
            )

            if (nameError) {
                Text(
                    text = "请输入动作名称",
                    color = AppTheme.colors.danger,
                    style = MaterialTheme.typography.caption
                )
            }

            // 步骤说明
            AppTextField(
                value = stepsText,
                onValueChange = { stepsText = it },
                label = "步骤说明",
                modifier = Modifier.fillMaxWidth(),
                minHeight = AppDimens.formTextAreaHeight,
                maxLines = 5
            )

            // 默认时长和休息时长
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.s4)
            ) {
                AppTextField(
                    value = defaultTime,
                    onValueChange = { newValue: String ->
                        defaultTime = newValue.filter { ch: Char -> ch.isDigit() }
                        timeError = false
                    },
                    label = "默认时长(秒) *",
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = timeError
                )

                AppTextField(
                    value = restTime,
                    onValueChange = { newValue: String ->
                        restTime = newValue.filter { ch: Char -> ch.isDigit() }
                        timeError = false
                    },
                    label = "休息时长(秒) *",
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = timeError
                )
            }

            // 排序索引
            AppTextField(
                value = orderIndex,
                onValueChange = { newValue: String ->
                    orderIndex = newValue.filter { ch: Char -> ch.isDigit() }
                    timeError = false
                },
                label = "排序索引",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = timeError
            )

            if (timeError) {
                Text(
                    text = errorMessage.ifEmpty { "请输入有效数字" },
                    color = AppTheme.colors.danger,
                    style = MaterialTheme.typography.caption
                )
            }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.buttonHeight),
                shape = AppShapes.button,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AppTheme.colors.brandStrong,
                    contentColor = AppTheme.colors.onBrand
                )
            ) {
                Text("保存", style = MaterialTheme.typography.subtitle1)
            }
        }
    }
}