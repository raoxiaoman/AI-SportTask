package ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 系统栏外观副作用：把状态栏/导航栏颜色设为当前主题对应色，
 * 并随主题切换系统栏图标的深浅，消除「系统灰条」与应用割裂的问题。
 * 注意：刻意不用全量 edge-to-edge——页面内部还有嵌套 Scaffold，
 * 启用后会重复叠加状态栏内边距；用「同色状态栏」达到同等视觉效果且零布局风险。
 */
@Composable
expect fun SystemBarAppearanceEffect(
    isDark: Boolean,
    statusBarColor: Color,
    navigationBarColor: Color
)
