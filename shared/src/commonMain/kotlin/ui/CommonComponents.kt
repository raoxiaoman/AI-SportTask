package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
 * 空状态占位组件
 *
 * @param icon 大图标 Emoji
 * @param title 主标题
 * @param subtitle 副标题（可选）
 * @param actionText 操作按钮文字（可选）
 * @param onAction 操作按钮点击回调
 */
@Composable
fun EmptyState(
    icon: String,
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
            Text(
                text = icon,
                fontSize = 64.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 标题
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            // 副标题
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.body2,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }

            // 操作按钮
            if (actionText.isNotEmpty() && onAction != null) {
                Spacer(Modifier.height(20.dp))
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
