package pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import data.GroupItem
import data.SportTaskRepository
import data.plan.Scheme
import data.plan.SchemeRepository
import kotlinx.coroutines.launch
import ui.AppConfirmDialog
import ui.AppDialog
import ui.AppDialogTextField
import ui.AppIcon
import ui.AppIcons
import ui.AppToast
import ui.EmptyState
import ui.IconBadge
import ui.LiteCard
import ui.PageHeader
import ui.theme.*

/**
 * 方案 Tab：方案成为一等公民。
 * 一级：方案列表（新建方案 / 模板库 / 设为当前 / 删除 / 进入分组）。
 * 二级：方案内分组列表（GroupListScreen，带 schemeId，新建分组自动归属）。
 * 三级：分组详情（复用 GroupDetailScreen）。
 */
@Composable
fun SchemeScreen(onSchemeChanged: (() -> Unit)? = null, refreshKey: Int = 0) {
    var selectedScheme by remember { mutableStateOf<Scheme?>(null) }
    var showTemplateLibrary by remember { mutableStateOf(false) }
    var revision by remember { mutableStateOf(0) }

    // 外部（设置页导入/切换）或模板库导入后强制刷新列表
    LaunchedEffect(refreshKey) { revision++ }

    val currentScheme = selectedScheme
    if (currentScheme == null) {
        SchemeListScreen(
            revision = revision,
            onOpenScheme = { selectedScheme = it },
            onSchemeChanged = {
                revision++
                onSchemeChanged?.invoke()
            },
            onOpenTemplateLibrary = { showTemplateLibrary = true }
        )
        if (showTemplateLibrary) {
            TemplateLibraryDialog(
                onDismiss = { showTemplateLibrary = false },
                onImported = {
                    revision++
                    onSchemeChanged?.invoke()
                }
            )
        }
    } else {
        // 方案内分组列表；删除方案时从列表页移除的方案对象在此已失效，返回时丢弃
        SchemeGroupsScreen(
            scheme = currentScheme,
            revision = revision,
            onBack = { selectedScheme = null },
            onGroupsChanged = { revision++ }
        )
    }
}

/** 一级：方案列表 */
@Composable
private fun SchemeListScreen(
    revision: Int,
    onOpenScheme: (Scheme) -> Unit,
    onSchemeChanged: () -> Unit,
    onOpenTemplateLibrary: () -> Unit
) {
    var schemes by remember { mutableStateOf<List<Scheme>>(emptyList()) }
    var currentId by remember { mutableStateOf<String?>(null) }
    var groupStats by remember { mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Scheme?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        val allGroups = runCatching { SportTaskRepository.loadGroupItemsSync() }.getOrDefault(emptyList())
        val state = runCatching { SchemeRepository.load() }.getOrDefault(data.plan.SchemeState())
        schemes = state.schemes
        currentId = state.currentId
        groupStats = state.schemes.associate { scheme ->
            val groups = allGroups.filter { it.id in scheme.groupIds }
            scheme.id to (groups.size to groups.sumOf { it.actionCount })
        }
    }

    LaunchedEffect(revision) { refresh() }

    Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        if (schemes.isEmpty()) {
            Column {
                SchemeListHeader(onCreate = { showCreateDialog = true }, onTemplate = onOpenTemplateLibrary)
                EmptyState(
                    icon = AppIcons.Package,
                    title = "还没有训练方案",
                    subtitle = "从模板库导入一套方案，或新建一个空方案自己安排分组",
                    actionText = "浏览方案模板库",
                    onAction = onOpenTemplateLibrary
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(AppDimens.s2)) {
                item {
                    SchemeListHeader(onCreate = { showCreateDialog = true }, onTemplate = onOpenTemplateLibrary)
                }
                items(schemes, key = { it.id }) { scheme ->
                    val isCurrent = scheme.id == currentId
                    val (groupCount, actionCount) = groupStats[scheme.id] ?: (0 to 0)
                    SchemeCard(
                        scheme = scheme,
                        isCurrent = isCurrent,
                        groupCount = groupCount,
                        actionCount = actionCount,
                        onOpen = { onOpenScheme(scheme) },
                        onSetCurrent = {
                            SchemeRepository.switchTo(scheme.id)
                            AppToast.show("已切换为「${scheme.name}」")
                            refresh()
                            onSchemeChanged()
                        },
                        onDelete = { pendingDelete = scheme }
                    )
                }
                item { Spacer(modifier = Modifier.height(AppDimens.s8)) }
            }
        }

        if (showCreateDialog) {
            val close = { showCreateDialog = false; newName = "" }
            AppDialog(
                onDismissRequest = close,
                title = "新建方案",
                confirmLabel = "创建",
                confirmEnabled = newName.isNotBlank(),
                dismissLabel = "取消",
                onDismiss = close,
                onConfirm = {
                    if (newName.isNotBlank()) {
                        val scheme = SchemeRepository.create(newName.trim(), todayDateString())
                        AppToast.show("已创建方案「${scheme.name}」")
                        close()
                        refresh()
                        onSchemeChanged()
                        onOpenScheme(scheme)
                    }
                }
            ) {
                Text("方案是一组训练分组的集合，创建后可在方案内添加分组。", style = MaterialTheme.typography.body2, color = AppTheme.colors.inkSub)
                Spacer(modifier = Modifier.height(AppDimens.s2))
                AppDialogTextField(value = newName, onValueChange = { newName = it }, label = "方案名称", singleLine = true)
            }
        }

        pendingDelete?.let { scheme ->
            AppConfirmDialog(
                title = "删除方案",
                message = "确定删除「${scheme.name}」吗？方案内的分组和动作会一并删除，打卡记录保留。此操作无法撤销。",
                onDismissRequest = { pendingDelete = null },
                onConfirm = {
                    scope.launch {
                        val groupIds = SchemeRepository.groupIdsOf(scheme.id)
                        groupIds.forEach { id ->
                            SportTaskRepository.deleteActionsInGroup(id)
                            SportTaskRepository.deleteGroup(id)
                        }
                        SchemeRepository.remove(scheme.id)
                        pendingDelete = null
                        AppToast.show("已删除方案「${scheme.name}」")
                        refresh()
                        onSchemeChanged()
                    }
                },
                confirmLabel = "删除",
                danger = true
            )
        }
    }
}

