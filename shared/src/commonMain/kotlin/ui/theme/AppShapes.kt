package ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes

/**
 * 全站唯一的圆角定义处。大卡 18、卡片 16、按钮 12、胶囊 50%、标签 8、弹窗 20。
 */
object AppShapes {
    val card = RoundedCornerShape(AppDimens.cardRadius)
    val cardLarge = RoundedCornerShape(AppDimens.cardRadiusLarge)
    val button = RoundedCornerShape(AppDimens.buttonRadius)
    val pill = RoundedCornerShape(percent = 50)
    val chip = RoundedCornerShape(AppDimens.chipRadius)
    val dialog = RoundedCornerShape(AppDimens.dialogRadius)
    val sheet = RoundedCornerShape(
        topStart = AppDimens.cardRadius,
        topEnd = AppDimens.cardRadius
    )
    val barTop = RoundedCornerShape(
        topStart = AppDimens.barTopRadius,
        topEnd = AppDimens.barTopRadius
    )
}

val AppMaterialShapes = Shapes(
    small = AppShapes.chip,
    medium = AppShapes.button,
    large = AppShapes.card
)
