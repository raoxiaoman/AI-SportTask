package pages
import ui.AppConfirmDialog
import ui.AppDialog
import ui.AppDialogTextField
import ui.AppIcon
import ui.AppIcons
import ui.IconBadge
import ui.LiteCard
import ui.PageHeader
import ui.SectionHeader
import ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.SportTaskRepository
import data.remote.ApiClient
import data.remote.AuthService
import data.remote.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 设置屏幕
@Composable
fun SettingsScreen(
    onThemeChange: ((Boolean) -> Unit)? = null,
    authService: AuthService? = null,
    onLogout: (() -> Unit)? = null
) {
    val repository = SportTaskRepository
    val scope = rememberCoroutineScope()

    // 同步状态
    var isSyncing by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var syncSuccess by remember { mutableStateOf(false) }

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
    var showLogoutDialog by remember { mutableStateOf(false) }

    var totalGroups by remember { mutableStateOf(0) }
    var totalActions by remember { mutableStateOf(0) }
    var totalCheckins by remember { mutableStateOf(0) }

    // 通知设置状态
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableStateOf(21) }
    var reminderMinute by remember { mutableStateOf(0) }

    // 训练设置状态
    var autoRestEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(TrainingFeedback.isSoundEnabled()) }
    var vibrationEnabled by remember { mutableStateOf(TrainingFeedback.isVibrationEnabled()) }
    var voiceEnabled by remember { mutableStateOf(TrainingFeedback.isVoiceEnabled()) }
    var tickEnabled by remember { mutableStateOf(TrainingFeedback.isTickEnabled()) }

    // 深色模式状态：从持久化偏好读取，与 App 入口保持一致
    var isDarkMode by remember { mutableStateOf(ThemePreference.isDark()) }

    // 康复计划状态
    var planSummary by remember { mutableStateOf("未导入") }
    var isPlanBusy by remember { mutableStateOf(false) }

    fun loadPlanSummary() {
        scope.launch {
            planSummary = withContext(Dispatchers.Default) {
                runCatching {
                    val state = data.plan.PlanRepository.loadState() ?: return@runCatching "未导入"
                    val task = data.plan.PlanRepository.todayTask()
                    when {
                        task == null -> "已导入 · ${state.startDate} 开始"
                        task.finished -> "8 周计划已完成"
                        else -> "${state.startDate} 开始 · ${data.plan.PlanScheduler.reminderText(task)}"
                    }
                }.getOrElse { "未导入" }
            }
        }
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
        loadPlanSummary()
        // 提醒开关与时间以平台侧（AlarmManager）为准，避免 UI 状态与真实闹钟不一致
        val config = data.plan.ReminderController.current()
        reminderEnabled = config.enabled
        reminderHour = config.hour
        reminderMinute = config.minute
        soundEnabled = TrainingFeedback.isSoundEnabled()
        vibrationEnabled = TrainingFeedback.isVibrationEnabled()
        voiceEnabled = TrainingFeedback.isVoiceEnabled()
        tickEnabled = TrainingFeedback.isTickEnabled()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // 页头：标题 + 头像容器
            item {
                PageHeader(
                    title = "设置",
                    trailing = {
                        IconBadge(
                            icon = AppIcons.Person,
                            contentDescription = "账号"
                        )
                    }
                )
            }

            // 训练设置
            item { SectionHeader(text = "训练设置") }
            item {
                LiteCard(modifier = Modifier.padding(horizontal = AppDimens.screenPadding)) {
                    Column(modifier = Modifier.padding(horizontal = AppDimens.s3, vertical = AppDimens.s1)) {
                        SettingsSwitchRow(
                            icon = AppIcons.Clock,
                            title = "自动休息",
                            subtitle = "动作完成后自动开始休息计时",
                            checked = autoRestEnabled,
                            onCheckedChange = { autoRestEnabled = it }
                        )
                        SettingsSwitchRow(
                            icon = AppIcons.Bell,
                            title = "每日提醒",
                            subtitle = if (reminderEnabled)
                                "每天 ${reminderHour.toString().padStart(2, '0')}:${reminderMinute.toString().padStart(2, '0')} 提醒训练"
                            else "每天定时提醒您完成训练",
                            checked = reminderEnabled,
                            onCheckedChange = { enabled ->
                                reminderEnabled = enabled
                                data.plan.ReminderController.setEnabled(enabled, reminderHour, reminderMinute)
                            }
                        )
                        if (reminderEnabled) {
                            SettingsRow(
                                icon = AppIcons.Clock,
                                title = "提醒时间",
                                subtitle = "${reminderHour.toString().padStart(2, '0')}:${reminderMinute.toString().padStart(2, '0')}",
                                onClick = { showTimePickerDialog = true }
                            )
                        }
                        SettingsSwitchRow(
                            icon = AppIcons.Sound,
                            title = "声音提示",
                            subtitle = "开始 / 暂停 / 动作切换 / 完成时播放提示音",
                            checked = soundEnabled,
                            onCheckedChange = { enabled ->
                                soundEnabled = enabled
                                TrainingFeedback.setSoundEnabled(enabled)
                            }
                        )
                        SettingsSwitchRow(
                            icon = AppIcons.Voice,
                            title = "倒数语音播报",
                            subtitle = "最后 5 秒用语音读出 5 / 4 / 3 / 2 / 1（跟随媒体音量）",
                            checked = voiceEnabled,
                            onCheckedChange = { enabled ->
                                voiceEnabled = enabled
                                TrainingFeedback.setVoiceEnabled(enabled)
                            }
                        )
                        SettingsSwitchRow(
                            icon = AppIcons.Clock,
                            title = "倒计时秒针声",
                            subtitle = "倒计时全程每秒一声轻滴答",
                            checked = tickEnabled,
                            onCheckedChange = { enabled ->
                                tickEnabled = enabled
                                TrainingFeedback.setTickEnabled(enabled)
                            }
                        )
                        SettingsSwitchRow(
                            icon = AppIcons.Vibrate,
                            title = "震动反馈",
                            subtitle = "与提示音同步震动，静音时也能感知",
                            checked = vibrationEnabled,
                            onCheckedChange = { enabled ->
                                vibrationEnabled = enabled
                                TrainingFeedback.setVibrationEnabled(enabled)
                            },
                            isLast = true
                        )
                    }
                }
            }

            // 训练计划
            item { SectionHeader(text = "训练计划") }
            item {
                LiteCard(modifier = Modifier.padding(horizontal = AppDimens.screenPadding)) {
                    Column(modifier = Modifier.padding(horizontal = AppDimens.s3, vertical = AppDimens.s1)) {
                        SettingsRow(
                            icon = AppIcons.Calendar,
                            title = "当前计划",
                            subtitle = planSummary,
                            onClick = { loadPlanSummary() }
                        )
                        SettingsRow(
                            icon = AppIcons.Package,
                            title = if (isPlanBusy) "处理中…" else "导入 8 周康复计划",
                            subtitle = "按阶段生成分组与动作，自动区分训练日与休息日",
                            onClick = {
                                if (isPlanBusy) return@SettingsRow
                                isPlanBusy = true
                                scope.launch {
                                    runCatching { data.plan.PlanRepository.importRehabPlan() }
                                        .onSuccess {
                                            loadStats(); loadPlanSummary()
                                            // 导入计划时自动开启每日提醒（默认 21:00）
                                            if (!reminderEnabled) {
                                                reminderEnabled = true
                                                data.plan.ReminderController.setEnabled(true, reminderHour, reminderMinute)
                                            }
                                        }
                                        .onFailure { planSummary = "导入失败：${it.message}" }
                                    isPlanBusy = false
                                }
                            }
                        )
                        SettingsRow(
                            icon = AppIcons.Delete,
                            title = "清除康复计划",
                            subtitle = "仅删除计划生成的分组与动作，打卡记录保留",
                            onClick = {
                                if (isPlanBusy) return@SettingsRow
                                isPlanBusy = true
                                scope.launch {
                                    runCatching { data.plan.PlanRepository.deletePlan() }
                                    loadStats()
                                    loadPlanSummary()
                                    isPlanBusy = false
                                }
                            },
                            isDestructive = true,
                            isLast = true
                        )
                    }
                }
            }

            // 数据与同步
            item { SectionHeader(text = "数据与同步") }
            item {
                LiteCard(modifier = Modifier.padding(horizontal = AppDimens.screenPadding)) {
                    Column(modifier = Modifier.padding(horizontal = AppDimens.s3, vertical = AppDimens.s1)) {
                        if (authService != null && authService.isLoggedIn) {
                            SettingsRow(
                                icon = AppIcons.Person,
                                title = "账号",
                                subtitle = authService.userEmail.ifEmpty { "未知" }
                            )
                            SettingsRow(
                                icon = if (isSyncing) AppIcons.Sync else AppIcons.Upload,
                                title = if (isSyncing) "同步中..." else "手动同步",
                                subtitle = when {
                                    isSyncing -> "正在上传下载数据"
                                    syncSuccess -> "上次同步成功"
                                    syncError != null -> "$syncError"
                                    else -> "点击立即同步数据到云端"
                                },
                                onClick = {
                                    if (!isSyncing) {
                                        isSyncing = true
                                        syncError = null
                                        syncSuccess = false
                                        scope.launch {
                                            try {
                                                val client = ApiClient()
                                                // 尝试拉取增量同步测试连接
                                                val result = client.pull(authService.token)
                                                result.fold(
                                                    onSuccess = {
                                                        syncSuccess = true
                                                        syncError = null
                                                    },
                                                    onFailure = { e ->
                                                        syncError = e.message ?: "同步失败"
                                                    }
                                                )
                                            } catch (e: Exception) {
                                                syncError = e.message ?: "连接失败"
                                            } finally {
                                                isSyncing = false
                                            }
                                        }
                                    }
                                }
                            )
                        } else {
                            SettingsRow(
                                icon = AppIcons.Key,
                                title = "未登录",
                                subtitle = "登录后可同步数据到云端"
                            )
                        }
                        SettingsRow(
                            icon = AppIcons.Link,
                            title = "服务器地址",
                            subtitle = ServerConfig.baseUrl
                        )
                        SettingsRow(
                            icon = AppIcons.Upload,
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
                        SettingsRow(
                            icon = AppIcons.Download,
                            title = "导入数据",
                            subtitle = "从 JSON 格式导入数据",
                            onClick = { showImportDialog = true }
                        )
                        SettingsRow(
                            icon = AppIcons.Delete,
                            title = "清除数据",
                            subtitle = "删除所有训练记录和分组",
                            onClick = { showClearDataDialog = true },
                            isDestructive = true,
                            isLast = true
                        )
                    }
                }
            }

            // 外观
            item { SectionHeader(text = "外观") }
            item {
                LiteCard(modifier = Modifier.padding(horizontal = AppDimens.screenPadding)) {
                    Column(modifier = Modifier.padding(horizontal = AppDimens.s3, vertical = AppDimens.s1)) {
                        SettingsSwitchRow(
                            icon = AppIcons.DarkMode,
                            title = "深色模式",
                            subtitle = if (isDarkMode) "已开启" else "未开启",
                            checked = isDarkMode,
                            onCheckedChange = { enabled ->
                                isDarkMode = enabled
                                onThemeChange?.invoke(enabled)
                            },
                            isLast = true
                        )
                    }
                }
            }

            // 关于
            item { SectionHeader(text = "关于") }
            item {
                LiteCard(modifier = Modifier.padding(horizontal = AppDimens.screenPadding)) {
                    Column(modifier = Modifier.padding(horizontal = AppDimens.s3, vertical = AppDimens.s1)) {
                        SettingsRow(
                            icon = AppIcons.Info,
                            title = "关于应用",
                            subtitle = "版本 1.0.0",
                            onClick = { showAboutDialog = true }
                        )
                        SettingsRow(
                            icon = AppIcons.Celebrate,
                            title = "评价我们",
                            subtitle = "在应用商店留下评价",
                            onClick = { },
                            isLast = true
                        )
                    }
                }
            }

            // 退出登录（独立危险操作卡）
            if (authService != null) {
                item {
                    Spacer(modifier = Modifier.height(AppDimens.s2))
                    LiteCard(
                        modifier = Modifier.padding(horizontal = AppDimens.screenPadding),
                        onClick = { showLogoutDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = AppDimens.s3,
                                vertical = AppDimens.s3
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBadge(
                                icon = AppIcons.Logout,
                                contentDescription = null,
                                tint = AppTheme.colors.danger,
                                container = AppTheme.colors.dangerSoft
                            )
                            Spacer(modifier = Modifier.width(AppDimens.s3))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "退出登录",
                                    style = MaterialTheme.typography.body1,
                                    fontWeight = AppTypeWeight.medium,
                                    color = AppTheme.colors.danger
                                )
                                Text(
                                    text = "退出后本地数据保留",
                                    style = MaterialTheme.typography.caption,
                                    color = AppTheme.colors.inkSub
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(AppDimens.s8)) }
        }
    }

    // 关于对话框
    if (showAboutDialog) {
        AppDialog(
            onDismissRequest = { showAboutDialog = false },
            title = "AI SportTask",
            confirmLabel = "知道了",
            onConfirm = { showAboutDialog = false }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppIcon(
                    imageVector = AppIcons.Training,
                    contentDescription = null,
                    size = AppDimens.iconHero,
                    tint = AppTheme.colors.brand
                )
                Spacer(modifier = Modifier.height(AppDimens.s4))
                Text(
                    text = "版本 1.0.0",
                    style = MaterialTheme.typography.body1,
                    color = AppTheme.colors.ink
                )
                Text(
                    text = "一款智能健身任务管理应用",
                    style = MaterialTheme.typography.body2,
                    color = AppTheme.colors.inkSub
                )
            }
        }
    }

    // 清除数据确认对话框
    if (showClearDataDialog) {
        AppDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = "确认清除",
            confirmLabel = "清除",
            confirmDanger = true,
            busy = isClearing,
            dismissLabel = "取消",
            onDismiss = { showClearDataDialog = false },
            onConfirm = {
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
            Text(
                text = "确定要删除以下所有数据吗？此操作无法撤销。",
                style = MaterialTheme.typography.body1,
                color = AppTheme.colors.inkSub
            )
            Spacer(modifier = Modifier.height(AppDimens.s2))
            listOf(
                "$totalGroups 个训练分组",
                "$totalActions 个训练动作",
                "$totalCheckins 条打卡记录"
            ).forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.body2,
                    color = AppTheme.colors.inkSub
                )
            }
        }
    }

    // 导出数据对话框
    if (showExportDialog) {
        AppDialog(
            onDismissRequest = { showExportDialog = false },
            title = "导出数据",
            confirmLabel = "关闭",
            onConfirm = { showExportDialog = false }
        ) {
            Text(
                text = "数据已导出为 JSON 格式。",
                style = MaterialTheme.typography.body1,
                color = AppTheme.colors.inkSub
            )
            Spacer(modifier = Modifier.height(AppDimens.s1))
            Text(
                text = "共 $totalGroups 个分组、$totalActions 个动作、$totalCheckins 条记录",
                style = MaterialTheme.typography.body2,
                color = AppTheme.colors.inkMuted
            )
            Spacer(modifier = Modifier.height(AppDimens.s4))
            AppDialogTextField(
                value = exportJson,
                onValueChange = {},
                readOnly = true,
                minHeight = AppDimens.dialogTextAreaHeight
            )
        }
    }

    // 导入数据对话框
    if (showImportDialog) {
        val closeImport = {
            showImportDialog = false
            importJson = ""
            importResult = null
        }
        AppDialog(
            onDismissRequest = closeImport,
            title = "导入数据",
            confirmLabel = "导入",
            confirmEnabled = importJson.isNotBlank(),
            busy = isImporting,
            dismissLabel = "取消",
            onDismiss = closeImport,
            onConfirm = {
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
            }
        ) {
            Text(
                text = "请粘贴 JSON 数据：",
                style = MaterialTheme.typography.body1,
                color = AppTheme.colors.inkSub
            )
            Spacer(modifier = Modifier.height(AppDimens.s2))
            AppDialogTextField(
                value = importJson,
                onValueChange = { importJson = it },
                placeholder = "粘贴 JSON 数据…",
                minHeight = AppDimens.dialogTextAreaHeight
            )

            if (importResult != null) {
                Spacer(modifier = Modifier.height(AppDimens.s4))
                if (importResult!!.success) {
                    Text(
                        text = "导入成功！",
                        style = MaterialTheme.typography.body1,
                        color = AppTheme.colors.success
                    )
                    Text(
                        text = "分组: ${importResult!!.groupsImported}, 动作: ${importResult!!.actionsImported}, 打卡: ${importResult!!.checkinsImported}",
                        style = MaterialTheme.typography.body2,
                        color = AppTheme.colors.inkSub
                    )
                } else {
                    Text(
                        text = "导入失败: ${importResult!!.errorMessage}",
                        style = MaterialTheme.typography.body1,
                        color = AppTheme.colors.danger
                    )
                }
            }
        }
    }

    // 时间选择器对话框
    if (showTimePickerDialog) {
        TimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onConfirm = { hour, minute ->
                reminderHour = hour
                reminderMinute = minute
                data.plan.ReminderController.setEnabled(reminderEnabled, hour, minute)
                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }

    // 成功提示
    if (showSuccessMessage) {
        AppConfirmDialog(
            title = "操作成功",
            message = "数据已清除。",
            onDismissRequest = { showSuccessMessage = false },
            onConfirm = { showSuccessMessage = false },
            confirmLabel = "知道了",
            dismissLabel = null
        )
    }

    // 退出登录确认对话框
    if (showLogoutDialog) {
        AppConfirmDialog(
            title = "退出登录",
            message = "退出后本地数据保留，下次登录可恢复同步。",
            onDismissRequest = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                authService?.logout()
                onLogout?.invoke()
            },
            confirmLabel = "退出",
            danger = true
        )
    }
}

