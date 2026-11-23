package ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 桌面端无 Android 系统栏概念，空实现 */
@Composable
actual fun SystemBarAppearanceEffect(isDark: Boolean, statusBarColor: Color, navigationBarColor: Color) {}

/** 桌面端 Dialog 不创建独立系统 Window，空实现 */
@Composable
actual fun DialogSystemBarAppearanceEffect(isDark: Boolean, statusBarColor: Color, navigationBarColor: Color) {}
