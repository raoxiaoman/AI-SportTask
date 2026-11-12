package ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 全站唯一字体与字阶定义处。
 *
 * 页面只准使用 MaterialTheme.typography.* 或 AppTypeScale.*，
 * 禁止再写 fontSize = 16.sp 这类硬编码字号，也不要单独指定 fontFamily。
 * 字阶共 5 级：30 / 20 / 15 / 13 / 11，辅以 24 / 18 / 16 三个中间档。
 */
val AppFontFamily: FontFamily = FontFamily.Default

/**
 * 字重 token。设计系统只用 3 档：常规 400 / 中等 500 / 强调 700。
 * 推荐优先通过 MaterialTheme.typography.* 继承字重；确需覆写时用这里的常量。
 */
object AppTypeWeight {
    val regular = FontWeight.Normal
    val medium = FontWeight.Medium
    val emphasis = FontWeight.Bold
}

object AppTypeScale {
    val display: TextUnit = 30.sp
    val headline: TextUnit = 24.sp
    val title: TextUnit = 20.sp
    val subtitle: TextUnit = 18.sp
    val body: TextUnit = 15.sp
    val label: TextUnit = 13.sp
    val caption: TextUnit = 11.sp
    val countdown: TextUnit = 56.sp
    val heroIcon: TextUnit = 64.sp
    val achievementIcon: TextUnit = 36.sp
}

private fun textStyle(size: TextUnit, weight: FontWeight, line: TextUnit): TextStyle = TextStyle(
    fontFamily = AppFontFamily,
    fontWeight = weight,
    fontSize = size,
    lineHeight = line
)

val AppTypography = Typography(
    h1 = textStyle(AppTypeScale.display, FontWeight.Medium, 38.sp),
    h2 = textStyle(AppTypeScale.headline, FontWeight.Medium, 32.sp),
    h3 = textStyle(AppTypeScale.title, FontWeight.Medium, 28.sp),
    h4 = textStyle(AppTypeScale.subtitle, FontWeight.Medium, 26.sp),
    h5 = textStyle(16.sp, FontWeight.Medium, 24.sp),
    h6 = textStyle(AppTypeScale.body, FontWeight.Medium, 22.sp),
    subtitle1 = textStyle(AppTypeScale.body, FontWeight.Medium, 22.sp),
    subtitle2 = textStyle(AppTypeScale.label, FontWeight.Medium, 18.sp),
    body1 = textStyle(AppTypeScale.body, FontWeight.Normal, 23.sp),
    body2 = textStyle(AppTypeScale.label, FontWeight.Normal, 19.sp),
    button = textStyle(AppTypeScale.body, FontWeight.Medium, 20.sp),
    caption = textStyle(AppTypeScale.caption, FontWeight.Normal, 16.sp),
    overline = textStyle(AppTypeScale.caption, FontWeight.Medium, 16.sp)
)
