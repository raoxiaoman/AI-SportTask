package ui
import ui.AppIcon
import ui.AppIcons
import ui.theme.*


import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.Achievement

/**
 * 成就弹窗组件 — 训练完成后展示新解锁成就
 */
@Composable
fun AchievementDialog(
    achievements: List<Achievement>,
    onDismiss: () -> Unit
) {
    if (achievements.isEmpty()) return

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppShapes.dialog,
        title = {
            Text(
                text = "新成就解锁！",
                style = MaterialTheme.typography.h5,
                fontWeight = AppTypeWeight.emphasis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimens.s4)
            ) {
                achievements.forEachIndexed { index, ach ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 成就图标
                        Text(
                            text = ach.icon,
                            fontSize = AppTypeScale.achievementIcon,
                            modifier = Modifier.width(AppDimens.buttonHeight)
                        )

                        Spacer(Modifier.width(AppDimens.s2))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ach.title,
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = AppTypeWeight.emphasis,
                                color = MaterialTheme.colors.primary
                            )
                            Text(
                                text = ach.description,
                                style = MaterialTheme.typography.caption,
                                color = AppTheme.colors.inkSub
                            )
                        }
                    }

                    if (index < achievements.size - 1) {
                        Divider(color = MaterialTheme.colors.primary.copy(alpha = 0.1f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("太棒了！继续加油")
            }
        }
    )
}