@Composable
private fun SchemeListHeader(onCreate: () -> Unit, onTemplate: () -> Unit) {
    PageHeader(
        title = "训练方案",
        eyebrow = "选择或创建一套方案开始训练",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(AppDimens.touchTarget).clickable(onClick = onTemplate),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(AppIcons.Package, contentDescription = "方案模板库", tint = AppTheme.colors.brandStrong)
                }
                Spacer(modifier = Modifier.width(AppDimens.s2))
                Box(
                    modifier = Modifier.size(AppDimens.touchTarget).clickable(onClick = onCreate),
                    contentAlignment = Alignment.Center
                ) {
                    IconBadge(icon = AppIcons.Add, contentDescription = "新建方案")
                }
            }
        }
    )
}

@Composable
private fun SchemeCard(
    scheme: Scheme,
    isCurrent: Boolean,
    groupCount: Int,
    actionCount: Int,
    onOpen: () -> Unit,
    onSetCurrent: () -> Unit,
    onDelete: () -> Unit
) {
    val sourceLabel = when {
        scheme.isRehab -> "8 周康复计划"
        scheme.isCustom -> "自建方案"
        else -> "模板导入"
    }
    LiteCard(
        modifier = Modifier.padding(horizontal = AppDimens.screenPadding),
        // 卡片主体即切换入口：非当前方案点一下就启用，当前方案点一下进详情
        onClick = if (isCurrent) onOpen else onSetCurrent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.s3, vertical = AppDimens.s3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = if (scheme.isRehab) AppIcons.Training else AppIcons.Folder,
                contentDescription = null,
                tint = if (isCurrent) AppTheme.colors.brandStrong else AppTheme.colors.inkSub,
                container = if (isCurrent) AppTheme.colors.brandSoft else AppTheme.colors.surfaceAlt
            )
            Spacer(modifier = Modifier.width(AppDimens.s3))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = scheme.name,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = AppTypeWeight.emphasis,
                        color = AppTheme.colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(AppDimens.s2))
                        Text(
                            text = "使用中",
                            style = MaterialTheme.typography.caption,
                            color = AppTheme.colors.brandDeep
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppDimens.s1))
                Text(
                    text = "$sourceLabel · $groupCount 个分组 · $actionCount 个动作 · ${scheme.startDate} 开始",
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.inkSub
                )
                if (!isCurrent) {
                    Spacer(modifier = Modifier.height(AppDimens.s2))
                    Row(
                        modifier = Modifier
                            .background(color = AppTheme.colors.brandSoft, shape = AppShapes.chip)
                            .clickable(onClick = onSetCurrent)
                            .padding(horizontal = AppDimens.s2, vertical = AppDimens.s1),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            AppIcons.Check,
                            contentDescription = null,
                            tint = AppTheme.colors.brandDeep,
                            size = AppDimens.iconSmall
                        )
                        Spacer(modifier = Modifier.width(AppDimens.s1))
                        Text(
                            text = "设为当前方案",
                            style = MaterialTheme.typography.caption,
                            color = AppTheme.colors.brandDeep
                        )
                    }
                }
            }
            Box(
                modifier = Modifier.size(AppDimens.touchTarget).clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(AppIcons.Delete, contentDescription = "删除方案", tint = AppTheme.colors.inkMuted, size = AppDimens.iconSmall)
            }
            Box(
                modifier = Modifier.size(AppDimens.touchTarget).clickable(onClick = onOpen),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(AppIcons.Chevron, contentDescription = "进入方案", tint = AppTheme.colors.inkMuted, size = AppDimens.iconSmall)
            }
        }
    }
}
