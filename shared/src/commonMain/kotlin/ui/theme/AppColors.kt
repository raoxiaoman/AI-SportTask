package ui.theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 语义色板 —— 全站唯一的颜色定义处。
 *
 * 页面只准通过 [AppTheme.colors] 取色，禁止再出现 Color(0xFF…) / Color.Gray 之类的硬编码。
 * 分区约定：
 *  - brand / brandStrong / brandDeep：主操作、进度、选中、今日任务主卡
 *  - success / warning / danger：语义色，各自带 Soft 背景版本
 *  - background / surface / surfaceAlt / outline：背景层、内容层、分隔线
 *  - ink / inkSub / inkMuted：主文字、副文字、提示文字
 *
 * 对比度（浅色主题，白/浅底）：ink 15.8:1、inkSub 6.1:1，均满足 WCAG AA 4.5:1。
 * brandStrong 上的白字约 6.1:1；brandDeep 上的白字约 9.8:1。
 * 因此承载白色正文的按钮/主卡一律用 brandStrong / brandDeep，不用较亮的 brand。
 */
data class AppColors(
    val brand: Color,
    val brandStrong: Color,
    val brandDeep: Color,
    val brandSoft: Color,
    val onBrand: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val outline: Color,
    val ink: Color,
    val inkSub: Color,
    val inkMuted: Color,
    val isLight: Boolean
)

val LightAppColors = AppColors(
    brand = Color(0xFF4F6FE8),
    brandStrong = Color(0xFF3B57CC),
    brandDeep = Color(0xFF2B3F86),
    brandSoft = Color(0xFFEEF2FF),
    onBrand = Color(0xFFFFFFFF),
    success = Color(0xFF1D9E75),
    successSoft = Color(0xFFE1F5EE),
    warning = Color(0xFFBA7517),
    warningSoft = Color(0xFFFAEEDA),
    danger = Color(0xFFC43C3C),
    dangerSoft = Color(0xFFFCEBEB),
    background = Color(0xFFF5F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEEF2F7),
    outline = Color(0xFFE6EBF3),
    ink = Color(0xFF16203A),
    inkSub = Color(0xFF5B667A),
    inkMuted = Color(0xFF8A94A6),
    isLight = true
)

val DarkAppColors = AppColors(
    brand = Color(0xFF85B7EB),
    brandStrong = Color(0xFF6C8CFF),
    brandDeep = Color(0xFF2B3F86),
    brandSoft = Color(0xFF1B2A4A),
    onBrand = Color(0xFFFFFFFF),
    success = Color(0xFF5DCAA5),
    successSoft = Color(0xFF123A30),
    warning = Color(0xFFEF9F27),
    warningSoft = Color(0xFF3A2A10),
    danger = Color(0xFFF09595),
    dangerSoft = Color(0xFF3A1C1C),
    background = Color(0xFF0F1520),
    surface = Color(0xFF182130),
    surfaceAlt = Color(0xFF222D3E),
    outline = Color(0xFF2C384B),
    ink = Color(0xFFE8EDF5),
    inkSub = Color(0xFFA6B2C4),
    inkMuted = Color(0xFF77859A),
    isLight = false
)

/** 把语义色板映射到 Material 2 色槽，让既有 MaterialTheme.colors.* 调用自动跟随主题 */
fun AppColors.toMaterialColors(): Colors = if (isLight) {
    lightColors(
        primary = brandStrong,
        primaryVariant = brandDeep,
        secondary = brand,
        secondaryVariant = brandDeep,
        background = background,
        surface = surface,
        error = danger,
        onPrimary = onBrand,
        onSecondary = onBrand,
        onBackground = ink,
        onSurface = ink,
        onError = onBrand
    )
} else {
    // 深色主题下 primary 为亮蓝，按钮文字改用深墨以达到 AA 对比度；
    // onBrand 恒为白色，供深蓝主卡与主卡内 CTA 使用。
    darkColors(
        primary = brandStrong,
        primaryVariant = brandDeep,
        secondary = brand,
        background = background,
        surface = surface,
        error = danger,
        onPrimary = Color(0xFF0D1526),
        onSecondary = Color(0xFF0D1526),
        onBackground = ink,
        onSurface = ink,
        onError = Color(0xFF0D1526)
    )
}

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
