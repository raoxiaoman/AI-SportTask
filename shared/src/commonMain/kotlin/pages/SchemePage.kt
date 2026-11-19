package pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                            // 单选范式：点当前项为空操作，不重复提示
                            if (!isCurrent) {
                                SchemeRepository.switchTo(scheme.id)
                                AppToast.show("已切换为「${scheme.name}」")
                                refresh()
                                onSchemeChanged()
                            }
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
        // 单选范式：点卡片主体即选中为当前方案；点箭头进详情。职责单一，不再有第二个"设为当前"入口
        onClick = onSetCurrent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.s3, vertical = AppDimens.s3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 单选圆点：实心=当前方案，空心=未选中，一眼区分当前是哪一个
            SchemeRadio(selected = isCurrent)
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
                    text = "$sourceLabel · $groupCount 个分组 · $actionCount 个动作",
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.inkSub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

/** 单选指示器：实心圆=当前方案，空心圆=未选中。语义明确，无 ✓ 歧义。 */
@Composable
private fun SchemeRadio(selected: Boolean) {
    val ring = if (selected) AppTheme.colors.brandStrong else AppTheme.colors.inkMuted
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(AppDimens.hairline, ring, CircleShape)
            .padding(AppDimens.s1)
            .semantics { contentDescription = if (selected) "当前方案" else "未选中方案" },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(modifier = Modifier.size(12.dp).background(ring, CircleShape))
        }
    }
}
