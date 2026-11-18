package pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import data.plan.Scheme
import data.plan.SchemeRepository
import ui.AppDialog
import ui.theme.*

/**
 * 当前方案选择：列出已导入的方案 + 「全部分组」，选中后切换并回调刷新。
 */
@Composable
fun SchemePickerDialog(
    currentId: String?,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val schemes = runCatching { SchemeRepository.all() }.getOrDefault(emptyList())

    AppDialog(
        onDismissRequest = onDismiss,
        title = "选择当前方案",
        subtitle = "首页将只显示所选方案的训练分组",
        dismissLabel = "关闭",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = AppDimens.dialogTextAreaHeight * 2)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.s2)
        ) {
            SchemeOption(
                title = "全部分组",
                subtitle = "不做筛选，显示所有训练分组",
                selected = currentId == null,
                onClick = {
                    SchemeRepository.switchTo(null)
                    onChanged(); onDismiss()
                }
            )
            schemes.forEach { scheme ->
                SchemeOption(
                    title = scheme.name,
                    subtitle = "${scheme.groupIds.size} 个分组 · ${scheme.startDate} 开始" +
                        if (scheme.isRehab) " · 康复计划" else "",
                    selected = currentId == scheme.id,
                    onClick = {
                        SchemeRepository.switchTo(scheme.id)
                        onChanged(); onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun SchemeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) AppTheme.colors.brandSoft else AppTheme.colors.surfaceAlt,
                shape = AppShapes.card
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.s3, vertical = AppDimens.s3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = if (selected) AppTypeWeight.emphasis else FontWeight.Normal,
                color = if (selected) AppTheme.colors.brandDeep else AppTheme.colors.ink
            )
            Spacer(modifier = Modifier.height(AppDimens.s1))
            Text(text = subtitle, style = MaterialTheme.typography.caption, color = AppTheme.colors.inkSub)
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = AppTheme.colors.brandStrong,
                unselectedColor = AppTheme.colors.inkMuted
            )
        )
    }
}
