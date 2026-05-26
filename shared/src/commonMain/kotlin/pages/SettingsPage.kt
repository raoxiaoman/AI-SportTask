package pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cloud.AuthService
import cloud.SyncManager
import data.SportTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 设置屏幕
@Composable
fun SettingsScreen(
    isDarkMode: Boolean = false,
    onThemeChange: ((Boolean) -> Unit)? = null
) {
    val repository = SportTaskRepository
    val scope = rememberCoroutineScope()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var exportJson by remember { mutableStateOf("") }
    var importJson by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<data.ImportResult?>(null) }

    var totalGroups by remember { mutableStateOf(0) }
    var totalActions by remember { mutableStateOf(0) }
    var totalCheckins by remember { mutableStateOf(0) }

    // 通知设置状态
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableStateOf(9) }
    var reminderMinute by remember { mutableStateOf(0) }

    // 训练设置状态
    var autoRestEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(false) }

    // 深色模式状态（由父组件管理）
    var localDarkMode by remember { mutableStateOf(false) }
    // 当父组件更新时同步
    LaunchedEffect(isDarkMode) {
        localDarkMode = isDarkMode
    }

    // 加载统计数据
    fun loadStats() {
        scope.launch {
            totalGroups = repository.getGroups().size
            totalActions = repository.getAllActions().size
            totalCheckins = withContext(Dispatchers.Default) {
                repository.getCheckinsByDateRange("1970-01-01", "2099-12-31").size
            }
        }
    }

    LaunchedEffect(Unit) {
        loadStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 统计数据部分
            item {
                SettingsSection(title = "数据统计") {
                    SettingsItem(
                        icon = "📁",
                        title = "训练分组",
                        subtitle = "$totalGroups 个",
                        onClick = { }
                    )
                    SettingsItem(
                        icon = "🏋️",
                        title = "训练动作",
                        subtitle = "$totalActions 个",
                        onClick = { }
                    )
                    SettingsItem(
                        icon = "✅",
                        title = "打卡记录",
                        subtitle = "$totalCheckins 条",
                        onClick = { }
                    )
                }
            }

            // 外观设置部分
            item {
                SettingsSection(title = "外观") {
                    SettingsSwitchItem(
                        icon = "🌙",
                        title = "深色模式",
                        subtitle = if (localDarkMode) "已开启" else "未开启",
                        checked = localDarkMode,
                        onCheckedChange = { enabled ->
                            localDarkMode = enabled
                            onThemeChange?.invoke(enabled)
                        }
                    )
                }
            }

            // 通知设置部分
            item {
                SettingsSection(title = "通知") {
                    SettingsSwitchItem(
                        icon = "🔔",
                        title = "训练提醒",
                        subtitle = if (reminderEnabled) "每天 ${reminderHour.toString().padStart(2, '0')}:${reminderMinute.toString().padStart(2, '0')} 提醒" else "每天提醒您完成训练",
                        checked = reminderEnabled,
                        onCheckedChange = { enabled ->
                            reminderEnabled = enabled
                        }
                    )
                    if (reminderEnabled) {
                        SettingsItem(
                            icon = "🕐",
                            title = "提醒时间",
                            subtitle = "${reminderHour.toString().padStart(2, '0')}:${reminderMinute.toString().padStart(2, '0')}",
                            onClick = { showTimePickerDialog = true }
                        )
                    }
                }
            }

            // 训练设置部分
            item {
                SettingsSection(title = "训练") {
                    SettingsSwitchItem(
                        icon = "⏱️",
                        title = "自动休息",
                        subtitle = "动作完成后自动开始休息计时",
                        checked = autoRestEnabled,
                        onCheckedChange = { autoRestEnabled = it }
                    )
                    SettingsSwitchItem(
                        icon = "🔊",
                        title = "声音提示",
                        subtitle = "计时器开始和结束时播放声音",
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )
                    SettingsSwitchItem(
                        icon = "📳",
                        title = "震动反馈",
                        subtitle = "计时器结束时震动提醒",
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it }
                    )
                }
            }

            // 数据管理部分
            item {
                SettingsSection(title = "数据管理") {
                    SettingsItem(
                        icon = "📤",
                        title = "导出数据",
                        subtitle = "将数据导出为 JSON 格式",
                        onClick = {
                            isExporting = true
                            scope.launch {
                                exportJson = withContext(Dispatchers.Default) {
                                    repository.exportDataAsJson()
                                }
                                isExporting = false
                                showExportDialog = true
                            }
                        }
                    )
                    SettingsItem(
                        icon = "📥",
                        title = "导入数据",
                        subtitle = "从 JSON 格式导入数据",
                        onClick = {
                            showImportDialog = true
                        }
                    )
                    SettingsItem(
                        icon = "🗑️",
                        title = "清除数据",
                        subtitle = "删除所有训练记录和分组",
                        onClick = { showClearDataDialog = true },
                        isDestructive = true
                    )
                }
            }

            // 云同步部分
            item {
                SettingsSection(title = "云同步") {
                    val syncState = SyncManager.state
                    val syncSubtitle = when (syncState) {
                        is cloud.SyncManager.SyncState.Idle -> {
                            if (SyncManager.lastSyncSuccess) "上次同步成功" else "未同步"
                        }
                        is cloud.SyncManager.SyncState.Syncing -> "同步中..."
                        is cloud.SyncManager.SyncState.Success -> "同步成功"
                        is cloud.SyncManager.SyncState.Error -> "同步失败: ${syncState.message}"
                    }
                    val syncIcon = when (syncState) {
                        is cloud.SyncManager.SyncState.Syncing -> "🔄"
                        is cloud.SyncManager.SyncState.Success -> "✅"
                        is cloud.SyncManager.SyncState.Error -> "❌"
                        else -> if (SyncManager.lastSyncSuccess) "☁️" else "☁️"
                    }
                    SettingsItem(
                        icon = syncIcon,
                        title = "同步状态",
                        subtitle = syncSubtitle,
                        onClick = {
                            scope.launch {
                                SyncManager.sync()
                            }
                        }
                    )
                    val authState = AuthService.authState.collectAsState().value
                    val userEmail = if (authState is cloud.AuthService.AuthState.SignedIn) {
                        authState.email
                    } else {
                        "未登录"
                    }
                    SettingsItem(
                        icon = "👤",
                        title = "账号",
                        subtitle = userEmail,
                        onClick = { }
                    )
                    SettingsItem(
                        icon = "🚪",
                        title = "退出登录",
                        subtitle = "退出后数据保留在本地",
                        onClick = {
                            scope.launch {
                                AuthService.signOut()
                            }
                        },
                        isDestructive = true
                    )
                }
            }

            // 关于部分
            item {
                SettingsSection(title = "关于") {
                    SettingsItem(
                        icon = "ℹ️",
                        title = "关于应用",
                        subtitle = "版本 1.0.0",
                        onClick = { showAboutDialog = true }
                    )
                    SettingsItem(
                        icon = "⭐",
                        title = "评价我们",
                        subtitle = "在应用商店留下评价",
                        onClick = { }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 关于对话框
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("AI SportTask") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🏋️",
                        style = MaterialTheme.typography.h1
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "版本 1.0.0",
                        style = MaterialTheme.typography.body1
                    )
                    Text(
                        text = "一款智能健身任务管理应用",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    // 清除数据确认对话框
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("确认清除") },
            text = {
                Column {
                    Text("确定要删除以下所有数据吗？此操作无法撤销。")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• $totalGroups 个训练分组",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                    Text(
                        text = "• $totalActions 个训练动作",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                    Text(
                        text = "• $totalCheckins 条打卡记录",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                if (isClearing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    TextButton(
                        onClick = {
                            isClearing = true
                            scope.launch {
                                withContext(Dispatchers.Default) {
                                    repository.clearAllData()
                                }
                                isClearing = false
                                showClearDataDialog = false
                                loadStats()
                                showSuccessMessage = true
                            }
                        }
                    ) {
                        Text("确定", color = Color.Red)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDataDialog = false },
                    enabled = !isClearing
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 导出数据对话框
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("导出数据") },
            text = {
                Column {
                    Text(
                        text = "数据已导出为 JSON 格式。",
                        style = MaterialTheme.typography.body2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "共 $totalGroups 个分组、$totalActions 个动作、$totalCheckins 条记录",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = exportJson,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        readOnly = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // 导入数据对话框
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { 
                showImportDialog = false
                importJson = ""
                importResult = null
            },
            title = { Text("导入数据") },
            text = {
                Column {
                    Text(
                        text = "请粘贴 JSON 数据：",
                        style = MaterialTheme.typography.body2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJson,
                        onValueChange = { importJson = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("粘贴 JSON 数据...") }
                    )
                    
                    if (importResult != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (importResult!!.success) {
                            Text(
                                text = "✅ 导入成功！",
                                style = MaterialTheme.typography.body1,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = "分组: ${importResult!!.groupsImported}, 动作: ${importResult!!.actionsImported}, 打卡: ${importResult!!.checkinsImported}",
                                style = MaterialTheme.typography.body2,
                                color = Color.Gray
                            )
                        } else {
                            Text(
                                text = "❌ 导入失败: ${importResult!!.errorMessage}",
                                style = MaterialTheme.typography.body1,
                                color = Color.Red
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    TextButton(
                        onClick = {
                            if (importJson.isNotBlank()) {
                                isImporting = true
                                scope.launch {
                                    importResult = withContext(Dispatchers.Default) {
                                        repository.importDataFromJson(importJson)
                                    }
                                    isImporting = false
                                    if (importResult?.success == true) {
                                        loadStats()
                                    }
                                }
                            }
                        },
                        enabled = importJson.isNotBlank()
                    ) {
                        Text("导入")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showImportDialog = false
                    importJson = ""
                    importResult = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    // 时间选择器对话框
    if (showTimePickerDialog) {
        TimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onConfirm = { hour, minute ->
                reminderHour = hour
                reminderMinute = minute
                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }

    // 成功提示
    if (showSuccessMessage) {
        AlertDialog(
            onDismissRequest = { showSuccessMessage = false },
            title = { Text("操作成功") },
            text = { Text("数据已清除。") },
            confirmButton = {
                TextButton(onClick = { showSuccessMessage = false }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SettingsItem(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.h6
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                color = if (isDestructive) Color.Red else Color.Unspecified
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.body2,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.h6
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.body2,
                color = Color.Gray
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择提醒时间") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 小时选择
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { 
                        selectedHour = (selectedHour + 1) % 24 
                    }) {
                        Text("▲", style = MaterialTheme.typography.h5)
                    }
                    Text(
                        text = selectedHour.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.h3
                    )
                    IconButton(onClick = { 
                        selectedHour = if (selectedHour > 0) selectedHour - 1 else 23 
                    }) {
                        Text("▼", style = MaterialTheme.typography.h5)
                    }
                }
                
                Text(
                    text = " : ",
                    style = MaterialTheme.typography.h3,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                // 分钟选择
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { 
                        selectedMinute = (selectedMinute + 5) % 60 
                    }) {
                        Text("▲", style = MaterialTheme.typography.h5)
                    }
                    Text(
                        text = selectedMinute.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.h3
                    )
                    IconButton(onClick = { 
                        selectedMinute = if (selectedMinute >= 5) selectedMinute - 5 else 55 
                    }) {
                        Text("▼", style = MaterialTheme.typography.h5)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHour, selectedMinute) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
