package pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    val repository = SportTaskRepository()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("打卡记录") },
                actions = {
                    // 搜索按钮
                    IconButton(onClick = { showFilterSheet = true }) {
                        Text("🔍")
                    }
                    // 删除全部按钮
                    if (filteredCheckins.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Text("🗑️")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 搜索栏
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 筛选标签
            FilterChips(
                groups = groups,
                selectedGroupId = selectedGroupId,
                onGroupSelected = { selectedGroupId = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // 内容区域
            if (isLoading) {
                FullScreenLoading()
            } else if (filteredCheckins.isEmpty()) {
                EmptyState(
                    icon = "📝",
                    title = "暂无打卡记录",
                    subtitle = "开始训练来完成你的第一次打卡吧！"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "共 ${filteredCheckins.size} 条记录",
                            style = MaterialTheme.typography.subtitle2,
                            color = Color.Gray
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
                        Spacer(modifier = Modifier.height(16.dp))
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
        AlertDialog(
            onDismissRequest = { deletingCheckin = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条打卡记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.Default) {
                                repository.deleteCheckin(checkin.id)
                            }
                            deletingCheckin = null
                            loadData()
                        }
                    }
                ) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCheckin = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 删除全部确认对话框
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("确认删除全部") },
            text = { Text("确定要删除所有筛选后的打卡记录吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.Default) {
                                filteredCheckins.forEach { repository.deleteCheckin(it.id) }
                            }
                            showDeleteAllDialog = false
                            loadData()
                        }
                    }
                ) {
                    Text("删除全部", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 筛选弹窗
    if (showFilterSheet) {
        AlertDialog(
            onDismissRequest = { showFilterSheet = false },
            title = { Text("筛选") },
            text = {
                FilterBottomSheet(
                    groups = groups,
                    selectedGroupId = selectedGroupId,
                    searchQuery = searchQuery,
                    onGroupSelected = { selectedGroupId = it },
                    onSearchQueryChange = { searchQuery = it },
                    onClearFilters = {
                        searchQuery = ""
                        selectedGroupId = null
                    },
                    onDismiss = { showFilterSheet = false }
                )
            },
            confirmButton = {
                TextButton(onClick = { showFilterSheet = false }) {
                    Text("确定")
                }
            }
        )
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
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("搜索分组名称或日期...") },
        singleLine = true,
        leadingIcon = { Text("🔍") }
    )
}

@Composable
fun FilterChips(
    groups: List<GroupOption>,
    selectedGroupId: Long?,
    onGroupSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups.take(4).forEach { group ->
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
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        border = if (isSelected) null else ButtonDefaults.outlinedBorder
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            color = if (isSelected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dayOfWeek,
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${checkins.size} 次训练",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = formatTime(checkins.sumOf { it.duration }),
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    checkins.forEachIndexed { index, checkin ->
                        CheckinItemRow(
                            checkin = checkin,
                            onEditClick = { onEditClick(checkin) },
                            onDeleteClick = { onDeleteClick(checkin) }
                        )
                        if (index < checkins.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
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
                .size(8.dp)
                .background(
                    color = if (checkin.isCompleted) Color(0xFF4CAF50) else Color.Gray,
                    shape = MaterialTheme.shapes.small
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 训练类型
        Text(
            text = checkin.groupName,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.weight(1f)
        )

        // 时长
        Text(
            text = formatTime(checkin.duration),
            style = MaterialTheme.typography.body2,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 完成状态标签
        if (checkin.isCompleted) {
            Text(
                text = "完成",
                style = MaterialTheme.typography.caption,
                color = Color(0xFF4CAF50),
                modifier = Modifier
                    .background(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        // 操作按钮
        if (showActions) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                Text("✏️", style = MaterialTheme.typography.body2)
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                Text("🗑️", style = MaterialTheme.typography.body2)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑打卡记录") },
        text = {
            Column {
                Text(
                    text = "分组: ${checkin.groupName}",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = duration,
                    onValueChange = onDurationChange,
                    label = { Text("时长（秒）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("已完成")
                    Switch(
                        checked = completed,
                        onCheckedChange = onCompletedChange
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val durationInt = duration.toIntOrNull() ?: 0
                    onSave(durationInt, completed)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun FilterBottomSheet(
    groups: List<GroupOption>,
    selectedGroupId: Long?,
    searchQuery: String,
    onGroupSelected: (Long?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "筛选",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 搜索
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("搜索") },
            leadingIcon = { Text("🔍") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 分组筛选
        Text(
            text = "按分组筛选",
            style = MaterialTheme.typography.subtitle2,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        groups.forEach { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGroupSelected(group.id) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedGroupId == group.id,
                    onClick = { onGroupSelected(group.id) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(group.name)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onClearFilters,
                modifier = Modifier.weight(1f)
            ) {
                Text("清除筛选")
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("确定")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
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
