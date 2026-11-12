@file:OptIn(ExperimentalMaterialApi::class)
package ui
import ui.AppIcon
import ui.AppIcons
import ui.theme.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全屏加载指示器
 */
@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * 页面内嵌标题行：替代厚重的 Material TopAppBar。
 * 左侧为「小字眉行 + 大标题」，右侧为任意 trailing 内容（如图标容器）。
 */
@Composable
fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding, vertical = AppDimens.s3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.inkMuted
                )
                Spacer(modifier = Modifier.height(AppDimens.s1))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.h3,
                fontWeight = AppTypeWeight.emphasis,
                color = AppTheme.colors.ink
            )
        }
        trailing?.invoke()
    }
}

/**
 * 小节标题：紧凑、中性色，配合 LiteCard 使用。
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding, vertical = AppDimens.s2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = AppTypeWeight.emphasis,
            color = AppTheme.colors.ink
        )
        trailing?.invoke()
    }
}

/**
 * 轻量内容卡：白底 + 细边框，设计稿里「lite」行卡的标准容器。
 */
@Composable
fun LiteCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.chip,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val baseModifier = modifier.fillMaxWidth()
    if (onClick != null) {
        Card(
            modifier = baseModifier,
            shape = shape,
            backgroundColor = AppTheme.colors.surface,
            elevation = AppDimens.zero,
            border = BorderStroke(AppDimens.divider, AppTheme.colors.outline),
            onClick = onClick
        ) { Row(content = content) }
    } else {
        Card(
            modifier = baseModifier,
            shape = shape,
            backgroundColor = AppTheme.colors.surface,
            elevation = AppDimens.zero,
            border = BorderStroke(AppDimens.divider, AppTheme.colors.outline)
        ) { Row(content = content) }
    }
}

/**
 * 图标容器：28dp 圆角方形底 + 居中矢量图标，列表行/页头的标准图标形态。
 */
@Composable
fun IconBadge(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.brandStrong,
    container: Color = AppTheme.colors.brandSoft,
    size: Dp = AppDimens.iconBadge
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color = container, shape = AppShapes.chip),
        contentAlignment = Alignment.Center
    ) {
        AppIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            size = AppDimens.iconSmall
        )
    }
}

/**
 * 空状态占位组件
 *
 * @param icon 空状态矢量图标
 * @param title 主标题
 * @param subtitle 副标题（可选）
 * @param actionText 操作按钮文字（可选）
 * @param onAction 操作按钮点击回调
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    actionText: String = "",
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 大图标
            AppIcon(
                imageVector = icon,
                contentDescription = null,
                size = AppDimens.iconHero,
                tint = AppTheme.colors.brand,
                modifier = Modifier.padding(bottom = AppDimens.s4)
            )

            // 标题
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = AppTypeWeight.medium,
                color = AppTheme.colors.inkSub,
                textAlign = TextAlign.Center
            )

            // 副标题
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(AppDimens.s2))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.body2,
                    color = AppTheme.colors.inkMuted,
                    textAlign = TextAlign.Center
                )
            }

            // 操作按钮
            if (actionText.isNotEmpty() && onAction != null) {
                Spacer(Modifier.height(AppDimens.s6))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
                ) {
                    Text(actionText)
                }
            }
        }
    }
}
