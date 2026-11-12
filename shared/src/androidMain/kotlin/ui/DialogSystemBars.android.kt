package ui

import android.util.Log
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

@Composable
actual fun DialogSystemBarAppearanceEffect(
    isDark: Boolean,
    statusBarColor: Color,
    navigationBarColor: Color
) {
    val view = LocalView.current
    DisposableEffect(view, isDark, statusBarColor, navigationBarColor) {
        // 组合刚建立时 DecorView 可能尚未 attach，post 到下一帧重试几次
        var disposed = false
        var attempts = 0

        fun applyStyle() {
            if (disposed) return
            val dialogWindow = view.findDialogWindow()
            if (dialogWindow == null) {
                if (attempts++ < 5) view.post { applyStyle() }
                return
            }
            // 全屏 Dialog 不需要背后的遮罩；FLAG_DIM_BEHIND 会把系统栏一起压暗，
            // 这正是「状态栏/导航栏变深灰条」的根因。清掉后再同步主题色。
            dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialogWindow.setDimAmount(0f)
            dialogWindow.statusBarColor = statusBarColor.toArgb()
            dialogWindow.navigationBarColor = navigationBarColor.toArgb()
            val controller = WindowCompat.getInsetsController(dialogWindow, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
            Log.d("SportTaskBars", "dialog window styled: dim=0, status=$statusBarColor")
        }

        applyStyle()
        onDispose { disposed = true }
    }
}

/** 沿 View 树向上找到 Compose 为 Dialog 提供的 Window 持有者 */
private fun View.findDialogWindow(): Window? {
    var current: ViewParent? = parent
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent
    }
    return null
}
