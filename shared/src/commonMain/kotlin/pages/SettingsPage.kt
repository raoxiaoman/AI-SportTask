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
import data.SportTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 设置屏幕
@Composable
fun SettingsScreen(
    onThemeChange: ((Boolean) -> Unit)? = null
) {
    val repository = SportTaskRepository()
    val scope = rememberCoroutineScope()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportJson by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf(false) }

    var totalGroups by remember { mutableStateOf(0) }
    var totalActions by remember { mutableStateOf(0) }
    var totalCheckins by remember { mutableStateOf(0) }

    // 通知设置状态
    var reminderEnabled by remember { mutableStateOf(false) }

    // 深色模式状态
    var isDarkMode by remember { mutableStateOf(false) }

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
                        subtitle = if (isDarkMode) "已开启" else "未开启",
                        checked = isDarkMode,
                        onCheckedChange = { enabled ->
                            isDarkMode = enabled
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
                        subtitle = "每天提醒您完成训练",
                        checked = reminderEnabled,
                        onCheckedChange = { enabled ->
                            reminderEnabled = enabled
                        }
                    )
                }
            }

            // 训练设置部分
            item {
                SettingsSection(title = "训练") {
                    var autoRestEnabled by remember { mutableStateOf(true) }
                    SettingsSwitchItem(
                        icon = "⏱️",
                        title = "自动休息",
                        subtitle = "动作完成后自动开始休息计时",
                        checked = autoRestEnabled,
                        onCheckedChange = { autoRestEnabled = it }
                    )
                    var soundEnabled by remember { mutableStateOf(true) }
                    SettingsSwitchItem(
                        icon = "🔊",
                        title = "声音提示",
                        subtitle = "计时器开始和结束时播放声音",
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )
                    var vibrationEnabled by remember { mutableStateOf(false) }
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
                        icon = "🗑️",
                        title = "清除数据",
                        subtitle = "删除所有训练记录和分组",
                        onClick = { showClearDataDialog = true },
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