// 设置行：图标容器 + 标题/副标题 + 可选右侧内容，行间细分隔线
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    isDestructive: Boolean = false,
    isLast: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                )
                .padding(vertical = AppDimens.s3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val titleColor = if (isDestructive) AppTheme.colors.danger else AppTheme.colors.ink
            IconBadge(
                icon = icon,
                contentDescription = null,
                tint = if (isDestructive) AppTheme.colors.danger else AppTheme.colors.brandStrong,
                container = if (isDestructive) AppTheme.colors.dangerSoft else AppTheme.colors.brandSoft
            )
            Spacer(modifier = Modifier.width(AppDimens.s3))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.body1,
                    fontWeight = AppTypeWeight.medium,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(AppDimens.s1))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.inkSub
                )
            }
            if (onClick != null) {
                AppIcon(
                    imageVector = AppIcons.Chevron,
                    contentDescription = null,
                    tint = AppTheme.colors.inkMuted,
                    size = AppDimens.iconSmall
                )
            }
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.divider)
                    .background(AppTheme.colors.outline)
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isLast: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = AppDimens.s3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(AppDimens.s3))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.body1,
                    fontWeight = AppTypeWeight.medium,
                    color = AppTheme.colors.ink
                )
                Spacer(modifier = Modifier.height(AppDimens.s1))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.inkSub
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppTheme.colors.onBrand,
                    checkedTrackColor = AppTheme.colors.brand,
                    uncheckedThumbColor = AppTheme.colors.surface,
                    uncheckedTrackColor = AppTheme.colors.surfaceAlt
                )
            )
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.divider)
                    .background(AppTheme.colors.outline)
            )
        }
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

    AppDialog(
        onDismissRequest = onDismiss,
        title = "选择提醒时间",
        subtitle = "设置每天的训练提醒时刻",
        confirmLabel = "确定",
        onConfirm = { onConfirm(selectedHour, selectedMinute) },
        dismissLabel = "取消",
        onDismiss = onDismiss
    ) {
        // 顶部留白把标题与上箭头拉开，避免旧版 AlertDialog 的标题/控件重叠
        Spacer(modifier = Modifier.height(AppDimens.s2))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeWheelColumn(
                value = selectedHour,
                onIncrease = { selectedHour = (selectedHour + 1) % 24 },
                onDecrease = { selectedHour = if (selectedHour > 0) selectedHour - 1 else 23 }
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.h2,
                fontWeight = AppTypeWeight.emphasis,
                color = AppTheme.colors.inkMuted,
                modifier = Modifier.padding(horizontal = AppDimens.s4)
            )
            TimeWheelColumn(
                value = selectedMinute,
                onIncrease = { selectedMinute = (selectedMinute + 5) % 60 },
                onDecrease = { selectedMinute = if (selectedMinute >= 5) selectedMinute - 5 else 55 }
            )
        }
        Spacer(modifier = Modifier.height(AppDimens.s2))
    }
}

/** 时间选择器的单列：上箭头 / 数字块 / 下箭头 */
@Composable
private fun TimeWheelColumn(
    value: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onIncrease) {
            AppIcon(
                imageVector = AppIcons.ChevronUp,
                contentDescription = "增加",
                tint = AppTheme.colors.brand,
                size = AppDimens.icon
            )
        }
        Surface(
            shape = AppShapes.button,
            color = AppTheme.colors.surfaceAlt
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.h2,
                fontWeight = AppTypeWeight.emphasis,
                color = AppTheme.colors.ink,
                modifier = Modifier.padding(
                    horizontal = AppDimens.s4,
                    vertical = AppDimens.s2
                )
            )
        }
        IconButton(onClick = onDecrease) {
            AppIcon(
                imageVector = AppIcons.ChevronDown,
                contentDescription = "减少",
                tint = AppTheme.colors.brand,
                size = AppDimens.icon
            )
        }
    }
}
