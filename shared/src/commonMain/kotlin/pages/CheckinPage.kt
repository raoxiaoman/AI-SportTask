package pages
import ui.AppConfirmDialog
import ui.AppDialog
import ui.AppDialogTextField
import ui.AppIcon
import ui.AppIcons
import ui.theme.*


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import data.SportTaskRepository
import data.formatTime
import ui.EmptyState
import ui.FullScreenLoading
import ui.IconBadge
import ui.LiteCard
import ui.PageHeader
import ui.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// 打卡屏幕
@Composable
fun CheckinScreen() {
    var isLoading by remember { mutableStateOf(true) }
    var checkins by remember { mutableStateOf<List<CheckinItemWithGroup>>(emptyList()) }
    var filteredCheckins by remember { mutableStateOf<List<CheckinItemWithGroup>>(emptyList()) }
    var groups by remember { mutableStateOf<List<GroupOption>>(emptyList()) }

    // 筛选状态
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // 编辑状态
    var editingCheckin by remember { mutableStateOf<CheckinItemWithGroup?>(null) }
    var editDuration by remember { mutableStateOf("") }
    var editCompleted by remember { mutableStateOf(false) }

    // 删除确认
    var deletingCheckin by remember { mutableStateOf<CheckinItemWithGroup?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val repository = SportTaskRepository
    val scope = rememberCoroutineScope()

    // 加载数据
    fun loadData() {
        scope.launch {
            isLoading = true
            val today = todayDateString()
            val monthAgo = daysAgoDateString(90) // 扩展到90天
            val dbCheckins = withContext(Dispatchers.Default) {
                repository.getCheckinsByDateRange(monthAgo, today)
            }

            val groupList = withContext(Dispatchers.Default) {
                repository.getGroups()
            }
            groups = listOf(GroupOption(null, "全部")) + groupList.map {
                GroupOption(it.id, it.name)
            }

            val groupMap = groupList.associateBy { it.id }

            checkins = dbCheckins.map { checkin ->
                CheckinItemWithGroup(
                    id = checkin.id,
                    date = checkin.date,
                    duration = checkin.duration?.toInt() ?: 0,
                    isCompleted = checkin.is_completed == 1L,
                    groupName = checkin.group_id?.let { groupMap[it]?.name } ?: "自由训练",
                    groupId = checkin.group_id
                )
            }.sortedByDescending { it.date }

            // 应用筛选
            filteredCheckins = applyFilters(checkins, searchQuery, selectedGroupId)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    // 重新应用筛选
    LaunchedEffect(searchQuery, selectedGroupId, checkins) {
        filteredCheckins = applyFilters(checkins, searchQuery, selectedGroupId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 页头：标题 + 搜索/删除入口
            PageHeader(
                title = "打卡记录",
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.s2)) {
                        IconBadge(
                            icon = AppIcons.Search,
                            contentDescription = "搜索",
                            // 直接可点击，规格与设置页页头徽标一致，不撑高页头行
                            modifier = Modifier.clickable { showFilterSheet = true }
                        )
                        if (filteredCheckins.isNotEmpty()) {
                            IconBadge(
                                icon = AppIcons.Delete,
                                contentDescription = "删除",
                                tint = AppTheme.colors.danger,
                                container = AppTheme.colors.dangerSoft,
                                modifier = Modifier.clickable { showDeleteAllDialog = true }
                            )
                        }
                    }
                }
            )

            // 搜索栏
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.screenPadding, vertical = AppDimens.s2)
            )

            // 筛选标签
            FilterChips(
                groups = groups,
                selectedGroupId = selectedGroupId,
                onGroupSelected = { selectedGroupId = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.screenPadding)
            )

            // 内容区域
            if (isLoading) {
                FullScreenLoading()
            } else if (filteredCheckins.isEmpty()) {
                EmptyState(
                    icon = AppIcons.Note,
                    title = "暂无打卡记录",
                    subtitle = "开始训练来完成你的第一次打卡吧！"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = AppDimens.screenPadding,
                        vertical = AppDimens.s2
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.s3)
                ) {
                    item {
                        Text(
                            text = "共 ${filteredCheckins.size} 条记录",
                            style = MaterialTheme.typography.caption,
                            color = AppTheme.colors.inkMuted
                        )
                    }

                    // 按日期分组显示
                    val groupedCheckins = filteredCheckins.groupBy { it.date }
                    groupedCheckins.forEach { (date, dayCheckins) ->
                        item {
                            DateSection(
                                date = date,
                                checkins = dayCheckins,
                                onEditClick = { checkin ->
                                    editingCheckin = checkin
                                    editDuration = checkin.duration.toString()
                                    editCompleted = checkin.isCompleted
                                },
                                onDeleteClick = { deletingCheckin = it }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(AppDimens.s4))
                    }
                }
            }
        }
    }

    // 编辑对话框
    editingCheckin?.let { checkin ->
        EditCheckinDialog(
            checkin = checkin,
            duration = editDuration,
            completed = editCompleted,
            onDurationChange = { editDuration = it },
            onCompletedChange = { editCompleted = it },
            onDismiss = { editingCheckin = null },
            onSave = { newDuration, newCompleted ->
                scope.launch {
                    withContext(Dispatchers.Default) {
                        repository.updateCheckin(
                            id = checkin.id,
                            duration = newDuration.toLong(),
                            isCompleted = if (newCompleted) 1L else 0L
                        )
                    }
                    editingCheckin = null
                    loadData()
                }
            }
        )
    }

    // 删除确认对话框
    deletingCheckin?.let { checkin ->
        AppConfirmDialog(
            title = "删除打卡记录",
            message = "确定要删除这条打卡记录吗？删除后无法恢复。",
            onDismissRequest = { deletingCheckin = null },
            onConfirm = {
                scope.launch {
                    withContext(Dispatchers.Default) {
                        repository.deleteCheckin(checkin.id)
                    }
                    deletingCheckin = null
                    loadData()
                }
            },
            confirmLabel = "删除",
            danger = true
        )
    }

    // 删除全部确认对话框
    if (showDeleteAllDialog) {
        AppConfirmDialog(
            title = "删除全部记录",
            message = "确定要删除所有筛选后的打卡记录吗？此操作无法撤销。",
            onDismissRequest = { showDeleteAllDialog = false },
            onConfirm = {
                scope.launch {
                    withContext(Dispatchers.Default) {
                        filteredCheckins.forEach { repository.deleteCheckin(it.id) }
                    }
                    showDeleteAllDialog = false
                    loadData()
                }
            },
            confirmLabel = "删除全部",
            danger = true
        )
    }

    // 筛选弹窗：标题与按钮由 AppDialog 统一提供，内容只负责表单
    if (showFilterSheet) {
        AppDialog(
            onDismissRequest = { showFilterSheet = false },
            title = "筛选打卡记录",
            subtitle = "按关键词或分组查看记录",
            extraActionLabel = "清除筛选",
            onExtraAction = {
                searchQuery = ""
                selectedGroupId = null
            },
            dismissLabel = "取消",
            confirmLabel = "完成",
            onConfirm = { showFilterSheet = false }
        ) {
            FilterDialogContent(
                groups = groups,
                selectedGroupId = selectedGroupId,
                searchQuery = searchQuery,
                onGroupSelected = { selectedGroupId = it },
                onSearchQueryChange = { searchQuery = it }
            )
        }
    }
}

