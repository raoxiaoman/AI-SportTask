package ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全站唯一的尺寸定义处。
 *
 * 所有 padding / Spacer / spacedBy 只准取 4pt 栅格：s1=4、s2=8、s3=12、s4=16、
 * s6=24、s8=32、s10=40、s12=48、s16=64。
 * 其余为具名语义尺寸（圆环直径、图表高度、按钮高度等），页面不得直接写 .dp。
 */
object AppDimens {
    val zero: Dp = 0.dp
    val divider: Dp = 1.dp
    val hairline: Dp = 2.dp

    val s1: Dp = 4.dp
    val s2: Dp = 8.dp
    val s3: Dp = 12.dp
    val s4: Dp = 16.dp
    val s6: Dp = 24.dp
    val s8: Dp = 32.dp
    val s10: Dp = 40.dp
    val s12: Dp = 48.dp
    val s16: Dp = 64.dp

    val screenPadding: Dp = s4
    val cardPadding: Dp = s4
    val sectionGap: Dp = s6

    val iconSmall: Dp = 18.dp
    val icon: Dp = 24.dp
    val iconLarge: Dp = 48.dp
    val iconHero: Dp = 64.dp
    val touchTarget: Dp = 48.dp
    val iconBadge: Dp = 28.dp
    val playButton: Dp = 52.dp
    val weekChipHeight: Dp = 28.dp

    val buttonHeight: Dp = 52.dp
    /** 弹窗内按钮高度（比页面主按钮略矮，避免弹窗过高） */
    val dialogButtonHeight: Dp = 40.dp
    /** 弹窗最大宽度，避免大屏上正文行过长 */
    val dialogMaxWidth: Dp = 360.dp
    /** 弹窗多行文本输入区高度（导出/导入 JSON） */
    val dialogTextAreaHeight: Dp = 180.dp
    /** 页面表单多行文本输入区高度（步骤说明） */
    val formTextAreaHeight: Dp = 120.dp
    val navHeight: Dp = 64.dp
    val logoSize: Dp = 80.dp
    val actionStripHeight: Dp = 72.dp
    val progressRing: Dp = 200.dp
    val progressRingSmall: Dp = 150.dp
    val chartHeight: Dp = 120.dp
    val dialogWidth: Dp = 280.dp
    /** 动作示意图边长 */
    val diagramSize: Dp = 224.dp

    val cardRadius: Dp = 16.dp
    val cardRadiusLarge: Dp = 18.dp
    val buttonRadius: Dp = 12.dp
    val chipRadius: Dp = 8.dp
    val dialogRadius: Dp = 20.dp
    val barTopRadius: Dp = 4.dp

    val cardElevation: Dp = 2.dp
    val headerElevation: Dp = 4.dp
}
