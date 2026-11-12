package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import data.Achievement
import ui.theme.*

/**
 * 成就弹窗 — 训练完成后展示新解锁成就。
 * 容器、标题与按钮统一走 [AppDialog]，内容只负责成就列表。
 */
@Composable
fun AchievementDialog(
    achievements: List<Achievement>,
    onDismiss: () -> Unit
) {
    if (achievements.isEmpty()) return

    AppDialog(
        onDismissRequest = onDismiss,
        title = "新成就解锁！",
        subtitle = "坚持训练，收获了 ${achievements.size} 枚新徽章",
        confirmLabel = "太棒了，继续加油",
        onConfirm = onDismiss
    ) {
        achievements.forEachIndexed { index, achievement ->
            Surface(
                shape = AppShapes.button,
                color = AppTheme.colors.brandSoft
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimens.s3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = achievement.icon,
                        fontSize = AppTypeScale.achievementIcon,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(AppDimens.buttonHeight)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.s3))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = achievement.title,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = AppTypeWeight.emphasis,
                            color = AppTheme.colors.ink
                        )
                        Text(
                            text = achievement.description,
                            style = MaterialTheme.typography.body2,
                            color = AppTheme.colors.inkSub
                        )
                    }
                    AppIcon(
                        imageVector = AppIcons.Check,
                        contentDescription = null,
                        tint = AppTheme.colors.success,
                        size = AppDimens.iconSmall
                    )
                }
            }
            if (index < achievements.size - 1) {
                Spacer(modifier = Modifier.height(AppDimens.s2))
            }
        }
    }
}