private fun applyFilters(
    checkins: List<CheckinItemWithGroup>,
    searchQuery: String,
    selectedGroupId: Long?
): List<CheckinItemWithGroup> {
    return checkins.filter { checkin ->
        val matchesSearch = searchQuery.isEmpty() ||
                checkin.groupName.contains(searchQuery, ignoreCase = true) ||
                checkin.date.contains(searchQuery)
        val matchesGroup = selectedGroupId == null || checkin.groupId == selectedGroupId
        matchesSearch && matchesGroup
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 填充式圆角搜索框：无描边，浅灰底
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("搜索分组名称或日期...") },
        singleLine = true,
        shape = AppShapes.chip,
        leadingIcon = { AppIcon(AppIcons.Search, "搜索", size = AppDimens.iconSmall, tint = AppTheme.colors.inkMuted) },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = AppTheme.colors.surfaceAlt,
            focusedBorderColor = AppTheme.colors.surfaceAlt,
            unfocusedBorderColor = AppTheme.colors.surfaceAlt,
            cursorColor = AppTheme.colors.brand
        )
    )
}

@Composable
fun FilterChips(
    groups: List<GroupOption>,
    selectedGroupId: Long?,
    onGroupSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    // 分组可能很多，一行放不下时横向滑动，绝不换行裁切
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.s2)
    ) {
        groups.forEach { group ->
            FilterChip(
                text = group.name,
                isSelected = selectedGroupId == group.id,
                onClick = {
                    onGroupSelected(if (selectedGroupId == group.id) null else group.id)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = AppShapes.pill,
        color = if (isSelected) AppTheme.colors.brand else AppTheme.colors.surface,
        border = if (isSelected) null else
            androidx.compose.foundation.BorderStroke(AppDimens.divider, AppTheme.colors.outline)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = if (isSelected) AppTheme.colors.onBrand else AppTheme.colors.inkSub,
            modifier = Modifier.padding(horizontal = AppDimens.s3, vertical = AppDimens.s2)
        )
    }
}

@Composable
fun DateSection(
    date: String,
    checkins: List<CheckinItemWithGroup>,
    onEditClick: (CheckinItemWithGroup) -> Unit,
    onDeleteClick: (CheckinItemWithGroup) -> Unit
) {
    val parts = date.split("-")
    val formattedDate = if (parts.size == 3) {
        "${parts[0]}年${parts[1].toInt()}月${parts[2].toInt()}日"
    } else {
        date
    }
    val dayLabels = listOf("", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
    val dayOfWeek = dayLabels.getOrElse(dayOfWeekFromDate(date)) { "" }

    var expanded by remember { mutableStateOf(true) }

    LiteCard {
        Column(
            modifier = Modifier.padding(AppDimens.s4)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.body1,
                        fontWeight = AppTypeWeight.emphasis,
                        color = AppTheme.colors.ink
                    )
                    Spacer(modifier = Modifier.height(AppDimens.s1))
                    Text(
                        text = dayOfWeek,
                        style = MaterialTheme.typography.caption,
                        color = AppTheme.colors.inkSub
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${checkins.size} 次训练",
                        style = MaterialTheme.typography.caption,
                        color = AppTheme.colors.brandStrong
                    )
                    Spacer(modifier = Modifier.height(AppDimens.s1))
                    Text(
                        text = formatTime(checkins.sumOf { it.duration }),
                        style = MaterialTheme.typography.body1,
                        fontWeight = AppTypeWeight.medium,
                        color = AppTheme.colors.ink
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(AppDimens.s3))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppDimens.divider)
                            .background(AppTheme.colors.outline)
                    )
                    Spacer(modifier = Modifier.height(AppDimens.s2))

                    checkins.forEachIndexed { index, checkin ->
                        CheckinItemRow(
                            checkin = checkin,
                            onEditClick = { onEditClick(checkin) },
                            onDeleteClick = { onDeleteClick(checkin) }
                        )
                        if (index < checkins.size - 1) {
                            Spacer(modifier = Modifier.height(AppDimens.s2))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckinItemRow(
    checkin: CheckinItemWithGroup,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showActions = !showActions },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 状态指示器
        Box(
            modifier = Modifier
                .size(AppDimens.s2)
                .background(
                    color = if (checkin.isCompleted) AppTheme.colors.success else AppTheme.colors.inkMuted,
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(AppDimens.s3))

        // 训练类型
        Text(
            text = checkin.groupName,
            style = MaterialTheme.typography.body1,
            color = AppTheme.colors.ink,
            modifier = Modifier.weight(1f)
        )

        // 时长
        Text(
            text = formatTime(checkin.duration),
            style = MaterialTheme.typography.body2,
            color = AppTheme.colors.inkSub
        )

        Spacer(modifier = Modifier.width(AppDimens.s2))

        // 完成状态标签
        if (checkin.isCompleted) {
            Text(
                text = "完成",
                style = MaterialTheme.typography.caption,
                color = AppTheme.colors.success,
                modifier = Modifier
                    .background(
                        color = AppTheme.colors.successSoft,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = AppDimens.s2, vertical = AppDimens.hairline)
            )
        }

        // 操作按钮
        if (showActions) {
            Spacer(modifier = Modifier.width(AppDimens.s2))
            IconButton(onClick = onEditClick, modifier = Modifier.size(AppDimens.s8)) {
                AppIcon(AppIcons.Edit, "编辑", size = AppDimens.iconSmall)
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(AppDimens.s8)) {
                AppIcon(AppIcons.Delete, "删除", size = AppDimens.iconSmall)
            }
        }
    }
}

@Composable
fun EditCheckinDialog(
    checkin: CheckinItemWithGroup,
    duration: String,
    completed: Boolean,
    onDurationChange: (String) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Int, Boolean) -> Unit
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = "编辑打卡记录",
        subtitle = "分组：${checkin.groupName}",
        confirmLabel = "保存",
        onConfirm = {
            val durationInt = duration.toIntOrNull() ?: 0
            onSave(durationInt, completed)
        },
        dismissLabel = "取消",
        onDismiss = onDismiss
    ) {
        AppDialogTextField(
            value = duration,
            onValueChange = onDurationChange,
            label = "时长（秒）",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(AppDimens.s4))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已完成",
                style = MaterialTheme.typography.body1,
                color = AppTheme.colors.ink
            )
            Switch(
                checked = completed,
                onCheckedChange = onCompletedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppTheme.colors.onBrand,
                    checkedTrackColor = AppTheme.colors.brand,
                    uncheckedThumbColor = AppTheme.colors.surface,
                    uncheckedTrackColor = AppTheme.colors.surfaceAlt
                )
            )
        }
    }
}

