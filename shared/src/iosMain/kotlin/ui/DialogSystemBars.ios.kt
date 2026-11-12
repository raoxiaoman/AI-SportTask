package ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** iOS 的 Compose Dialog 不创建独立 Window，系统栏沿用宿主页面，无需额外处理。 */
@Composable
actual fun DialogSystemBarAppearanceEffect(
    isDark: Boolean,
    statusBarColor: Color,
    navigationBarColor: Color
) = Unit
