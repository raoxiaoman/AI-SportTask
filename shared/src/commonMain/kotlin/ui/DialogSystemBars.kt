package ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 修正 Compose `Dialog` 独立 Window 的系统栏外观。
 *
 * 背景：`Dialog` 在 Android 上会创建自己的 Window，其状态栏/导航栏默认由系统主题绘制，
 * 与 Activity Window 的 [SystemBarAppearanceEffect] 无关。因此全屏 Dialog 页面会出现
 * 深灰色状态栏与导航栏，视觉上像「不是 edge-to-edge」。
 *
 * 这里把 Dialog Window 的系统栏颜色与图标深浅同步到当前主题，使其与主页面完全一致。
 * iOS 的 Dialog 不创建独立 Window，无需处理。
 */
@Composable
expect fun DialogSystemBarAppearanceEffect(
    isDark: Boolean,
    statusBarColor: Color,
    navigationBarColor: Color
)
