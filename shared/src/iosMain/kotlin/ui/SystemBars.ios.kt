package ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun SystemBarAppearanceEffect(
    isDark: Boolean,
    statusBarColor: Color,
    navigationBarColor: Color
) {
    // iOS 状态栏外观由 Info.plist 配置控制，暂无实现
}
