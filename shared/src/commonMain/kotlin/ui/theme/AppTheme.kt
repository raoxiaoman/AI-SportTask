package ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import data.remote.PlatformStorageProvider

/** 主题偏好持久化：设置页切换后重启 App 仍保持 */
object ThemePreference {
    private const val KEY = "app_dark_mode_enabled"

    fun isDark(): Boolean = runCatching {
        PlatformStorageProvider.get().load(KEY) == "true"
    }.getOrDefault(false)

    fun setDark(enabled: Boolean) {
        runCatching { PlatformStorageProvider.get().save(KEY, enabled.toString()) }
    }
}

/**
 * 设计系统统一入口。页面通过 AppTheme.colors / .dimens / .shapes / .typography 取 token。
 */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val dimens: AppDimens
        get() = AppDimens

    val shapes: AppShapes
        get() = AppShapes

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography
}

@Composable
fun SportTaskTheme(
    darkTheme: Boolean = ThemePreference.isDark(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides palette) {
        MaterialTheme(
            colors = palette.toMaterialColors(),
            typography = AppTypography,
            shapes = AppMaterialShapes,
            content = content
        )
    }
}
