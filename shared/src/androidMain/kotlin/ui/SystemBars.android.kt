package ui

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun SystemBarAppearanceEffect(
    isDark: Boolean,
    statusBarColor: Color,
    navigationBarColor: Color
) {
    val view = LocalView.current
    DisposableEffect(view, isDark, statusBarColor, navigationBarColor) {
        var ctx: android.content.Context = view.context
        var activity: Activity? = null
        while (ctx is ContextWrapper) {
            if (ctx is Activity) { activity = ctx; break }
            ctx = ctx.baseContext
        }
        val window = activity?.window
        if (window != null) {
            window.statusBarColor = statusBarColor.toArgb()
            window.navigationBarColor = navigationBarColor.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
        onDispose { }
    }
}
