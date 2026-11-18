package pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import data.ImportResult
import data.plan.TemplateRepository
import data.plan.TrainingTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.AppConfirmDialog
import ui.AppDialog
import ui.AppToast
import ui.theme.*

/**
 * 方案模板库：优先展示服务端最新模板，离线时回退内置。
 * 导入是纯追加动作，不会覆盖用户已有分组或当前激活的康复计划状态。
 */
@Composable
fun TemplateLibraryDialog(
    onDismiss: () -> Unit,
    onImported: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var templates by remember { mutableStateOf<List<TrainingTemplate>>(emptyList()) }
    var fromServer by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var importingId by remember { mutableStateOf<String?>(null) }
    var confirmTemplate by remember { mutableStateOf<TrainingTemplate?>(null) }
    var result by remember { mutableStateOf<ImportResult?>(null) }

    LaunchedEffect(Unit) {
        val loaded = TemplateRepository.load()
        templates = loaded.templates
        fromServer = loaded.fromServer
        isLoading = false
    }

    AppDialog(
        onDismissRequest = onDismiss,
        title = "方案模板库",
        subtitle = when {
            isLoading -> "正在获取模板…"
            fromServer -> "来自服务器 · 共 ${templates.size} 套方案"
            else -> "内置模板 · 离线也能导入"
        },
        dismissLabel = "关闭",
        onDismiss = onDismiss
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colors.brand)
            }
            return@AppDialog
        }

        if (templates.isEmpty()) {
            Text(
                text = "暂无可用方案，请稍后再试。",
                style = MaterialTheme.typography.body1,
                color = AppTheme.colors.inkSub
            )
            return@AppDialog
        }

        result?.let { r ->
            Spacer(modifier = Modifier.height(AppDimens.s2))
            Text(
                text = if (r.success)
                    "已导入 ${r.groupsImported} 个分组、${r.actionsImported} 个动作"
                else "导入失败：${r.errorMessage ?: "未知错误"}",
                style = MaterialTheme.typography.caption,
                color = if (r.success) AppTheme.colors.success else AppTheme.colors.danger
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.s2))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = AppDimens.dialogTextAreaHeight * 2)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.s2)
        ) {
            templates.forEach { template ->
                Surface(
                    shape = AppShapes.card,
                    color = AppTheme.colors.surfaceAlt,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppDimens.s3)) {
                        Text(
                            text = template.name,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = AppTypeWeight.emphasis,
                            color = AppTheme.colors.ink
                        )
                        Spacer(modifier = Modifier.height(AppDimens.s1))
                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.body2,
                            color = AppTheme.colors.inkSub
                        )
                        Spacer(modifier = Modifier.height(AppDimens.s1))
                        Text(
                            text = "${template.phases.size} 阶段 · ${template.actionCount} 动作 · ${template.totalDays} 天 · 单次约 ${template.durationMinutes} 分钟",
                            style = MaterialTheme.typography.caption,
                            color = AppTheme.colors.inkMuted
                        )
                        Spacer(modifier = Modifier.height(AppDimens.s2))
                        Button(
                            onClick = { confirmTemplate = template },
                            enabled = importingId == null,
                            shape = AppShapes.button,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = AppTheme.colors.brandStrong,
                                contentColor = AppTheme.colors.onBrand,
                                disabledBackgroundColor = AppTheme.colors.surfaceAlt,
                                disabledContentColor = AppTheme.colors.inkMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppDimens.dialogButtonHeight)
                        ) {
                            Text(
                                text = if (importingId == template.id) "导入中…" else "导入这套方案",
                                style = MaterialTheme.typography.subtitle2
                            )
                        }
                    }
                }
            }
        }
    }

    confirmTemplate?.let { template ->
        AppConfirmDialog(
            title = "导入方案",
            message = "将「${template.name}」导入为 ${template.phases.size} 个新分组，不影响现有数据。",
            confirmLabel = "导入",
            busy = importingId != null,
            onDismissRequest = { confirmTemplate = null },
            onConfirm = {
                importingId = template.id
                scope.launch {
                    val imported = withContext(Dispatchers.Default) {
                        TemplateRepository.importTemplate(template)
                    }
                    result = imported
                    importingId = null
                    confirmTemplate = null
                    if (imported.success) {
                        // 关闭确认框后弹全局提示，避免内联文本容易被忽略
                        onDismiss()
                        AppToast.show("已导入「${template.name}」：${imported.groupsImported} 个分组、${imported.actionsImported} 个动作")
                        onImported()
                    }
                }
            }
        )
    }
}