/**
 * 筛选弹窗的表单内容。标题、辅助操作与确认按钮统一由 [AppDialog] 提供，
 * 这里只渲染控件，避免旧版「AlertDialog 标题 + 内容里又一个标题」的重复结构。
 */
@Composable
fun FilterDialogContent(
    groups: List<GroupOption>,
    selectedGroupId: Long?,
    searchQuery: String,
    onGroupSelected: (Long?) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AppDialogTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = "搜索记录",
            singleLine = true,
            leadingIcon = {
                AppIcon(
                    imageVector = AppIcons.Search,
                    contentDescription = "搜索",
                    tint = AppTheme.colors.inkMuted,
                    size = AppDimens.iconSmall
                )
            }
        )

        Spacer(modifier = Modifier.height(AppDimens.s6))
        Text(
            text = "按分组筛选",
            style = MaterialTheme.typography.subtitle2,
            color = AppTheme.colors.inkSub
        )
        Spacer(modifier = Modifier.height(AppDimens.s2))

        groups.forEach { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGroupSelected(group.id) }
                    .padding(vertical = AppDimens.s1),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedGroupId == group.id,
                    onClick = { onGroupSelected(group.id) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = AppTheme.colors.brand,
                        unselectedColor = AppTheme.colors.inkMuted
                    )
                )
                Spacer(modifier = Modifier.width(AppDimens.s2))
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.body1,
                    color = AppTheme.colors.ink
                )
            }
        }
    }
}



data class CheckinItemWithGroup(
    val id: Long,
    val date: String,
    val duration: Int,
    val isCompleted: Boolean,
    val groupName: String,
    val groupId: Long?
)

data class GroupOption(
    val id: Long?,
    val name: String
)
